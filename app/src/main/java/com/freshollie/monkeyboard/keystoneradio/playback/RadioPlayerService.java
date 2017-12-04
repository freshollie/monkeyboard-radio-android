/*
 * Created by Oliver Bell on 14/01/2017
 * Copyright (c) 2017. by Oliver bell <freshollie@gmail.com>
 *
 * Last modified 15/06/17 21:50
 */

package com.freshollie.monkeyboard.keystoneradio.playback;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
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

import com.freshollie.monkeyboard.keystoneradio.R;
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
 * Radio Player Service manages the control of the radio.
 *
 */
public class RadioPlayerService extends Service implements  {
    private static String TAG = RadioPlayerService.class.getSimpleName();

    // Actions
    public final static String ACTION_SEARCH_FORWARDS =
            "com.freshollie.monkeyboard.keystoneradio.playback.SEARCH_FORWARDS";
    public final static String ACTION_SEARCH_BACKWARDS =
            "com.freshollie.monkeyboard.keystoneradio.playback.SEARCH_BACKWARDS";
    public final static String ACTION_NEXT =
            "com.freshollie.monkeyboard.keystoneradio.playback.NEXT";
    public final static String ACTION_PREVIOUS =
            "com.freshollie.monkeyboard.keystoneradio.playback.PREVIOUS";
    public final static String ACTION_STOP =
            "com.freshollie.monkeyboard.keystoneradio.playback.STOP";
    public final static String ACTION_PLAY =
            "com.freshollie.monkeyboard.keystoneradio.playback.PLAY";
    public final static String ACTION_PAUSE =
            "com.freshollie.monkeyboard.keystoneradio.playback.PAUSE";
    public final static String ACTION_SET_RADIO_MODE = "";

    private IBinder binder = new RadioPlayerBinder();

    private RadioStation[] dabRadioStations = new RadioStation[0];
    private int totalCollectedDabStations = 0;
    private int radioTotalStoredPrograms;

    private ArrayList<RadioStation> fmRadioStations = new ArrayList<>();

    private RadioStation currentFmRadioStation;

    private AudioManager audioManager;
    private MediaSessionCompat mediaSession;
    private VolumeProviderCompat volumeProvider;
    private PlaybackStateCompat.Builder playbackStateBuilder;

    private Runnable queuedAction;

    private int radioMode;
    private boolean playGranted;

    private boolean actionComplete;
    private boolean copyTaskRunning = false;

    private int currentDabChannelIndex = -1;
    private int currentFmFrequency = 88000;

    public interface PlayerCallback {
        void onPlayerVolumeChanged(int volume);

        void onRadioModeChanged(int radioMode);

        void onNoStoredStations();
        void onDeviceAttachTimeout();

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

