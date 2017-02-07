package com.freshollie.monkeyboarddabradio.playback;

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

import com.freshollie.monkeyboarddabradio.R;
import com.freshollie.monkeyboarddabradio.radio.ListenerManager;
import com.freshollie.monkeyboarddabradio.radio.RadioDevice;
import com.freshollie.monkeyboarddabradio.radio.RadioStation;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
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
    static int ATTACH_TIMEOUT = 20000; // Radio will stop trying to connect after 20 seconds

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

    private SharedPreferences sharedPreferences;

    private IBinder binder = new RadioPlayerBinder();

    private RadioPlayerNotification playerNotification;
    private RadioDevice radio;

    private AudioManager audioManager;

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder playbackStateBuilder;

    private RadioStation[] radioStations = new RadioStation[0];

    private int volume = 13;
    private int duckVolume = 3;
    private boolean muted = false;

    enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }

    private AudioFocus audioFocusState = AudioFocus.NoFocusNoDuck;

    private int currentChannelIndex = 0;

    public interface PlayerCallback {
        void onNoSavedStations();
    }

    ArrayList<PlayerCallback> playerCallbacks = new ArrayList<>();

    public class RadioPlayerBinder extends Binder {
        public RadioPlayerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return RadioPlayerService.this;
        }
    }

    private RadioDevice.CopyProgramsListener copyProgramsListener =
            new RadioDevice.CopyProgramsListener() {
        @Override
        public void onProgressUpdate(int progress, int max) {
            Log.v(TAG, String.format("Collected %s/%s", progress, max));
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
                connectionStateListener.onStart();
            } else {
                onNoSavedStations();
            }
        }
    };

    private ListenerManager.ConnectionStateChangeListener connectionStateListener =
            new ListenerManager.ConnectionStateChangeListener() {
        @Override
        public void onStart() {
            startSequence();
        }

        @Override
        public void onStop() {
            handlePauseRequest();
        }
    };

    private Thread connectThread = new Thread();
    private Runnable connectRunnable =
            new Runnable() {
                @Override
                public void run() {
                    long startTime = SystemClock.currentThreadTimeMillis();

                    while (!radio.isAttached()
                            && (startTime - SystemClock.currentThreadTimeMillis()) < ATTACH_TIMEOUT) {

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
                for (String stationJsonString : stationsJsonList) {
                    JSONObject stationJson = new JSONObject(stationJsonString);

                    radioStations[stationJson.getInt("channelNumber")] =
                            new RadioStation(
                                    stationJson.getString("name"),
                                    stationJson.getInt("channelNumber"),
                                    stationJson.getInt("genreId"),
                                    stationJson.getString("ensemble")
                            );
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "Could not load station list");
            }
        } else {
            radioStations = new RadioStation[0];
        }

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

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mediaSession = new MediaSessionCompat(this, "RadioPlayerService");
        mediaSession.setCallback(new MediaSessionCallback());
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setActive(true);

        playbackStateBuilder = new PlaybackStateCompat.Builder();
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
        updateMetadata(new RadioStation("", 0, 0, ""));

        radio = new RadioDevice(getApplicationContext());

        radio.getListenerManager().registerDataListener(dataListener);
        radio.getListenerManager().registerConnectionStateChangedListener(connectionStateListener);
        radio.getListenerManager().registerChannelScanListener(channelScanListener);

        playerNotification = new RadioPlayerNotification(this);

        openConnection();
    }

    public void startSequence() {
        loadPreferences();

        if (getRadioStations().length < 1) {
            if (radio.getTotalPrograms() > 0) {
                radio.copyStationList(copyProgramsListener);
            } else {
                Log.v(TAG, "No stations stored, need to perform channel search");
                onNoSavedStations();
            }

        } else {
            radio.startPollLoop();
            radio.setStereoMode(1);
            handlePlayRequest();
        }
    }

    private void openConnection() {
        if (!connectThread.isAlive()) {
            connectThread = new Thread(connectRunnable);
            connectThread.start();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_STOP:
                    stopSelf();
                    break;
                case ACTION_NEXT:
                    handleNextChannelRequest();
                    break;
                case ACTION_PAUSE:
                    handleMuteRequest();
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
        return radioStations[getCurrentChannelIndex()];
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

    public void onAttachTimeout() {
        Log.v(TAG, "Timed out waiting for device to attach");
    }

    public void onNoSavedStations() {
        for (PlayerCallback callback: playerCallbacks) {
            callback.onNoSavedStations();
        }
    }

    public boolean handleSetChannel(int channelIndex) {
        Log.v(TAG, "Requesting board to play: " + getCurrentStation().getName());
        if (radio.play(getStationFromIndex(channelIndex).getChannelId())) {
            setCurrentChannelIndex(channelIndex);
            Log.v(TAG, "Approved, updating meta");
            updateMetadata(getCurrentStation());
            return true;
        }
        return false;
    }

    public void handlePlayRequest() {
        Log.v(TAG, "Handling a play request");
        if (radio.isConnected()) {
            Log.d(TAG, "is connected");

            if (!hasFocus()) {
                requestAudioFocus();
            }

            if (hasFocus()) {
                if (handleSetChannel(currentChannelIndex)) {
                    Log.v(TAG, "Updating playstate");
                    handleUnmuteRequest();
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                } else {
                    Log.v(TAG, "Board denied request");
                }
            }
        } else {
            openConnection();
        }
    }

    public void handleSetVolumeRequest(int newVolume) {
        volume = newVolume;
        radio.setVolume(volume);
        saveVolume();
    }

    public boolean handleMuteRequest() {
        muted = true;
        return radio.setVolume(0);
    }

    public void handleUnmuteRequest() {
        muted = false;
        radio.setVolume(volume);
    }

    public void handlePauseRequest() {
        while (!handleMuteRequest() && radio.isConnected()) {}
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
    }

    public void handleNextChannelRequest() {
        if (currentChannelIndex < getRadioStations().length - 1) {
            handleSetChannel(currentChannelIndex + 1);
        } else {
            handleSetChannel(0);
        }
    }

    public void handlePreviousChannelRequest() {
        if (currentChannelIndex > 0) {
            handleSetChannel(currentChannelIndex - 1);
        } else {
            handleSetChannel(getRadioStations().length - 1);
        }
    }

    public void handleFocusDuck() {
        radio.setVolume(duckVolume);
    }

    public void handleFocusGain() {
        radio.setVolume(volume);
    }

    public void handleFocusLost() {
        handlePauseRequest();
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
        }
    }

    public void updateMetadata(RadioStation station) {
        if (mediaSession != null) {
            mediaSession.setMetadata(
                    new MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, station.getName())
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, station.getEnsemble())
                            .putString(MediaMetadataCompat.METADATA_KEY_GENRE,
                                    RadioDevice.StringValues.getGenreFromId(station.getGenreId())
                            )
                            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, station.getChannelId())
                            .build()
            );
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
                handleFocusDuck();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                handleMuteRequest();
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                handleFocusGain();
                break;
        }
    }

    @Override
    public void onDestroy() {
        abandonAudioFocus();
        handlePauseRequest();
        radio.disconnect();
        playerNotification.cancel();
        mediaSession.release();
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            handlePlayRequest();
        }

        @Override
        public void onPause() {
            handleMuteRequest();
        }

        @Override
        public void onStop() {
            handlePauseRequest();
        }

        @Override
        public void onSkipToNext() {
            handleNextChannelRequest();
        }

        @Override
        public void onSkipToPrevious() {
            handlePreviousChannelRequest();
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

    private ListenerManager.ChannelScanListener channelScanListener =
            new ListenerManager.ChannelScanListener() {
        @Override
        public void onScanFinished() {

        }
    };

    public void registerCallback(PlayerCallback callback) {
        playerCallbacks.add(callback);
    }

    public void unregisterCallback(PlayerCallback callback) {
        playerCallbacks.remove(callback);
    }
}
