package com.freshollie.monkeyboarddabradio.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.freshollie.monkeyboarddabradio.R;
import com.freshollie.monkeyboarddabradio.playback.RadioPlayerService;
import com.freshollie.monkeyboarddabradio.radio.ListenerManager;
import com.freshollie.monkeyboarddabradio.radio.RadioDevice;
import com.freshollie.monkeyboarddabradio.radio.RadioStation;

import java.util.Arrays;

public class PlayerActivity extends AppCompatActivity implements ListenerManager.DataListener,
        RadioPlayerService.PlayerCallback {
    private String TAG = this.getClass().getSimpleName();

    public static final String ACTION_SEND_KEYEVENT =
            "com.freshollie.headunitcontroller.action.SEND_KEYEVENT";

    private RadioPlayerService playerService;
    private Boolean playerBound = false;
    private RadioDevice radio;

    private ImageButton nextButton;
    private ImageButton previousButton;
    private ImageButton playPauseButton;
    private ImageButton volumeButton;
    private ImageButton settingsButton;

    private TextView currentChannelView;
    private TextView programTextTextView;
    private TextView signalStrengthView;
    private TextView playStatusView;
    private TextView genreTextView;
    private TextView ensembleTextView;
    private TextView dataRateTextView;
    private TextView stereoStateTextView;
    private ImageView signalStrengthIcon;

    private RecyclerView stationListRecyclerView;
    private StationListAdapter stationListAdapter = new StationListAdapter(this);
    private StationListLayoutManager stationListLayoutManager;

    private RadioStatusDialog radioStatusDialog = new RadioStatusDialog();

    private SharedPreferences sharedPreferences;

    private RecyclerView.OnScrollListener stationListScrollListener;
    private Runnable cursorScrollRunnable;
    private Runnable selectChannelScrollRunnable;

    private boolean controllerInput = false;
    private boolean cursorScrollWrap = true;

    private BroadcastReceiver controlInputReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_SEND_KEYEVENT)) {
                if (intent.hasExtra("keyCode") && controllerInput) {
                    handleKeyDown(intent.getIntExtra("keyCode", -1));
                }
            }
        }
    };

    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            if (s.equals(getString(R.string.SCROLL_WRAP_KEY))) {
                cursorScrollWrap =
                        sharedPreferences.getBoolean(
                                getString(R.string.SCROLL_WRAP_KEY),
                                false
                        );
                Log.v(TAG, "Cursor Scroll wrap set to: " + String.valueOf(cursorScrollWrap));
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

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to Player, cast the IBinder and get RadioPlayerService instance
            RadioPlayerService.RadioPlayerBinder binder = (RadioPlayerService.RadioPlayerBinder) service;
            playerService = binder.getService();
            radio = playerService.getRadio();
            playerBound = true;

            onPlaybackServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            playerBound = false;
            playerService = null;
            radio = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        sharedPreferences = getSharedPreferences(getString(R.string.SHARED_PREFERENCES_KEY), Context.MODE_PRIVATE);
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        controllerInput = sharedPreferences.getBoolean(
                getString(R.string.HEADUNIT_MAIN_INPUT_KEY),
                false
        );

        cursorScrollWrap = sharedPreferences.getBoolean(
                getString(R.string.SCROLL_WRAP_KEY),
                false
        );

        bindPlayerService();

        setupPlayerAttributes();
        setupStationList();
        clearPlayerAttributes();
    }

    public void bindPlayerService() {
        startService(new Intent(this, RadioPlayerService.class));

        bindService(
                new Intent(this, RadioPlayerService.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "On Resume");
        registerReceiver(controlInputReceiver, new IntentFilter(ACTION_SEND_KEYEVENT));

        if (playerBound) {
            if (!Arrays.equals(playerService.getRadioStations(), stationListAdapter.getStationList())) {
                stationListAdapter.updateStationList(playerService.getRadioStations());
                stationListAdapter.setCurrentStationIndex(playerService.getCurrentChannelIndex());
                stationListAdapter.refreshCurrentStation();
            }
            playerService.registerCallback(this);
            if (playerService.getRadioStations().length > 0) {
                playerService.handlePlayRequest();
            }
        } else {
            bindPlayerService();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(controlInputReceiver);
        if (playerBound) {
            playerService.unregisterCallback(this);
        }
    }

    public void onPlaybackServiceConnected() {
        radioStatusDialog.setPlayerService(playerService);

        radio.getListenerManager().registerDataListener(this);
        playerService.getMediaController().registerCallback(mediaControllerCallback);
        playerService.registerCallback(this);

        setupPlaybackControls();
        setupVolumeControls();
        setupSettingsButton();

        stationListAdapter.updateStationList(playerService.getRadioStations());
        stationListRecyclerView.scrollToPosition(playerService.getCurrentChannelIndex());
        stationListLayoutManager.setSnapDuration(1);
        updatePlayerMetadata();
        stationListLayoutManager.setSnapDuration(250);
        stationListRecyclerView.getItemAnimator().setChangeDuration(100);
        stationListRecyclerView.getItemAnimator().setRemoveDuration(0);
        stationListRecyclerView.getItemAnimator().setAddDuration(100);

        if (playerService.getRadioStations().length > 0) {
            playerService.handlePlayRequest();
        }
    }

    public void sendActionToService(String action) {
        startService(new Intent(this, RadioPlayerService.class).setAction(action));
    }

    public void setupStationList() {
        stationListLayoutManager = new StationListLayoutManager(this);

        stationListRecyclerView = (RecyclerView) findViewById(R.id.station_list);
        stationListRecyclerView.setLayoutManager(stationListLayoutManager);
        stationListRecyclerView.setAdapter(stationListAdapter);
    }

    public void setupPlaybackControls() {
        nextButton = (ImageButton) findViewById(R.id.skip_next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!playerBound) {
                    bindPlayerService();
                    sendActionToService(RadioPlayerService.ACTION_NEXT);
                } else {
                    playerService.handleNextChannelRequest();
                }
            }
        });

        previousButton = (ImageButton) findViewById(R.id.skip_previous_button);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!playerBound) {
                    bindPlayerService();
                    sendActionToService(RadioPlayerService.ACTION_NEXT);
                } else {
                    playerService.handlePreviousChannelRequest();
                }
            }
        });

        playPauseButton = (ImageButton) findViewById(R.id.play_pause_button);
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!playerBound) {
                    bindPlayerService();
                } else {
                    if (playerService.getPlaybackState() == PlaybackStateCompat.STATE_PLAYING) {
                        playerService.handlePauseRequest();
                    } else {
                        playerService.handlePlayRequest();
                    }
                }
            }
        });

        updatePlayIcon(playerService.getPlaybackState());
    }

    public void setupVolumeControls() {
        volumeButton = (ImageButton) findViewById(R.id.volume_button);
        onVolumeChanged(radio.getVolume());
    }

    public void setupSettingsButton() {
        settingsButton = (ImageButton) findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(
                        new Intent(getApplicationContext(), SettingsActivity.class)
                );
            }
        });
    }

    public void setupPlayerAttributes() {
        currentChannelView = (TextView) findViewById(R.id.channel_name);
        dataRateTextView = (TextView) findViewById(R.id.data_rate);
        ensembleTextView = (TextView) findViewById(R.id.station_ensemble_name);
        genreTextView = (TextView) findViewById(R.id.station_genre);
        playStatusView = (TextView) findViewById(R.id.play_status);
        signalStrengthView = (TextView) findViewById(R.id.signal_strength);
        signalStrengthIcon = (ImageView) findViewById(R.id.signal_strength_icon);
        programTextTextView = (TextView) findViewById(R.id.program_text);
        stereoStateTextView = (TextView) findViewById(R.id.program_stereo_mode);
    }

    public void clearPlayerAttributes() {
        signalStrengthView.setText("");
        programTextTextView.setText("");
        stereoStateTextView.setText("");
        onSignalQualityChanged(0);
        onPlayStatusChanged(RadioDevice.Values.PLAY_STATUS_STREAM_STOP);
        genreTextView.setText("");
        ensembleTextView.setText("");
        currentChannelView.setText("");
        updatePlayerMetadata();
    }

    public void updatePlayerMetadata() {
        if (playerBound) {
            RadioStation currentStation = playerService.getCurrentStation();
            if (currentStation != null) {
                updateCurrentChannelName(currentStation.getName());
                updateEnsembleName(currentStation.getEnsemble());
                updateGenreName(RadioDevice.StringValues.getGenreFromId(currentStation.getGenreId()));
                updateStationListSelection(playerService.getCurrentChannelIndex());
            }
        } else {
            updateCurrentChannelName("");
            updateEnsembleName("");
            updateGenreName("");
            updateStationListSelection(0);
        }

        programTextTextView.setText("");
    }

    public void updatePlayIcon(int playState) {
        int icon;
        if (playState == PlaybackStateCompat.STATE_PLAYING) {
            icon = R.drawable.ic_pause_white_24dp;
        } else {
            icon = R.drawable.ic_play_arrow_white_24dp;
        }

        playPauseButton.setForeground(ContextCompat.getDrawable(this, icon));
    }

    public void updateCurrentChannelName(String channelName) {
        currentChannelView.setText(channelName);
    }

    public void updateEnsembleName(String ensembleName) {
        ensembleTextView.setText(ensembleName);
    }

    public void updateGenreName(String genre) {
        genreTextView.setText(genre);
    }

    public void updateStationListSelection(final int channelIndex) {
        stationListAdapter.setCurrentStationIndex(channelIndex);

        if (selectChannelScrollRunnable != null) {
            stationListRecyclerView.removeCallbacks(selectChannelScrollRunnable);
        }

        stationListRecyclerView.clearOnScrollListeners();

        selectChannelScrollRunnable =  new Runnable() {
            @Override
            public void run() {
                stationListRecyclerView.clearOnScrollListeners();
                stationListRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    private boolean done = false;
                    @Override
                    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                        super.onScrollStateChanged(recyclerView, newState);
                        updateSelection();
                    }

                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        updateSelection();
                    }

                    private void updateSelection() {
                        if (!done) {
                            done = true;
                            stationListAdapter.refreshCurrentStation();
                            stationListRecyclerView.removeOnScrollListener(this);
                        }
                    }
                });
                stationListRecyclerView.smoothScrollToPosition(
                        stationListAdapter.getCurrentStationIndex()
                );
            }
        };

        stationListRecyclerView.post(selectChannelScrollRunnable);
    }

    public void updateCursorPosition(final int newCursorIndex) {
        if (cursorScrollRunnable != null) {
            stationListRecyclerView.removeCallbacks(cursorScrollRunnable);
        }

        stationListRecyclerView.clearOnScrollListeners();

        cursorScrollRunnable =  new Runnable() {
            @Override
            public void run() {
                stationListRecyclerView.clearOnScrollListeners();
                stationListRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    private boolean done = false;
                    @Override
                    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                        super.onScrollStateChanged(recyclerView, newState);
                        updateSelection();
                    }

                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        updateSelection();
                    }

                    private void updateSelection() {
                        if (!done) {
                            done = true;
                            stationListAdapter.setCursorIndex(newCursorIndex);
                            stationListRecyclerView.removeOnScrollListener(this);
                        }
                    }
                });
                stationListLayoutManager.setSnapDuration(1);
                stationListRecyclerView.smoothScrollToPosition(newCursorIndex);
                stationListLayoutManager.setSnapDuration(250);
            }
        };

        stationListRecyclerView.post(cursorScrollRunnable);
    }

    public void clearProgramText() {
        programTextTextView.setText("");
    }

    public void handleSetChannel(int channel) {
        if (playerBound) {
            playerService.handleSetChannel(channel);
            playerService.handlePlayRequest();
        }
    }

    public void handleNextCursorPosition() {
        int newPosition = stationListAdapter.getCursorIndex() + 1;
        if (newPosition >= stationListAdapter.getItemCount()) {
            if (cursorScrollWrap) {
                newPosition = 0;
            } else {
                newPosition = -1;
            }
        }

        if (newPosition != -1) {
            updateCursorPosition(newPosition);
        }
    }

    public void handlePreviousCursorPosition() {
        int newPosition = stationListAdapter.getCursorIndex() - 1;
        if (newPosition < 0) {
            if (cursorScrollWrap) {
                newPosition = stationListAdapter.getItemCount() - 1;
            } else {
                newPosition = -1;
            }
        }
        if (newPosition != -1) {
            updateCursorPosition(newPosition);
        }
    }

    @Override
    public void onProgramTextChanged(String programText) {
        programTextTextView.setText(programText);
    }

    @Override
    public void onPlayStatusChanged(int playStatus) {
        Log.v(TAG, "Updating Play Status: " + RadioDevice.StringValues.getPlayStatusFromId(playStatus));
        playStatusView.setText(RadioDevice.StringValues.getPlayStatusFromId(playStatus));
    }

    @Override
    public void onSignalQualityChanged(int signalStrength) {
        signalStrengthView.setText(String.valueOf(signalStrength) + "%");
        int drawableId = 0;

        if (signalStrength > 70) {
            drawableId = R.drawable.ic_signal_cellular_4_bar_white_24dp;
        } else if (signalStrength > 60) {
            drawableId = R.drawable.ic_signal_cellular_3_bar_white_24dp;
        } else if (signalStrength > 50) {
            drawableId = R.drawable.ic_signal_cellular_2_bar_white_24dp;
        } else if (signalStrength > 40) {
            drawableId = R.drawable.ic_signal_cellular_1_bar_white_24dp;
        } else {
            drawableId = R.drawable.ic_signal_cellular_0_bar_white_24dp;
        }

        signalStrengthIcon.setImageDrawable(getDrawable(drawableId));
    }

    @Override
    public void onProgramDataRateChanged(int dataRate) {
        dataRateTextView.setText(getString(R.string.program_datarate_placeholder, dataRate));
    }

    @Override
    public void onStereoStateChanged(int stereoState) {
        stereoStateTextView.setText(RadioDevice.StringValues.getStereoModeFromId(stereoState));
    }

    @Override
    public void onVolumeChanged(int volume) {
        int icon = 0;
        if (volume < playerService.getPlayerVolume()) { // Ducking
            if (volume == 0 &&
                    playerService.getPlaybackState() == PlaybackStateCompat.STATE_PLAYING) {
                // Full duck
                icon = R.drawable.ic_volume_mute_white_24dp;
            } else if (volume != 0) {
                // Duck
                icon = R.drawable.ic_volume_down_white_24dp;
            }
        } else {
            // At full volume
            icon = R.drawable.ic_volume_up_white_24dp;
        }

        if (icon != 0) {
            volumeButton.setForeground(ContextCompat.getDrawable(this, icon));
        }
    }

    public boolean isRadioStatusDialogOpen() {
        return getFragmentManager().findFragmentByTag("RadioStatusDialog") != null;
    }

    public void openRadioStatusDialog(RadioStatusDialog.State state) {
        if (!isRadioStatusDialogOpen() && playerBound) {
            radioStatusDialog = new RadioStatusDialog();
            radioStatusDialog.setPlayerService(playerService);
            radioStatusDialog.setInitialState(state);
            radioStatusDialog.show(getFragmentManager(), "RadioStatusDialog");
        }
    }

    @Override
    public void onNoStoredStations() {
        openRadioStatusDialog(RadioStatusDialog.State.Connecting);
    }

    public void onAttachTimeout() {
        if (!isRadioStatusDialogOpen()) {
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.device_connection_timed_out_try_again))
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (playerBound) {
                                playerService.openConnection();
                            }
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                            stopService(new Intent(getApplicationContext(), RadioPlayerService.class));
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onSearchStart() {
        openRadioStatusDialog(RadioStatusDialog.State.Searching);
    }

    @Override
    public void onSearchProgressUpdate(int numChannels, int progress) {
        openRadioStatusDialog(RadioStatusDialog.State.Searching);
    }

    @Override
    public void onSearchComplete(int numChannels) {

    }

    @Override
    public void onStationListCopyStart() {
        openRadioStatusDialog(RadioStatusDialog.State.Copying);
    }


    public void onStationListCopyProgressUpdate(int progress, int max) {
        openRadioStatusDialog(RadioStatusDialog.State.Copying);
    }

    public void onStationListCopyComplete() {
        if (playerBound) {
            stationListAdapter.updateStationList(playerService.getRadioStations());
            playerService.handlePlayRequest();
        }
    }

    public void onDismissed() {
        Log.v(TAG, "Received intent radio notification dismissed");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        if (playerBound) {
            playerService.getMediaController().unregisterCallback(mediaControllerCallback);
            radio.getListenerManager().unregisterDataListener(this);
            playerService.unregisterCallback(this);
            unbindService(serviceConnection);
            if (!playerService.isPlaying()) {
                stopService(new Intent(this, RadioPlayerService.class));
            }
        }
    }

    private MediaControllerCompat.Callback mediaControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            updatePlayerMetadata();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            updatePlayIcon(state.getState());
        }
    };

    public void handleKeyDown(int keyCode)  {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
                if (stationListAdapter != null) {
                    if (playerBound) {
                        int lastChannel = playerService.getCurrentChannelIndex();
                        playerService.handleSetChannel(stationListAdapter.getCursorIndex());

                        // Pause the channel if we have not switched channels
                        if (playerService.isPlaying() &&
                                lastChannel == stationListAdapter.getCursorIndex()) {
                            playerService.handlePauseRequest();
                        } else {
                            playerService.handlePlayRequest();
                        }
                    }
                }
                break;
            case KeyEvent.KEYCODE_TAB:
                handleNextCursorPosition();
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                handlePreviousCursorPosition();
                break;

            case KeyEvent.KEYCODE_BACK:
                finish();
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (!controllerInput || keyCode == KeyEvent.KEYCODE_BACK) {
            handleKeyDown(keyCode);
        }
        return true;
    }

}