    private RadioDevice.CopyProgramsListener copyProgramsListener =
            new RadioDevice.CopyProgramsListener() {
        @Override
        public void onProgressUpdate(int progress, int max) {
            Log.v(TAG, String.format("Collected %s/%s", progress, max));
            notifyStationListCopyProgressUpdate(progress, max);
        }

        @Override
        public void onComplete(RadioStation[] collectedRadioStations) {
            Log.v(TAG, "Collected all stations");
            ArrayList<RadioStation> newStationList = new ArrayList<>();


            for (RadioStation radioStation: collectedRadioStations) {
                if (radioStation.getGenreId() != -1) {
                    newStationList.add(radioStation);
                }
            }

            setDabStationList(
                    newStationList.toArray(
                            new RadioStation[newStationList.size()]
                    ),
                    collectedRadioStations.length
            );

            copyTaskRunning = false;

            if (dabRadioStations.length > 0) {
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
            startDabStationListCopyTask();
            notifyChannelSearchComplete(numPrograms);
        }

        @Override
        public void onStarted() {
            notifyChannelSearchStart();
        }
    };

    private RadioDeviceListenerManager.ConnectionStateChangeListener connectionStateListener =
            new RadioDeviceListenerManager.ConnectionStateChangeListener() {
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
            }

            updatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
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

        // Get audio manager
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if (!sharedPreferences.getBoolean(getString(R.string.pref_sync_volume_key), true)) {
            volume = sharedPreferences.getInt(getString(R.string.saved_volume_key), 13);
        } else {
            // Set the player volume to the system volume
            volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }

        Log.d(TAG, "Start volume: " + volume);

        // Build a media session for the RadioPlayer
        mediaSession = new MediaSessionCompat(this, this.getClass().getSimpleName());
        mediaSession.setCallback(new MediaSessionCallback());
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        setupVolumeControls();

        // Build a playback state for the player
        playbackStateBuilder = new PlaybackStateCompat.Builder();

        // Update info of the current session to initial state
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
        createNewPlaybackMetadataForStation(new RadioStation());

        // Connect to the radio API
        radio = new RadioDevice(getApplicationContext());

        // Register listeners of the radio
        radio.getListenerManager().registerDataListener(dataListener);
        radio.getListenerManager().registerConnectionStateChangedListener(connectionStateListener);

        saveVolume(volume);

        loadPreferences();

        // Start the process of connecting to the radio device
        openConnection();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Got an intent");
        if (intent != null && intent.getAction() != null) {
            Log.v(TAG, "Received intent:" + intent.getAction());

            switch (intent.getAction()) {
                case ACTION_STOP:
                    notifyDismissed();
                    closeConnection();
                    break;
                case ACTION_SEARCH_FORWARDS:
                    handleSearchForwards();
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
                case ACTION_SEARCH_BACKWARDS:
                    handleSearchBackwards();
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

    private void setupVolumeControls() {
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
    }

    public static int modulus(int a, int b) {
        return (a % b + b) % b;
    }

    private void loadPreferences() {
        Set<String> dabStationsJsonList =
                sharedPreferences.getStringSet(getString(R.string.DAB_STATION_LIST_KEY), null);

        Set<String> fmStationsJsonList =
                sharedPreferences.getStringSet(getString(R.string.FM_STATION_LIST_KEY), null);

        if (dabStationsJsonList != null) {
            dabRadioStations = new RadioStation[dabStationsJsonList.size()];
            totalCollectedDabStations = sharedPreferences.getInt(
                    getString(R.string.TOTAL_COLLECTED_DAB_STATIONS_KEY),
                    0
            );

            try {
                int i = 0;
                for (String stationJsonString: dabStationsJsonList) {
                    JSONObject stationJson = new JSONObject(stationJsonString);

                    dabRadioStations[i] = new RadioStation(stationJson);
                    i++;
                }

                Arrays.sort(dabRadioStations, new Comparator<RadioStation>() {
                    @Override
                    public int compare(RadioStation radioStation, RadioStation t1) {
                        return Integer.valueOf(radioStation.getFrequency())
                                .compareTo(t1.getFrequency());
                    }
                });

            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "Could not load dab station list");
            }
        } else {
            dabRadioStations = new RadioStation[0];
        }

        if (fmStationsJsonList != null) {
            fmRadioStations.clear();
            try {
                for (String stationJsonString: fmStationsJsonList) {
                    JSONObject stationJson = new JSONObject(stationJsonString);

                    fmRadioStations.add(new RadioStation(stationJson));
                }

                sortFmRadioStations();

            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "Could not load dab station list");
            }
        }

        currentDabChannelIndex = sharedPreferences.getInt(getString(R.string.DAB_CURRENT_CHANNEL_INDEX_KEY), 0);
        currentFmFrequency = sharedPreferences.getInt(getString(R.string.FM_CURRENT_FREQUENCY_KEY),
                RadioDevice.Values.MIN_FM_FREQUENCY);

        // Gets the last radio mode preference
        radioMode = sharedPreferences.getBoolean(getString(R.string.RADIO_MODE_KEY), false)?
                RadioDevice.Values.STREAM_MODE_DAB:
                RadioDevice.Values.STREAM_MODE_FM;

        if (!sharedPreferences.getBoolean(getString(R.string.pref_sync_volume_key), true)) {
            volume = sharedPreferences.getInt(getString(R.string.saved_volume_key), 13);
        }
    }

    private void saveDabStationList() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Set<String> stringSet = new HashSet<>();
        for (RadioStation station: dabRadioStations) {
            stringSet.add(station.toJsonString());
        }

        editor.putStringSet(getString(R.string.DAB_STATION_LIST_KEY), stringSet);

        editor.putInt(getString(R.string.TOTAL_COLLECTED_DAB_STATIONS_KEY), totalCollectedDabStations);

        editor.apply();
    }

