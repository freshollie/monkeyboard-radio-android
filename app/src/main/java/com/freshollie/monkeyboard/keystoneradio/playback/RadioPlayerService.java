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
import com.freshollie.monkeyboard.keystoneradio.radio.RadioDeviceListenerManager;
import com.freshollie.monkeyboard.keystoneradio.radio.RadioDevice;
import com.freshollie.monkeyboard.keystoneradio.radio.RadioStation;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    public final static String ACTION_SEARCH_FORWARDS =
            "com.freshollie.monkeyboarddab.playback.radioplayerservice.action.SEARCH_FORWARDS";
    public final static String ACTION_SEARCH_BACKWARDS =
            "com.freshollie.monkeyboarddab.playback.radioplayerservice.action.SEARCH_BACKWARDS";
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

    public final static String ACTION_SET_RADIO_MODE = "";

    private ExecutorService requestExecutor = Executors.newSingleThreadExecutor();

    private IBinder binder = new RadioPlayerBinder();

    private RadioPlayerNotification playerNotification;

    private RadioDevice radio;
    private RadioStation[] dabRadioStations = new RadioStation[0];
    private ArrayList<RadioStation> fmRadioStations = new ArrayList<>();

    private RadioStation currentFmRadioStation;

    private AudioManager audioManager;
    private MediaSessionCompat mediaSession;
    private VolumeProviderCompat volumeProvider;
    private PlaybackStateCompat.Builder playbackStateBuilder;

    private Runnable queuedAction;

    private int radioMode;
    private boolean playGranted;

    private SettingsVolumeObserver settingsVolumeObserver;

    private boolean controllerInput = false;

    private int volume = 13;
    private int duckVolume = 3;
    private boolean muted = false;
    private boolean ducked = false;

    public static int MAX_PLAYER_VOLUME = 15;
    private boolean actionComplete;


    private enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }

    private AudioFocus audioFocusState = AudioFocus.NoFocusNoDuck;

    private int currentDabChannelIndex = -1;
    private int currentFmFrequency = 88000;

    public interface PlayerCallback {
        void onPlayerVolumeChanged(int volume);

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
    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            if (s.equals(getString(R.string.DUCK_VOLUME_KEY))) {
                duckVolume =
                        sharedPreferences.getInt(
                                getString(R.string.DUCK_VOLUME_KEY),
                                3
                        );
                Log.v(TAG, "Duck volume set to " + String.valueOf(duckVolume));
            } else if (s.equals(getString(R.string.HEADUNIT_MAIN_INPUT_KEY))) {
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

            setDabStationList(
                    newStationList.toArray(
                            new RadioStation[newStationList.size()]
                    )
            );

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

        // Build a media session for the RadioPlayer
        mediaSession = new MediaSessionCompat(this, this.getClass().getSimpleName());
        mediaSession.setCallback(new MediaSessionCallback());
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        setupVolumeControls();

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

    private void loadPreferences() {
        Set<String> dabStationsJsonList =
                sharedPreferences.getStringSet(getString(R.string.DAB_STATION_LIST_KEY), null);

        Set<String> fmStationsJsonList =
                sharedPreferences.getStringSet(getString(R.string.FM_STATION_LIST_KEY), null);

        if (dabStationsJsonList != null) {
            dabRadioStations = new RadioStation[dabStationsJsonList.size()];
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
                        return Integer.valueOf(radioStation.getChannelFrequency())
                                .compareTo(t1.getChannelFrequency());
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
                int i = 0;
                for (String stationJsonString: fmStationsJsonList) {
                    JSONObject stationJson = new JSONObject(stationJsonString);

                    fmRadioStations.add(new RadioStation(stationJson));
                    i++;
                }

                sortFmRadioStations();

            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "Could not load dab station list");
            }
        }

        controllerInput =
                sharedPreferences.getBoolean(
                        getString(R.string.HEADUNIT_MAIN_INPUT_KEY),
                        false
                );
        currentDabChannelIndex = sharedPreferences.getInt(getString(R.string.DAB_CURRENT_CHANNEL_INDEX_KEY), 0);
        currentFmFrequency = sharedPreferences.getInt(getString(R.string.FM_CURRENT_FREQUENCY_KEY),
                RadioDevice.Values.MIN_FM_FREQUENCY);

        duckVolume = sharedPreferences.getInt(getString(R.string.DUCK_VOLUME_KEY), 3);

        // Gets the last radio mode preference
        radioMode = sharedPreferences.getBoolean(getString(R.string.RADIO_MODE_KEY), false)?
                RadioDevice.Values.STREAM_MODE_DAB:
                RadioDevice.Values.STREAM_MODE_FM;

        if (!sharedPreferences.getBoolean(getString(R.string.SYNC_VOLUME_KEY), true)) {
            volume = sharedPreferences.getInt(getString(R.string.VOLUME_KEY), 13);
        }
    }

    private void saveDabStationList() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Set<String> stringSet = new HashSet<>();
        for (RadioStation station: dabRadioStations) {
            stringSet.add(station.toJsonString());
        }

        editor.putStringSet(getString(R.string.DAB_STATION_LIST_KEY), stringSet);

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
        if (currentFmRadioStation != null) {
            for (RadioStation station: fmRadioStations) {
                if (((station.getChannelFrequency()) / 100) * 100
                        == ((currentFmRadioStation.getChannelFrequency()) / 100) * 100) {
                    return false;
                }
            }
            fmRadioStations.add(currentFmRadioStation);
            sortFmRadioStations();
            saveFmStationList();
            return true;
        }
        return false;
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
                return Integer.valueOf(radioStation.getChannelFrequency())
                        .compareTo(t1.getChannelFrequency());
            }
        });

        fmRadioStations = new ArrayList<>(Arrays.asList(radioStationsArray));
    }

    private void setDabStationList(RadioStation[] stationList) {
        dabRadioStations = stationList;
        saveDabStationList();
    }

    private void saveRadioMode() {
        sharedPreferences
                .edit()
                .putBoolean(getString(R.string.RADIO_MODE_KEY), radioMode == RadioDevice.Values.STREAM_MODE_DAB)
                .apply();
    }

    private void saveCurrentDabChannelFrequency() {
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
        radio.copyDabStationList(copyProgramsListener);
        setCurrentDabChannelIndex(0);
        notifyDabStationListCopyStart();
    }

    private void onConnectedSequence() {
        // If the our internal database is dramatically different to that on the board, we will try
        // and sync our copies

        radio.setStereoMode(1);

        if (queuedAction != null) {
            new Thread(queuedAction).start();
            queuedAction = null;
        }

        if (getRadioMode() == RadioDevice.Values.STREAM_MODE_DAB) {
            handleSetDabChannelRequest(getCurrentDabChannelIndex());
        } else {
            handleSetFmFrequencyRequest(getCurrentFmFrequency());
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

    public int getRadioMode() {
        return radioMode;
    }

    private void setRadioMode(int newRadioMode) {
        radioMode = newRadioMode;
        updateFmFrequencyMetadata(currentFmFrequency);
        saveRadioMode();
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
            if (station.getChannelFrequency() == channelId) {
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

    public int getCurrentSavedFmStationIndex() {
        if (getCurrentStation() != null) {
            RadioStation[] fmRadioStations = getFmRadioStations();
            DecimalFormat df = new DecimalFormat("#.00");

            for (int i = 0; i < fmRadioStations.length; i++) {
                if (df.format(fmRadioStations[i].getChannelFrequency() / 1000.0).equals(
                        df.format(getCurrentStation().getChannelFrequency() / 1000.0))) {
                    return i;
                }
            }
        }

        return -1;
    }

    private void setCurrentDabChannelIndex(int channelIndex) {
        currentDabChannelIndex = channelIndex;
        saveCurrentDabChannelFrequency();
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
        Runnable newRunnable = new Runnable() {
            @Override
            public void run() {
                action.run();
            }
        };


        if (radio.isConnected()) {
            requestExecutor.submit(newRunnable);
        } else {
            queuedAction = action;
            openConnection();
        }
    }

    private boolean isActionComplete() {
        return actionComplete;
    }

    private boolean updateBoardFmFrequencyAction() {
        Log.v(TAG, "Requesting board to play: " + currentFmFrequency);
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

    private boolean updateBoardDabChannelAction() {
        if (Math.abs(getDabRadioStations().length - radio.getTotalPrograms()) > 8 ||
                getDabRadioStations().length < 1) {
            if (radio.getTotalPrograms() > 0) {
                startDabStationListCopyTask();

            } else {
                Log.v(TAG, "No stations stored, need to perform channel search");
                notifyNoStoredStations();
            }

        } else if (getDabRadioStations().length > 0) {
            Log.v(TAG, "Requesting board to play: " + getCurrentStation().getName());
            if (radio.play(getRadioMode(), currentDabChannelIndex)) {
                Log.v(TAG, "Approved, updating meta");
                updateMetadata(getCurrentStation());
                return true;
            }
        }
        return false;
    }

    public void handleSetDabChannelRequest(final int channelFrequency) {
        // Saves the new current channel
        setCurrentDabChannelIndex(channelFrequency);

        handleAction(new Runnable() {
                @Override
                public void run() {
                    // Only execute final thread
                    if (channelFrequency == dabRadioStations[currentDabChannelIndex]
                            .getChannelFrequency()) {
                        updateBoardDabChannelAction();
                    }
                }
        });
    }

    public void handleSetFmFrequencyRequest(final int frequency) {
        setCurrentFmChannelFrequency(frequency);

        handleAction(new Runnable() {
            @Override
            public void run() {
                // Only execute the final thread
                if (frequency == currentFmFrequency) {
                    updateBoardFmFrequencyAction();
                }
            }
        });
    }

    public void handlePlayRequest() {
        Log.v(TAG, "Handling a play request");
        int channel = -1;
        if (getRadioMode() == RadioDevice.Values.STREAM_MODE_DAB) {
            if (currentDabChannelIndex == -1 || getDabRadioStations().length < 1) {
                Log.v(TAG, "No station to play, ignoring request");
                handleAction(new Runnable() {
                    @Override
                    public void run() {
                        updateBoardDabChannelAction();
                    }
                });
            } else {
                channel = currentDabChannelIndex;
            }
        } else {
            channel = currentFmFrequency;
        }

        if (channel != -1) {
            handleAction(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (!hasFocus()) {
                                requestAudioFocus();
                            }

                            if (hasFocus()) {
                                boolean granted;
                                if (getRadioMode() == RadioDevice.Values.STREAM_MODE_DAB) {
                                    granted = updateBoardDabChannelAction();

                                } else {
                                    granted = updateBoardFmFrequencyAction();
                                }

                                if (granted) {
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
        while (!radio.setVolume(0) && radio.isConnected()){}

        if (radio.isConnected() && radio.getPlayStatus() == RadioDevice.Values.PLAY_STATUS_SEARCHING) {
            radio.stopSearch();
        }
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
    }


    public void handleNextChannelRequest() {
        handleAction(
                new Runnable() {
                    @Override
                    public void run() {
                        if (getRadioMode() == RadioDevice.Values.STREAM_MODE_DAB) {
                            if (currentDabChannelIndex < getDabRadioStations().length - 1) {
                                handleSetDabChannelRequest(currentDabChannelIndex + 1);
                            } else {
                                handleSetDabChannelRequest(0);
                            }
                        } else {
                            int currentFmChannelIndex = getCurrentSavedFmStationIndex();
                            RadioStation[] radioStations = getFmRadioStations();

                            // We have saved stations
                            if (radioStations.length > 0) {
                                int nextChannelIndex = -1;

                                // We are not currently on a station
                                if (currentFmChannelIndex < 0) {
                                    for (int i = 0; i < radioStations.length; i++) {
                                        if (radioStations[i].getChannelFrequency() >
                                                getCurrentStation().getChannelFrequency()) {
                                            nextChannelIndex = i;
                                            break;
                                        }
                                    }
                                } else {
                                    if (currentFmChannelIndex < radioStations.length - 1) {
                                        nextChannelIndex = currentFmChannelIndex + 1;
                                    }
                                }

                                if (nextChannelIndex != -1) {
                                    handleSetFmFrequencyRequest(
                                            radioStations[nextChannelIndex].getChannelFrequency()
                                    );
                                } else {
                                    handlePlayRequest();
                                }
                            }
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
                        if (getRadioMode() == RadioDevice.Values.STREAM_MODE_DAB) {
                            if (currentDabChannelIndex > 0) {
                                handleSetDabChannelRequest(currentDabChannelIndex - 1);
                            } else {
                                handleSetDabChannelRequest(getDabRadioStations().length - 1);
                            }
                        } else {
                            int currentFmChannelIndex = getCurrentSavedFmStationIndex();
                            RadioStation[] radioStations = getFmRadioStations();

                            // We have saved stations
                            if (radioStations.length > 0) {
                                int nextChannelIndex = -1;

                                // We are not currently on a station
                                if (currentFmChannelIndex < 0) {
                                    // Go through the stations backwards until we find the next lowest station
                                    for (int i = radioStations.length - 1; i > -1; i--) {
                                        if (radioStations[i].getChannelFrequency() <
                                                getCurrentStation().getChannelFrequency()) {
                                            nextChannelIndex = i;
                                            break;
                                        }
                                    }

                                } else {
                                    if (currentFmChannelIndex > 0) {
                                        nextChannelIndex = currentFmChannelIndex - 1;
                                    }
                                }

                                // If we dont get a next channel index then we don't change channel

                                if (nextChannelIndex != -1) {
                                    handleSetFmFrequencyRequest(
                                            radioStations[nextChannelIndex].getChannelFrequency()
                                    );
                                } else {
                                    handlePlayRequest();
                                }
                            }
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

    public void handleSetRadioMode(int newRadioMode) {
        setRadioMode(newRadioMode);
        if (newRadioMode == RadioDevice.Values.STREAM_MODE_DAB) {
            handleSetDabChannelRequest(currentDabChannelIndex);
        } else {
            handleSetFmFrequencyRequest(currentFmFrequency);
        }
    }

    public void handleSearchForwards() {
        handleFmSearch(RadioDevice.Values.SEARCH_FORWARDS);
    }

    public void handleSearchBackwards() {
        handleFmSearch(RadioDevice.Values.SEARCH_BACKWARDS);
    }

    public void handleFmSearch(final int direction) {
        handleAction(new Runnable() {
            @Override
            public void run() {
                radio.stopSearch();
                radio.search(direction);
            }
        });
    }

    public void startDabChannelSearchTask() {
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

    private void updateFmFrequencyMetadata(int frequency) {
        currentFmRadioStation = new RadioStation();
        DecimalFormat df = new DecimalFormat("#.00");

        currentFmRadioStation.setName(
                getString(R.string.fm_frequency_placeholder, df.format(frequency / 1000.0))
        );
        currentFmRadioStation.setChannelFrequency(frequency);

        updateMetadata(currentFmRadioStation);
    }

    private void updateFmStationNameMetadata(String name) {
        if (currentFmRadioStation == null) {
            updateFmFrequencyMetadata(currentFmFrequency);
        }

        currentFmRadioStation.setName(name);
        updateMetadata(currentFmRadioStation);
    }

    private void updateFmStationTypeMetadata(int genreId) {
        if (currentFmRadioStation == null) {
            updateFmFrequencyMetadata(currentFmFrequency);
        }

        currentFmRadioStation.setGenreId(genreId);
        updateMetadata(currentFmRadioStation);
    }

    private void updateMetadata(RadioStation station) {
        if (mediaSession != null) {
            if (getMediaController().getMetadata() != null) {
                if (getMediaController()
                        .getMetadata()
                        .getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER)
                        == station.getChannelFrequency()) {
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
                                    station.getChannelFrequency())
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

    private boolean searchPauseState = false;

    private RadioDeviceListenerManager.DataListener dataListener =
            new RadioDeviceListenerManager.DataListener() {
        @Override
        public void onProgramTextChanged(String programText) {

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
            Log.v(TAG, "Search frequency changed");
            setCurrentFmChannelFrequency(frequency);
            updateFmFrequencyMetadata(frequency);
        }

        @Override
        public void onFmProgramNameUpdated(String newFmProgramName) {
            updateFmStationNameMetadata(newFmProgramName);
        }

        @Override
        public void onFmProgramTypeUpdated(int newFmProgramType) {
            updateFmStationTypeMetadata(newFmProgramType);
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
