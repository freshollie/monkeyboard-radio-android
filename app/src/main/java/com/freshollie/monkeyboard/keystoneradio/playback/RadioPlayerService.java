package com.freshollie.monkeyboard.keystoneradio.playback;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import com.freshollie.monkeyboard.R;
import com.freshollie.monkeyboard.keystoneradio.radio.ListenerManager;
import com.freshollie.monkeyboard.keystoneradio.radio.RadioDevice;
import com.freshollie.monkeyboard.keystoneradio.radio.RadioStation;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Freshollie on 14/01/2017.
 *
 * Radio Player Service manages the control of the radio.
 *
 */
public class RadioPlayerService extends Service implements AudioManager.OnAudioFocusChangeListener {
    private static String TAG = RadioPlayerService.class.getSimpleName();
    static int ATTACH_TIMEOUT = 10000; // Radio will stop trying to connect after 10 seconds

    // Actions
    public final static String ACTION_NEXT =
            "com.freshollie.monkeyboarddab.playback.radioplayerservice.action.NEXT";
    public final static String ACTION_PREVIOUS =
            "com.freshollie.monkeyboarddab.playback.radioplayerservice.action.PREVIOUS";
    public final static String ACTION_STOP =
            "com.freshollie.monkeyboarddab.playback.radioplayerservice.action.STOP";
    public final static String ACTION_PLAY =
            "com.freshollie.monkeyboarddab.playback.radioplayerservice.action.PLAY";
    public final static String ACTION_PAUSE =
            "com.freshollie.monkeyboarddab.playback.radioplayerservice.action.PAUSE";

    private IBinder binder = new RadioPlayerBinder();

    private RadioPlayerNotification playerNotification;

    private RadioDevice radio;
    private RadioStation[] radioStations = new RadioStation[0];


    private AudioManager audioManager;
    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder playbackStateBuilder;

    private Runnable queuedAction;


    private boolean controllerInput = false;

    private int volume = 13;
    private int duckVolume = 3;
    private boolean muted = false;

    enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }

    private AudioFocus audioFocusState = AudioFocus.NoFocusNoDuck;

    private int currentChannelIndex = -1;

    public interface PlayerCallback {
        void onNoStoredStations();
        void onAttachTimeout();

        void onSearchStart();
        void onSearchProgressUpdate(int numChannels, int progress);
        void onSearchComplete(int numChannels);

        void onStationListCopyStart();
        void onStationListCopyProgressUpdate(int progress, int max);
        void onStationListCopyComplete();
        void onDismissed();
    }

    HashSet<PlayerCallback> playerCallbacks = new HashSet<>();

    public class RadioPlayerBinder extends Binder {
        public RadioPlayerService getService() {
            // Return this instance of RadioPlayerService so clients can call public methods
            return RadioPlayerService.this;
        }
    }

    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            if (s.equals(getString(R.string.DUCK_VOLUME_KEY))) {
                duckVolume =
                        sharedPreferences.getInt(
                                getString(R.string.DUCK_VOLUME_KEY),
                                3
                        );
                Log.v(TAG, "Duck volume set to " + String.valueOf(duckVolume));
            } else if (s.equals(getString(R.string.HEADUNIT_MAIN_INPUT_KEY))){
                controllerInput =
                        sharedPreferences.getBoolean(
                                getString(R.string.HEADUNIT_MAIN_INPUT_KEY),
                                false
                        );
                Log.v(TAG, "Headunit input set to: " + String.valueOf(controllerInput));
            }
        }
    };

    private RadioDevice.CopyProgramsListener copyProgramsListener =
            new RadioDevice.CopyProgramsListener() {
        @Override
        public void onProgressUpdate(int progress, int max) {
            Log.v(TAG, String.format("Collected %s/%s", progress, max));
            onStationListCopyProgressUpdate(progress, max);
        }

        @Override
        public void onComplete(RadioStation[] stationList) {
            Log.v(TAG, "Collected all stations");
            ArrayList<RadioStation> newStationList = new ArrayList<>();


            for (RadioStation radioStation: stationList) {
                if (radioStation.getGenreId() != -1) {
                    newStationList.add(radioStation);
                }
            }

            setStationList(
                    newStationList.toArray(
                            new RadioStation[newStationList.size()]
                    )
            );

            if (radioStations.length > 0) {
                onStationListCopyComplete();
            } else {
                onNoStoredStations();
            }
        }
    };

    private RadioDevice.DABSearchListener dabSearchListener = new RadioDevice.DABSearchListener() {
        @Override
        public void onProgressUpdate(int numPrograms, int progress) {
            onChannelSearchProgressUpdate(numPrograms, progress);
        }

        @Override
        public void onComplete(int numPrograms) {
            onChannelSearchComplete(numPrograms);
        }

        @Override
        public void onStarted() {
            onChannelSearchStart();
        }
    };

    private ListenerManager.ConnectionStateChangeListener connectionStateListener =
            new ListenerManager.ConnectionStateChangeListener() {
        @Override
        public void onStart() {
            onConnectedSequence();
        }

        @Override
        public void onStop() {
            if (connectThread.isAlive()) {
                connectThread.interrupt();
            } else {
                if (hasFocus()) {
                    handlePauseRequest();
                }
            }
        }
    };

    private Thread connectThread = new Thread();
    private Runnable connectRunnable =
            new Runnable() {
                @Override
                public void run() {
                    Log.v(TAG, "Starting a wait for attachment thread");
                    long startTime = SystemClock.currentThreadTimeMillis();

                    while (!radio.isAttached()
                            && (SystemClock.currentThreadTimeMillis() - startTime) < ATTACH_TIMEOUT) {
                        if (Thread.currentThread().isInterrupted()) {
                            return;
                        }
                    }

                    if (radio.isAttached()) {
                        new Handler(getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Log.v(TAG, "Monkeyboard connected");
                                radio.connect();
                            }
                        });
                    } else {
                        new Handler(getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                onAttachTimeout();
                            }
                        });
                    }
                }
            };

    public void loadPreferences() {
        Set<String> stationsJsonList =
                sharedPreferences.getStringSet(
                        getString(R.string.STATION_LIST_KEY),
                        null
                );

        if (stationsJsonList != null) {
            radioStations = new RadioStation[stationsJsonList.size()];
            try {
                int i = 0;
                for (String stationJsonString: stationsJsonList) {
                    JSONObject stationJson = new JSONObject(stationJsonString);

                    radioStations[i] =
                            new RadioStation(
                                    stationJson.getString("name"),
                                    stationJson.getInt("channelNumber"),
                                    stationJson.getInt("genreId"),
                                    stationJson.getString("ensemble")
                            );
                    i++;
                }

                Arrays.sort(radioStations, new Comparator<RadioStation>() {
                    @Override
                    public int compare(RadioStation radioStation, RadioStation t1) {
                        return Integer.compare(radioStation.getChannelId(), t1.getChannelId());
                    }
                });

            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "Could not load station list");
            }
        } else {
            radioStations = new RadioStation[0];
        }

        controllerInput =
                sharedPreferences.getBoolean(
                        getString(R.string.HEADUNIT_MAIN_INPUT_KEY),
                        false
                );
        currentChannelIndex = sharedPreferences.getInt(getString(R.string.CURRENT_CHANNEL_KEY), 0);
        volume = sharedPreferences.getInt(getString(R.string.VOLUME_KEY), 13);
        duckVolume = sharedPreferences.getInt(getString(R.string.DUCK_VOLUME_KEY), 3);
    }

    public void saveStationList() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Set<String> stringSet = new HashSet<>();
        for (RadioStation station: radioStations) {
            stringSet.add(station.toJsonString());
        }

        editor.putStringSet(getString(R.string.STATION_LIST_KEY), stringSet);

        editor.apply();
    }

    public void setStationList(RadioStation[] stationList) {
        radioStations = stationList;
        saveStationList();
    }

    public void saveCurrentChannel() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(getString(R.string.CURRENT_CHANNEL_KEY), currentChannelIndex);
        editor.apply();
    }

    public void saveVolume() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(getString(R.string.VOLUME_KEY), volume);
        editor.apply();
    }

    @Override
    public void onCreate(){
        sharedPreferences = getSharedPreferences(getString(R.string.SHARED_PREFERENCES_KEY), Context.MODE_PRIVATE);
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mediaSession = new MediaSessionCompat(this, "RadioPlayerService");
        mediaSession.setCallback(new MediaSessionCallback());
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setActive(true);

        playbackStateBuilder = new PlaybackStateCompat.Builder();

        radio = new RadioDevice(getApplicationContext());

        radio.getListenerManager().registerDataListener(dataListener);
        radio.getListenerManager().registerConnectionStateChangedListener(connectionStateListener);

        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
        updateMetadata(new RadioStation("", -1, -1, ""));

        playerNotification = new RadioPlayerNotification(this);

        loadPreferences();
        openConnection();
    }

    public void startStationListCopyTask() {
        radio.copyStationList(copyProgramsListener);
        setCurrentChannelIndex(0);
        onStationListCopyStart();
    }

    public void onConnectedSequence() {
        // If the our internal database is dramatically different to that on the board, we will try
        // and sync our copies
        if (getRadioStations().length < 1
                || Math.abs(getRadioStations().length - radio.getTotalPrograms()) > 8) {
            if (radio.getTotalPrograms() > 0) {
                startStationListCopyTask();
            } else {
                Log.v(TAG, "No stations stored, need to perform channel search");
                onNoStoredStations();
            }

        } else {
            radio.setStereoMode(1);
            if (queuedAction != null) {
                queuedAction.run();
                queuedAction = null;
            }
        }
        if (playerNotification != null) {
            playerNotification.update();
        }
    }

    public void openConnection() {
        Log.v(TAG, "Starting device connection");
        if (playerNotification != null) {
            playerNotification.update();
        }

        if (!connectThread.isAlive()) {
            connectThread = new Thread(connectRunnable);
            connectThread.start();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Got start intent");
        if (intent != null && intent.getAction() != null) {
            Log.v(TAG, "Received intent:" + intent.getAction());

            switch (intent.getAction()) {
                case ACTION_STOP:
                    Log.v(TAG, "Received stop intent");
                    onDismissed();
                    closeConnection();
                    break;
                case ACTION_NEXT:
                    handleNextChannelRequest();
                    break;
                case ACTION_PAUSE:
                    handlePauseRequest();
                    break;
                case ACTION_PREVIOUS:
                    handlePreviousChannelRequest();
                    break;
                case ACTION_PLAY:
                    handlePlayRequest();
                    break;
                default:
                    MediaButtonReceiver.handleIntent(mediaSession, intent);
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent){
        return binder;
    }

    public RadioDevice getRadio() {
        return radio;
    }

    public MediaSessionCompat getMediaSession() {
        return mediaSession;
    }

    public MediaControllerCompat getMediaController() {
        return mediaSession.getController();
    }

    public int getPlaybackState() {
        return getMediaController().getPlaybackState().getState();
    }

    public MediaMetadataCompat getMetadata() {
        return getMediaController().getMetadata();
    }

    public int getPlayerVolume() {
        return volume;
    }

    public RadioStation getCurrentStation() {
        if (radioStations.length > 0) {
            return radioStations[getCurrentChannelIndex()];
        } else {
            return null;
        }
    }

    public RadioStation getStationFromId(int channelId) {
        for (RadioStation station: radioStations) {
            if (station.getChannelId() == channelId) {
                return station;
            }
        }
        return null;
    }

    public RadioStation getStationFromIndex(int stationIndex) {
        return radioStations[stationIndex];
    }

    public RadioStation[] getRadioStations() {
        return radioStations;
    }

    public int getCurrentChannelIndex() {
        return currentChannelIndex;
    }

    public void setCurrentChannelIndex(int channelIndex) {
        currentChannelIndex = channelIndex;
        saveCurrentChannel();
    }

    public boolean isMuted() {
        return muted;
    }

    public void handleAction(Runnable action) {
        if (radio.isConnected()) {
            action.run();
        } else {
            queuedAction = action;
            openConnection();
        }
    }

    public boolean setChannelAction(int channelIndex) {
        if (getRadioStations().length > 0) {
            Log.v(TAG, "Requesting board to play: " + getCurrentStation().getName());
            if (radio.play(getStationFromIndex(channelIndex).getChannelId())) {
                setCurrentChannelIndex(channelIndex);
                Log.v(TAG, "Approved, updating meta");
                updateMetadata(getCurrentStation());
                return true;
            }
        }
        return false;
    }

    public boolean handleSetChannel(final int channelIndex) {
        if (radio.isConnected()) {
            return setChannelAction(channelIndex);
        } else {
            queuedAction = new Runnable() {
                @Override
                public void run() {
                    setChannelAction(channelIndex);
                }
            };
            openConnection();
            return false;
        }
    }

    public void handlePlayRequest() {
        Log.v(TAG, "Handling a play request");
        if (currentChannelIndex == -1 || getRadioStations().length < 1) {
            Log.v(TAG, "No station to play, ignoring request");
        } else {
            handleAction(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (!hasFocus()) {
                                requestAudioFocus();
                            }

                            if (hasFocus()) {
                                if (handleSetChannel(currentChannelIndex)) {
                                    Log.v(TAG, "Updating playstate");
                                    handleUnmuteRequest();
                                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                                } else {
                                    Log.v(TAG, "Board denied play request");
                                }
                            }
                        }

                    }
            );
        }
    }

    public void handleSetVolumeRequest(final int newVolume) {
        volume = newVolume;
        saveVolume();
        handleAction(
                new Runnable() {
                    @Override
                    public void run() {
                        radio.setVolume(volume);
                    }
                }
        );
    }

    public void handleMuteRequest() {
        handleAction(
                new Runnable() {
                    @Override
                    public void run() {
                        muted = true;
                        radio.setVolume(0);
                    }
                }
        );
    }

    public void handleUnmuteRequest() {
        handleAction(
                new Runnable() {
                    @Override
                    public void run() {
                        muted = false;
                        radio.setVolume(volume);
                    }
                }
        );
    }

    public void handlePauseRequest() {
        muted = true;

        // Used to make sure that the volume is lowered, command
        // is executed until the volume is confirmed lowered
        while (!radio.setVolume(0) && radio.isConnected()) {}
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
    }


    public void handleNextChannelRequest() {
        handleAction(
                new Runnable() {
                    @Override
                    public void run() {
                        if (currentChannelIndex < getRadioStations().length - 1) {
                            handleSetChannel(currentChannelIndex + 1);
                        } else {
                            handleSetChannel(0);
                        }
                    }
                }
        );
    }

    public void handlePreviousChannelRequest() {
        handleAction(
                new Runnable() {
                    @Override
                    public void run() {
                        if (currentChannelIndex > 0) {
                            handleSetChannel(currentChannelIndex - 1);
                        } else {
                            handleSetChannel(getRadioStations().length - 1);
                        }
                    }
                }
        );
    }

    public void handleFocusDuck() {
        radio.setVolume(duckVolume);
    }

    public void handleFocusGain() {
        radio.setVolume(volume);
    }

    public void handleFocusLost() {
        handlePauseRequest();
        abandonAudioFocus();
    }

    public void startChannelSearchTask() {
        radio.startDABSearch(dabSearchListener);
    }

    public boolean hasFocus() {
        return audioFocusState == AudioFocus.Focused;
    }

    public boolean isPlaying() {
        return getPlaybackState() == PlaybackStateCompat.STATE_PLAYING;
    }

    public void updatePlaybackState(int state) {
        if (mediaSession != null) {
            mediaSession.setPlaybackState(
                    playbackStateBuilder
                        .setState(
                                state,
                                0,
                                1)
                        .build()
            );

            if (playerNotification != null) {
                playerNotification.update();
            }
        }
    }

    public void updateMetadata(RadioStation station) {
        if (mediaSession != null) {
            if (getMediaController().getMetadata() != null) {
                if (getMediaController()
                        .getMetadata()
                        .getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER)
                        == station.getChannelId()) {
                    return;
                }
            }

            mediaSession.setMetadata(
                    new MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                                    station.getName())
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                                    station.getEnsemble())
                            .putString(MediaMetadataCompat.METADATA_KEY_GENRE,
                                    RadioDevice.StringValues.getGenreFromId(
                                            station.getGenreId()
                                    )
                            )
                            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER,
                                    station.getChannelId())
                            .build()
            );

            if (playerNotification != null) {
                playerNotification.update();
            }
        }
    }

    private void requestAudioFocus() {
        Log.v(TAG, "Requesting Audio Focus");
        int result = audioManager.requestAudioFocus(this,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.v(TAG, "Request granted");
            audioFocusState = AudioFocus.Focused;
        } else {
            Log.v(TAG, "Could not gain full focus");
            audioFocusState = AudioFocus.NoFocusCanDuck;
        }
    }

    public void abandonAudioFocus() {
        Log.v(TAG, "Abandoning audio focus");
        if (hasFocus()) {
            audioManager.abandonAudioFocus(this);
        }
        audioFocusState = AudioFocus.NoFocusNoDuck;
    }

    @Override
    public void onAudioFocusChange(int focus){
        Log.v(TAG, "Audio focus changed");
        switch(focus) {
            case AudioManager.AUDIOFOCUS_LOSS:
                // Another app has gained focus;
                handleFocusLost();
                break;
            case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
                handlePauseRequest();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (radio.isAttached()) {
                    handleFocusDuck();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (radio.isAttached()) {
                    handleMuteRequest();
                }
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                handleFocusGain();
                break;
        }
    }

    public void closeConnection () {
        if (connectThread.isAlive()) {
            connectThread.interrupt();
        }
        abandonAudioFocus();

        if (radio.isConnected()) {
            handlePauseRequest();
        }

        radio.disconnect();
        playerNotification.cancel();
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "Stopping service");
        connectThread.interrupt();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        mediaSession.setCallback(null);
        mediaSession.release();

        radio.getListenerManager().unregisterDataListener(dataListener);
        radio.getListenerManager().unregisterConnectionStateChangedListener(connectionStateListener);

        if (radio.isConnected()) {
            radio.disconnect();
        }
        playerNotification.cancel();
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            handlePlayRequest();
        }

        @Override
        public void onPause() {
            handlePauseRequest();
        }

        @Override
        public void onStop() { onDismissed();
        }

        @Override
        public void onSkipToNext() {
            handleNextChannelRequest();
        }

        @Override
        public void onSkipToPrevious() {
            handlePreviousChannelRequest();
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
            Log.v(TAG, "Got media button intent");
            KeyEvent event = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

            if (event.getAction() == KeyEvent.ACTION_UP) {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        if (controllerInput) {
                            handlePlayRequest();
                        } else {
                            if (isPlaying()) {
                                handlePauseRequest();
                            } else {
                                handlePlayRequest();
                            }
                        }
                        return true;

                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        handlePlayRequest();
                        return true;

                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        handleNextChannelRequest();
                        return true;

                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        handlePreviousChannelRequest();
                        return true;

                    case KeyEvent.KEYCODE_MEDIA_STOP:
                        handlePauseRequest();
                        return true;

                    default:
                        return false;
                }
            } else {
                return false;
            }
        }
    }

    private ListenerManager.DataListener dataListener = new ListenerManager.DataListener() {
        @Override
        public void onProgramTextChanged(String programText) {

        }

        @Override
        public void onPlayStatusChanged(int playStatus) {

        }

        @Override
        public void onSignalQualityChanged(int signalStrength) {

        }

        @Override
        public void onProgramDataRateChanged(int dataRate) {

        }

        @Override
        public void onVolumeChanged(int volume) {

        }

        @Override
        public void onStereoStateChanged(int stereoState) {

        }
    };

    public void registerCallback(PlayerCallback callback) {
        playerCallbacks.add(callback);
    }

    public void unregisterCallback(PlayerCallback callback) {
        playerCallbacks.remove(callback);
    }

    public void onAttachTimeout() {
        Log.v(TAG, "Timed out waiting for device to attach");
        for (PlayerCallback playerCallback: playerCallbacks) {
            playerCallback.onAttachTimeout();
        }
    }

    public void onStationListCopyStart() {
        for (PlayerCallback callback: playerCallbacks) {
            callback.onStationListCopyStart();
        }
    }

    public void onChannelSearchStart() {
        for (PlayerCallback callback: playerCallbacks) {
            callback.onSearchStart();
        }
    }

    public void onNoStoredStations() {
        for (PlayerCallback callback: playerCallbacks) {
            callback.onNoStoredStations();
        }
    }

    public void onChannelSearchProgressUpdate(int channelsFound, int progress) {
        for (PlayerCallback callback: playerCallbacks) {
            callback.onSearchProgressUpdate(channelsFound, progress);
        }
    }

    public void onChannelSearchComplete(int numChannels) {
        for (PlayerCallback callback: playerCallbacks) {
            callback.onSearchComplete(numChannels);
        }
    }

    public void onStationListCopyProgressUpdate(int progress, int max) {
        for (PlayerCallback callback: playerCallbacks) {
            callback.onStationListCopyProgressUpdate(progress, max);
        }
    }

    public void onStationListCopyComplete() {
        for (PlayerCallback callback: playerCallbacks) {
            callback.onStationListCopyComplete();
        }
    }

    public void onDismissed() {
        for (PlayerCallback playerCallback: playerCallbacks) {
            playerCallback.onDismissed();
        }
    }
}