    private void saveFmStationList() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Set<String> stringSet = new HashSet<>();
        for (RadioStation station: fmRadioStations) {
            stringSet.add(station.toJsonString());
        }

        editor.putStringSet(getString(R.string.FM_STATION_LIST_KEY), stringSet);

        editor.apply();
    }

    public boolean saveCurrentFmStation() {
        RadioStation radioStationCopy = new RadioStation();
        radioStationCopy.setName(currentFmRadioStation.getName());
        radioStationCopy.setGenreId(currentFmRadioStation.getGenreId());
        radioStationCopy.setFrequency(currentFmRadioStation.getFrequency());

        if (currentFmRadioStation != null) {
            for (RadioStation station: fmRadioStations) {
                if (((station.getFrequency()) / 100) * 100
                        == ((radioStationCopy.getFrequency()) / 100) * 100) {
                    return false;
                }
            }
            fmRadioStations.add(radioStationCopy);
            sortFmRadioStations();
            saveFmStationList();
            return true;
        }
        return false;
    }

    public void handleClearFmRadioStations() {
        fmRadioStations.clear();
        saveFmStationList();
    }
    public void removeFmRadioStation(RadioStation radioStation) {
        fmRadioStations.remove(radioStation);
        saveFmStationList();
    }

    private void sortFmRadioStations() {
        RadioStation[] radioStationsArray =
                fmRadioStations.toArray(new RadioStation[fmRadioStations.size()]);

        Arrays.sort(radioStationsArray, new Comparator<RadioStation>() {
            @Override
            public int compare(RadioStation radioStation, RadioStation t1) {
                return Integer.valueOf(radioStation.getFrequency())
                        .compareTo(t1.getFrequency());
            }
        });

        fmRadioStations = new ArrayList<>(Arrays.asList(radioStationsArray));
    }

    private void setDabStationList(RadioStation[] stationList, int totalCollectedPrograms) {
        dabRadioStations = stationList;
        totalCollectedDabStations = totalCollectedPrograms;
        saveDabStationList();
    }

    private void saveRadioMode() {
        sharedPreferences
                .edit()
                .putBoolean(getString(R.string.RADIO_MODE_KEY), radioMode == RadioDevice.Values.STREAM_MODE_DAB)
                .apply();
    }

    private void saveCurrentDabChannelIndex() {
        Log.v(TAG, "Saving DAB index " + currentDabChannelIndex);
        sharedPreferences.edit()
                .putInt(getString(R.string.DAB_CURRENT_CHANNEL_INDEX_KEY), currentDabChannelIndex)
                .apply();
    }

    private void saveCurrentFmFrequency() {
        sharedPreferences.edit()
                .putInt(getString(R.string.FM_CURRENT_FREQUENCY_KEY), currentFmFrequency)
                .apply();
    }

    public void startDabStationListCopyTask() {
        if (!copyTaskRunning) {
            copyTaskRunning = true;
            radio.copyDabStationList(copyProgramsListener);
            setCurrentDabChannelIndex(0);
            notifyDabStationListCopyStart();
        }
    }

    private void onConnectedSequence() {
        handleAction(new Runnable() {
            @Override
            public void run() {
                radio.waitForReady();
                // When the radio first connects, try to get the number of programs stored,
                // and set the stereo mode to stereo
                radioTotalStoredPrograms = radio.getTotalPrograms();

                updateBoardStereoModeAction();
            }
        });

        if (playerNotification != null) {
            playerNotification.cancel();
        }

        playerNotification = new RadioPlayerNotification(this);

        if (queuedAction != null) {
            handleAction(queuedAction);
            queuedAction = null;
        }

        if (getRadioMode() == RadioDevice.Values.STREAM_MODE_DAB) {
            handleSetDabChannelRequest(getCurrentDabChannelIndex());
        } else {
            handleSetFmFrequencyRequest(getCurrentFmFrequency());
        }

        if (getPlaybackState() == PlaybackStateCompat.STATE_PLAYING) {
            handlePlayRequest();
        }
    }

