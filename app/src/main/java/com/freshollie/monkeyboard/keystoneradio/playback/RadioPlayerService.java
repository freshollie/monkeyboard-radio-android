package com.freshollie.monkeyboard.keystoneradio.playback;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.VolumeProviderCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import com.freshollie.monkeyboard.keystoneradio.R;
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
    private VolumeProviderCompat volumeProvider;
    private PlaybackStateCompat.Builder playbackStateBuilder;

    private Runnable queuedAction;

    private SettingsVolumeObserver settingsVolumeObserver;

    private boolean controllerInput = false;

    private int volume = 13;
    private int duckVolume = 3;
    private boolean muted = false;
    private boolean ducked = false;

    public static int MAX_PLAYER_VOLUME = 15;


    private enum RadioMode {
        FM,
        DAB
    }

    private RadioMode mode;

    private enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }

    private AudioFocus audioFocusState = AudioFocus.NoFocusNoDuck;

    private int currentChannelIndex = -1;

    public interface PlayerCallback {
        void onPlayerVolumeChanged(int volume);

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
            notifyStationListCopyProgressUpdate(progress, max);
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
                notifyStationListCopyComplete();
            } else {
                notifyNoStoredStations();
            }
        }
    };

    private RadioDevice.DABSearchListener dabSearchListener = new RadioDevice.DABSearchListener() {
        @Override
        public void onProgressUpdate(int numPrograms, int progress) {
            notifyChannelSearchProgressUpdate(numPrograms, progress);
        }

        @Override
        public void onComplete(int numPrograms) {
            notifyChannelSearchComplete(numPrograms);
        }

        @Override
        public void onStarted() {
            notifyChannelSearchStart();
        }
    };

    private ListenerManager.ConnectionStateChangeListener connectionStateListener =
            new ListenerManager.ConnectionStateChangeListener() {
        @Override
        public void onStart() {
            openingConnection = false;
            onConnectedSequence();
        }

        @Override
        public void onFail() {
            openingConnection = false;
        }

        @Override
        public void onStop() {
            openingConnection = false;
            if (waitForAttachThread.isAlive()) {
                waitForAttachThread.interrupt();
            } else {
                if (hasFocus()) {
                    handlePauseRequest();
                }
            }
        }
    };

    private boolean openingConnection = false;
    private Thread waitForAttachThread = new Thread();
    private Runnable waitForAttachRunnable =
            new Runnable() {
                @Override
                public void run() {
                    Log.v(TAG, "Starting a wait for attachment thread");
                    long startTime = System.currentTimeMillis();

                    while (!radio.isAttached()
                            && (System.currentTimeMillis() - startTime) < ATTACH_TIMEOUT) {
                        if (Thread.interrupted()) {
                            return;
                        }
                    }

                    if (radio.isAttached()) {
                        Log.v(TAG, "Keystone radio connected");
                        radio.connect();
                    } else {
                        openingConnection = false;
                        new Handler(getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                notifyAttachTimeout();
                            }
                        });
                    }
                }
            };


    @Override
    public void onCreate() {
        // Get preferences storage;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        // Get audio manager
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if (!sharedPreferences.getBoolean(getString(R.string.SYNC_VOLUME_KEY), true)) {
            volume = sharedPreferences.getInt(getString(R.string.VOLUME_KEY), 13);
        } else {
            // Set the player volume to the system volume
            volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }

        Log.d(TAG, "Start volume: " + volume);

        // Build a media session for the radioplayer
        mediaSession = new MediaSessionCompat(this, this.getClass().getSimpleName());
        mediaSession.setCallback(new MediaSessionCallback());
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Listen for settings changes and see if volume changes
        // Only required before android 5.0

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // Make an observer of the settings, which notifies us if the volume changes
            settingsVolumeObserver =
                    new SettingsVolumeObserver(
                            this,
                            new Handler(getMainLooper()),
                            new SettingsVolumeObserver.SettingsVolumeChangeListener() {
                                @Override
                                public void onChange(int newVolume) {
                                    setPlayerVolume(newVolume);
                                    Log.v(TAG, "Volume set: " + getPlayerVolume());
                                    notifyPlayerVolumeChanged(getPlayerVolume());
                                }
                            }
                    );

            // Register the observer
            getApplicationContext().getContentResolver()
                    .registerContentObserver(
                            android.provider.Settings.System.CONTENT_URI,
                            true,
                            settingsVolumeObserver
                    );

            mediaSession.setPlaybackToLocal(AudioManager.STREAM_MUSIC);
            Log.v(TAG, "Registering settings content observer");
        } else {
            volumeProvider = new VolumeProviderCompat(
                    VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE,
                    MAX_PLAYER_VOLUME,
                    volume
            ) {
                @Override
                public void onSetVolumeTo(int volume) {
                    super.onSetVolumeTo(volume);
                    setPlayerVolume(volume);
                    Log.v(TAG, "Volume set: " + getPlayerVolume());
                    notifyPlayerVolumeChanged(getPlayerVolume());
                }

                @Override
                public void onAdjustVolume(int direction) {
                    super.onAdjustVolume(direction);
                    if (direction != 0) {
                        setPlayerVolume(getPlayerVolume() + direction);
                        Log.v(TAG, "Volume adjusted: " + getPlayerVolume());
                        notifyPlayerVolumeChanged(getPlayerVolume());
                    }
                }

            };

            mediaSession.setPlaybackToRemote(volumeProvider);
        }

        // Build a playback state for the player
        playbackStateBuilder = new PlaybackStateCompat.Builder();

        // Update info of the current session
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
        updateMetadata(new RadioStation("", -1, -1, ""));

        // Connect to the radio API
        radio = new RadioDevice(getApplicationContext());

        // Register listeners of the radio
        radio.getListenerManager().registerDataListener(dataListener);
        radio.getListenerManager().registerConnectionStateChangedListener(connectionStateListener);

        // Make the notification
        playerNotification = new RadioPlayerNotification(this);

        saveVolume(volume);

        loadPreferences();

        // Start the process of connecting to the radio device
        openConnection();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Got start intent");
        if (intent != null && intent.getAction() != null) {
            Log.v(TAG, "Received intent:" + intent.getAction());

            switch (intent.getAction()) {
                case ACTION_STOP:
                    Log.v(TAG, "Received stop intent");
                    notifyDismissed();
                    closeConnection();
                    stopSelf();
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

    private void loadPreferences() {
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
                        return Integer.valueOf(radioStation.getChannelId()).compareTo(t1.getChannelId());
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
        duckVolume = sharedPreferences.getInt(getString(R.string.DUCK_VOLUME_KEY), 3);

        if (!sharedPreferences.getBoolean(getString(R.string.SYNC_VOLUME_KEY), true)) {
            volume = sharedPreferences.getInt(getString(R.string.VOLUME_KEY), 13);
        }
    }

    private void saveStationList() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Set<String> stringSet = new HashSet<>();
        for (RadioStation station: radioStations) {
            stringSet.add(station.toJsonString());
        }

        editor.putStringSet(getString(R.string.STATION_LIST_KEY), stringSet);

        editor.apply();
    }

    private void setStationList(RadioStation[] stationList) {
        radioStations = stationList;
        saveStationList();
    }

    private void saveCurrentChannel() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(getString(R.string.CURRENT_CHANNEL_KEY), currentChannelIndex);
        editor.apply();
    }

    public void startStationListCopyTask() {
        radio.copyStationList(copyProgramsListener);
        setCurrentChannelIndex(0);
        notifyStationListCopyStart();
    }

    private void onConnectedSequence() {

        // If the our internal database is dramatically different to that on the board, we will try
        // and sync our copies
        if (getRadioStations().length < 1
                || Math.abs(getRadioStations().length - radio.getTotalPrograms()) > 8) {
            if (radio.getTotalPrograms() > 0) {
                startStationListCopyTask();

            } else {
                Log.v(TAG, "No stations stored, need to perform channel search");
                notifyNoStoredStations();
            }

        } else {
            radio.setStereoMode(1);
            if (queuedAction != null) {
                queuedAction.run();
                queuedAction = null;
            }
            handleSetChannel(getCurrentChannelIndex());
        }
        if (playerNotification != null) {
            playerNotification.update();
        }
    }

    public void openConnection() {
        if (!waitForAttachThread.isAlive() && !openingConnection) {
            Log.v(TAG, "Starting device connection");

            if (playerNotification != null) {
                playerNotification.update();
            }

            openingConnection = true;
            waitForAttachThread = new Thread(waitForAttachRunnable);
            waitForAttachThread.start();
        }
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
        int volume = this.volume;

        if (sharedPreferences.getBoolean(getString(R.string.SYNC_VOLUME_KEY), true)) {
            volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

            if (volumeProvider != null) {
                if (volume != volumeProvider.getCurrentVolume()) {
                    volumeProvider.setCurrentVolume(volume);
                }
            }
        }

        return volume;

    }

    public void setPlayerVolume(int volume) {
        if (-1 < volume && volume < (MAX_PLAYER_VOLUME + 1) && volume != getPlayerVolume()) {
            if (radio.isConnected()) {
                handleSetVolumeRequest(volume);
            } else {
                this.volume = volume;
            }
        }

        saveVolume(volume);
    }

    public void saveVolume(int volume) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(getString(R.string.VOLUME_KEY), volume);
        editor.apply();

        if (sharedPreferences.getBoolean(getString(R.string.SYNC_VOLUME_KEY), true)) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        }

        if (volumeProvider != null) {
            volumeProvider.setCurrentVolume(volume);
        }
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

    private void setCurrentChannelIndex(int channelIndex) {
        currentChannelIndex = channelIndex;
        saveCurrentChannel();
    }

    public boolean isMuted() {
        return muted;
    }

    public boolean hasFocus() {
        return audioFocusState == AudioFocus.Focused;
    }

    public boolean isPlaying() {
        return getPlaybackState() == PlaybackStateCompat.STATE_PLAYING;
    }

    public boolean isDucked() {
        return ducked;
    }

    private void handleAction(Runnable action) {
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

    private void handleSetVolumeRequest(final int newVolume) {
        volume = newVolume;
        if (isPlaying()) {
            handleAction(
                    new Runnable() {
                        @Override
                        public void run() {
                            radio.setVolume(volume);
                        }
                    }
            );
        }
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
                        radio.setVolume(getPlayerVolume());
                    }
                }
        );
    }

    public void handlePauseRequest() {
        muted = true;

        // Used to make sure that the volume is lowered, command
        // is executed until the volume is confirmed lowered

        //noinspection StatementWithEmptyBody
        while (!radio.setVolume(0) && radio.isConnected());
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

    private void handleFocusDuck() {
        ducked = true;
        radio.setVolume(duckVolume);
    }

    private void handleFocusGain() {
        ducked = false;
        radio.setVolume(getPlayerVolume());
    }

    private void handleFocusLost() {
        handlePauseRequest();
        abandonAudioFocus();
    }

    public void startChannelSearchTask() {
        radio.startDABSearch(dabSearchListener);
    }

    private void updatePlaybackState(int state) {
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

    private void updateMetadata(RadioStation station) {
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

    private void abandonAudioFocus() {
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

    public void closeConnection() {
        if (waitForAttachThread.isAlive()) {
            waitForAttachThread.interrupt();
        }
        abandonAudioFocus();

        if (radio.isConnected()) {
            handlePauseRequest();
            radio.disconnect();
        }

        playerNotification.cancel();
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "Stopping service");
        closeConnection();
        if (waitForAttachThread.isAlive()) {
            waitForAttachThread.interrupt();
        }

        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        if (settingsVolumeObserver != null) {
            getApplicationContext().getContentResolver()
                    .unregisterContentObserver(settingsVolumeObserver);
        } else if (volumeProvider != null) {
            mediaSession.setPlaybackToLocal(AudioManager.STREAM_MUSIC);
        }

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
        public void onStop() { notifyDismissed();
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
        public void onRadioVolumeChanged(int volume) {

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

    private void notifyPlayerVolumeChanged(int newVolume) {
        for (PlayerCallback playerCallback: playerCallbacks) {
            playerCallback.onPlayerVolumeChanged(newVolume);
        }
    }

    private void notifyAttachTimeout() {
        Log.v(TAG, "Timed out waiting for device to attach");
        for (PlayerCallback playerCallback: playerCallbacks) {
            playerCallback.onAttachTimeout();
        }
    }

    private void notifyStationListCopyStart() {
        for (PlayerCallback callback: playerCallbacks) {
            callback.onStationListCopyStart();
        }
    }

    private void notifyChannelSearchStart() {
        for (PlayerCallback callback: playerCallbacks) {
            callback.onSearchStart();
        }
    }

    private void notifyNoStoredStations() {
        for (PlayerCallback callback: playerCallbacks) {
            callback.onNoStoredStations();
        }
    }

    private void notifyChannelSearchProgressUpdate(int channelsFound, int progress) {
        for (PlayerCallback callback: playerCallbacks) {
            callback.onSearchProgressUpdate(channelsFound, progress);
        }
    }

    private void notifyChannelSearchComplete(int numChannels) {
        for (PlayerCallback callback: playerCallbacks) {
            callback.onSearchComplete(numChannels);
        }
    }

    private void notifyStationListCopyProgressUpdate(int progress, int max) {
        for (PlayerCallback callback: playerCallbacks) {
            callback.onStationListCopyProgressUpdate(progress, max);
        }
    }

    public void notifyStationListCopyComplete() {
        for (PlayerCallback callback: playerCallbacks) {
            callback.onStationListCopyComplete();
        }
    }

    public void notifyDismissed() {
        for (PlayerCallback playerCallback: playerCallbacks) {
            playerCallback.onDismissed();
        }
    }
}