    public void openConnection() {
        if (!waitForAttachThread.isAlive() && !openingConnection) {
            Log.v(TAG, "Starting device connection");

            if (playerNotification != null) {
                playerNotification.cancel();
            }

            playerNotification = new RadioPlayerNotification(this);

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

    public int getRadioMode() {
        return radioMode;
    }

    private void setRadioMode(int newRadioMode) {
        radioMode = newRadioMode;
        currentStereoMode = -1;
        updateFmFrequencyMetadata(currentFmFrequency);
        saveRadioMode();
    }

    public int getPlayerVolume() {
        int volume = this.volume;

        if (sharedPreferences.getBoolean(getString(R.string.pref_sync_volume_key), true)) {
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
        editor.putInt(getString(R.string.saved_volume_key), volume);
        editor.apply();

        if (sharedPreferences.getBoolean(getString(R.string.pref_sync_volume_key), true)) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        }

        if (volumeProvider != null) {
            volumeProvider.setCurrentVolume(volume);
        }
    }

    public RadioStation getCurrentStation() {
        if (getRadioMode() == RadioDevice.Values.STREAM_MODE_DAB) {
            if (dabRadioStations.length > 0) {
                return dabRadioStations[getCurrentDabChannelIndex()];
            } else {
                return null;
            }
        } else {
            return currentFmRadioStation;
        }
    }

    public RadioStation getStationFromId(int channelId) {
        for (RadioStation station: dabRadioStations) {
            if (station.getFrequency() == channelId) {
                return station;
            }
        }
        return null;
    }

    public RadioStation getStationFromIndex(int stationIndex) {
        return dabRadioStations[stationIndex];
    }

    public RadioStation[] getDabRadioStations() {
        return dabRadioStations;
    }

    public RadioStation[] getFmRadioStations() {
        return fmRadioStations.toArray(new RadioStation[fmRadioStations.size()]);
    }

    public int getCurrentDabChannelIndex() {
        return currentDabChannelIndex;
    }

    public int getCurrentFmFrequency() {
        return currentFmFrequency;
    }

    /**
     * Returns the first saved station that contains the same name or is the same frequency.
     */
    public int getCurrentSavedFmStationIndex() {
        if (getCurrentStation() != null) {

            for (int i = 0; i < fmRadioStations.size(); i++) {
                if ((fmRadioStations.get(i).getName().equals(getCurrentStation().getName()) &&
                        !getCurrentStation().getName().isEmpty()) ||
                        currentFmFrequency / 100 == fmRadioStations.get(i).getFrequency() / 100
                        ) {
                    return i;
                }
            }
        }

        return -1;
    }

    private void setCurrentDabChannelIndex(int channelIndex) {
        currentDabChannelIndex = channelIndex;
        saveCurrentDabChannelIndex();
    }

    private void setCurrentFmChannelFrequency(int frequency) {
        currentFmFrequency = frequency;
        saveCurrentFmFrequency();
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

    /**
     * Handle action makes sure that requests are executed sequentially.
     * @param action the request the needs to be fulfilled.
     */
    private void handleAction(final Runnable action) {
        if (radio.isConnected()) {
            requestExecutor.submit(action);
        } else {
            queuedAction = action;
            openConnection();
        }
    }

    private void updateBoardStereoModeAction() {
        int stereoMode = 0;

        if (sharedPreferences.getBoolean(getString(R.string.pref_fm_stereo_enabled_key), false) ||
                getRadioMode() == RadioDevice.Values.STREAM_MODE_DAB) {
            stereoMode = 1;
        }

        if (currentStereoMode != stereoMode) {
            if (radio.setStereoMode(stereoMode)) {
                currentStereoMode = stereoMode;
            }
        }
    }

    private boolean updateBoardFmFrequencyAction() {
        Log.v(TAG, "Requesting board to set channel to: " + currentFmFrequency);
        if (radio.getPlayStatus() == RadioDevice.Values.PLAY_STATUS_SEARCHING) {
            radio.stopSearch();
        }

        if (radio.play(getRadioMode(), currentFmFrequency)) {
            Log.v(TAG, "Approved, updating meta");
            updateFmFrequencyMetadata(currentFmFrequency);
            return true;
        }
        return false;
    }

    /**
     * Updates the radio board to play the currently selected DAB station.
     *
     * If there is a large difference between the number of programs and the number of stored
     * programs, the stations will be copied from the board, if there are no stations on the board
     * a search will be performed
     */
    private boolean updateBoardDabChannelAction() {
        // Make sure we are up to date
        if (totalCollectedDabStations != radioTotalStoredPrograms) {
            radio.waitForReady();
            radioTotalStoredPrograms = radio.getTotalPrograms();
        }

        // If the our internal database is dramatically different to that on the board, we will try
        // and sync our copies
        if (getDabRadioStations().length < 1 ||
                totalCollectedDabStations != radioTotalStoredPrograms) {

            if (radioTotalStoredPrograms > 0) {
                if (getDabRadioStations().length < 1) {
                    Log.v(TAG, "No stations stored on device");
                } else {
                    Log.v(TAG, "" + totalCollectedDabStations);
                    Log.v(TAG, "" + radioTotalStoredPrograms);
                }
                startDabStationListCopyTask();
            } else {
                Log.v(TAG, "No stations stored, need to perform channel search");
                notifyNoStoredStations();
            }
        } else if (getDabRadioStations().length > 0) {

            Log.v(TAG, "Requesting board to set channel to: " + getCurrentStation().getName());
            if (radio.play(getRadioMode(), dabRadioStations[currentDabChannelIndex].getFrequency())) {
                Log.v(TAG, "Approved, updating meta");
                createNewPlaybackMetadataForStation(getCurrentStation());
                return true;
            }
        }
        return false;
    }

    public void handleSetDabChannelRequest(final int channelIndex) {
        if (channelIndex < 0 || channelIndex >= dabRadioStations.length) {
            return;
        }

        // Saves the new current channel
        setCurrentDabChannelIndex(channelIndex);

        if (radio.isAttached()) {
            // Board is attached so try and connect and perform
            // and confirm channel change before updating metadata
            handleAction(new Runnable() {
                @Override
                public void run() {
                    // Only execute final thread
                    if (channelIndex == currentDabChannelIndex) {
                        updateBoardDabChannelAction();
                    }
                }
            });
        } else {
            createNewPlaybackMetadataForStation(getCurrentStation());
        }
    }

    public void handleSetFmFrequencyRequest(final int frequency) {
        setCurrentFmChannelFrequency(frequency);

        if (radio.isAttached()) {
            handleAction(new Runnable() {
                @Override
                public void run() {
                    // Only execute the final thread
                    if (frequency == currentFmFrequency) {
                        updateBoardFmFrequencyAction();
                    }
                }
            });
        } else {
            updateFmFrequencyMetadata(currentFmFrequency);
        }
    }



    public void startDabChannelSearchTask() {
        dabRadioStations = new RadioStation[0];
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
        }
    }

    private void updateFmFrequencyMetadata(int frequency) {
        int actualFrequency = frequency / 100 * 100;

        if (currentFmRadioStation != null) {
            if (actualFrequency == currentFmRadioStation.getFrequency()) {
                return;
            }
        }

        currentFmRadioStation = new RadioStation();
        currentFmRadioStation.setFrequency(actualFrequency);

        // We are already on a saved station so take our saved name
        if (getCurrentSavedFmStationIndex() > -1) {
            currentFmRadioStation.setName(fmRadioStations.get(getCurrentSavedFmStationIndex()).getName());
        }

        createNewPlaybackMetadataForStation(currentFmRadioStation);
    }

    private void updateFmStationNameMetadata(String name) {
        if (currentFmRadioStation == null) {
            updateFmFrequencyMetadata(currentFmFrequency);
        }

        currentFmRadioStation.setName(name);

        if (getCurrentSavedFmStationIndex() > -1) {
            if (!fmRadioStations.get(getCurrentSavedFmStationIndex()).getName().equals(name)) {
                fmRadioStations.get(getCurrentSavedFmStationIndex()).setName(name);
                saveFmStationList();
            }
        }

        createNewPlaybackMetadataForStation(currentFmRadioStation);
    }

    private void updateFmStationTypeMetadata(int genreId) {
        if (currentFmRadioStation == null) {
            updateFmFrequencyMetadata(currentFmFrequency);
        }

        currentFmRadioStation.setGenreId(genreId);
        createNewPlaybackMetadataForStation(currentFmRadioStation);
    }

    private void addImageToPlaybackMetadata(Bitmap slideshowImage) {
        if (mediaSession != null) {
            mediaSession.setMetadata(
                new MediaMetadataCompat.Builder(getMetadata())
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ART,
                                slideshowImage)
                        .build()
            );
        }
    }

    private void addProgramTextToPlaybackMetadata(String programText) {
        if (mediaSession != null) {
            mediaSession.setMetadata(
                    new MediaMetadataCompat.Builder(getMetadata())
                            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION,
                                    programText)
                            .build()
            );
        }
    }

    private void createNewPlaybackMetadataForStation(RadioStation station) {
        if (mediaSession != null) {
            if (getMediaController().getMetadata() != null) {
                // Check for infinite loops due to callbacks
                if (getMediaController()
                        .getMetadata()
                        .getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER)
                        == station.getFrequency()) {
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
                                    station.getFrequency())
                            .build()
            );
        }
    }



    public void closeConnection() {
        if (waitForAttachThread.isAlive()) {
            waitForAttachThread.interrupt();
        }

        if (playerNotification != null) {
            playerNotification.cancel();
        }
        playerNotification = null;

        abandonAudioFocus();

        if (radio.isConnected()) {
            handlePauseRequest();
            radio.disconnect();
        }
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "Stopping service");
        closeConnection();
        if (waitForAttachThread.isAlive()) {
            waitForAttachThread.interrupt();
        }

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
    }



    private boolean searchPauseState = false;

    private RadioDeviceListenerManager.DataListener dataListener =
            new RadioDeviceListenerManager.DataListener() {
        @Override
        public void onNewProgramText(String programText) {
            addProgramTextToPlaybackMetadata(programText);
        }

        @Override
        public void onPlayStatusChanged(int playStatus) {
            if (radioMode == RadioDevice.Values.STREAM_MODE_FM) {
                if (playStatus == RadioDevice.Values.PLAY_STATUS_SEARCHING &&
                        isPlaying()) {
                    searchPauseState = true;
                    updatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
                } else {
                    if (!isPlaying() && playStatus != RadioDevice.Values.PLAY_STATUS_STREAM_STOP &&
                            playStatus != RadioDevice.Values.PLAY_STATUS_SEARCHING &&
                            searchPauseState) {
                        searchPauseState = false;
                        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                    }
                }
            } else if (searchPauseState) {
                searchPauseState = false;
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
            }
        }

        @Override
        public void onDabSignalQualityChanged(int signalStrength) {

        }

        @Override
        public void onDabProgramDataRateChanged(int dataRate) {

        }

        @Override
        public void onRadioVolumeChanged(int volume) {

        }

        @Override
        public void onStereoStateChanged(int stereoState) {

        }

        @Override
        public void onFmSignalStrengthChanged(int signalStrength) {

        }

        @Override
        public void onFmSearchFrequencyChanged(int frequency) {
            if (radioMode == RadioDevice.Values.STREAM_MODE_FM) {
                Log.v(TAG, "Search frequency changed");
                setCurrentFmChannelFrequency(frequency);
                updateFmFrequencyMetadata(frequency);
            }
        }

        @Override
        public void onFmProgramNameUpdated(String newFmProgramName) {
            updateFmStationNameMetadata(newFmProgramName);
        }

        @Override
        public void onFmProgramTypeUpdated(int newFmProgramType) {
            updateFmStationTypeMetadata(newFmProgramType);
        }

        @Override
        public void onNewSlideshowImage(Bitmap bitmap) {
            addImageToPlaybackMetadata(bitmap);
        }
    };

    public void registerCallback(PlayerCallback callback) {
        playerCallbacks.add(callback);
    }

    public void unregisterCallback(PlayerCallback callback) {
        playerCallbacks.remove(callback);
    }

    private void notifyRadioModeChanged(int radioMode) {
        for (PlayerCallback playerCallback: playerCallbacks) {
            playerCallback.onRadioModeChanged(radioMode);
        }
    }

    private void notifyPlayerVolumeChanged(int newVolume) {
        for (PlayerCallback playerCallback: playerCallbacks) {
            playerCallback.onPlayerVolumeChanged(newVolume);
        }
    }

    private void notifyAttachTimeout() {
        Log.v(TAG, "Timed out waiting for device to attach");
        for (PlayerCallback playerCallback: playerCallbacks) {
            playerCallback.onDeviceAttachTimeout();
        }
    }

    private void notifyDabStationListCopyStart() {
        for (PlayerCallback callback: playerCallbacks) {
            callback.onStationListCopyStart();
        }
    }

    private void notifyChannelSearchStart() {
        for (final PlayerCallback callback: playerCallbacks) {
            new Handler(getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    callback.onSearchStart();
                }
            });
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
