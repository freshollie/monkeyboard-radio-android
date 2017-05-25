package com.freshollie.monkeyboard.keystoneradio.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.freshollie.monkeyboard.keystoneradio.R;
import com.freshollie.monkeyboard.keystoneradio.playback.RadioPlayerService;
import com.freshollie.monkeyboard.keystoneradio.radio.RadioDeviceListenerManager;
import com.freshollie.monkeyboard.keystoneradio.radio.RadioDevice;
import com.freshollie.monkeyboard.keystoneradio.radio.RadioStation;

import java.util.Arrays;

public class PlayerActivity extends AppCompatActivity implements RadioDeviceListenerManager.DataListener,
        RadioPlayerService.PlayerCallback {
    private String TAG = this.getClass().getSimpleName();

    public static final String HEADUNITCONTROLLER_ACTION_SEND_KEYEVENT =
            "com.freshollie.headunitcontroller.action.SEND_KEYEVENT";

    private RadioPlayerService playerService;
    private Boolean playerBound = false;
    private RadioDevice radio;

    private ImageButton nextButton;
    private ImageButton previousButton;
    private ImageButton pauseButton;
    private ImageButton playButton;
    private ImageButton volumeButton;
    private ImageButton settingsButton;

    private Switch modeSwitch;

    private SeekBar volumeSeekBar;

    private SeekBar fmSeekBar;

    private boolean userChangingFmFrequency = false;

    private TextView currentChannelView;
    private TextView programTextTextView;
    private TextView signalStrengthView;
    private TextView playStatusTextView;
    private TextView genreTextView;
    private TextView ensembleTextView;
    private TextView dataRateTextView;
    private TextView stereoStateTextView;
    private ImageView signalStrengthIcon;
    private TextView volumeText;

    private RecyclerView stationListRecyclerView;
    private StationListAdapter stationListAdapter = new StationListAdapter(this);
    private StationListLayoutManager stationListLayoutManager;

    private RadioStatusDialog radioStatusDialog = new RadioStatusDialog();

    private SharedPreferences sharedPreferences;

    private RecyclerView.OnScrollListener stationListScrollListener;
    private Runnable cursorScrollRunnable;
    private Runnable selectChannelScrollRunnable;

    private boolean preferenceControllerInput = false;
    private boolean preferenceCursorScrollWrap = true;
    private boolean preferencePlayOnOpen = false;

    private boolean isRestartedInstance = false;

    private BroadcastReceiver controlInputReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(HEADUNITCONTROLLER_ACTION_SEND_KEYEVENT)) {
                if (intent.hasExtra("keyCode") && preferenceControllerInput) {
                    handleKeyDown(intent.getIntExtra("keyCode", -1));
                }
            }
        }
    };

    /**
     * Updates our internal player preferences when changed
     */
    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            if (s.equals(getString(R.string.SCROLL_WRAP_KEY))) {
                preferenceCursorScrollWrap =
                        sharedPreferences.getBoolean(
                                getString(R.string.SCROLL_WRAP_KEY),
                                false
                        );
                Log.v(TAG, "Cursor Scroll wrap set to: " + String.valueOf(preferenceCursorScrollWrap));
            } else if (s.equals(getString(R.string.HEADUNIT_MAIN_INPUT_KEY))) {
                preferenceControllerInput =
                        sharedPreferences.getBoolean(
                                getString(R.string.HEADUNIT_MAIN_INPUT_KEY),
                                false
                        );
                Log.v(TAG, "Headunit input set to: " + String.valueOf(preferenceControllerInput));
            } else if (s.equals(getString(R.string.PLAY_ON_OPEN_KEY))) {
                preferencePlayOnOpen =
                        sharedPreferences.getBoolean(
                                getString(R.string.PLAY_ON_OPEN_KEY),
                                false
                        );
                Log.v(TAG, "Play on open set to: " + String.valueOf(preferencePlayOnOpen));
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
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        setContentView(R.layout.activity_player);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        preferenceControllerInput = sharedPreferences.getBoolean(
                getString(R.string.HEADUNIT_MAIN_INPUT_KEY),
                false
        );

        preferenceCursorScrollWrap = sharedPreferences.getBoolean(
                getString(R.string.SCROLL_WRAP_KEY),
                false
        );

        preferencePlayOnOpen = sharedPreferences.getBoolean(
                getString(R.string.PLAY_ON_OPEN_KEY),
                false
        );


        bindPlayerService();
        setupPlayerAttributes(savedInstanceState);
        setupStationList();

        if (savedInstanceState == null) {
            clearPlayerAttributes();

        } else {
            isRestartedInstance = true;
        }
    }

    public void sendActionToService(String action) {
        startService(new Intent(this, RadioPlayerService.class).setAction(action));
    }

    /**
     * Starts the bind to the player service
     */
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
        registerReceiver(controlInputReceiver,
                new IntentFilter(HEADUNITCONTROLLER_ACTION_SEND_KEYEVENT));

        userChangingFmFrequency = false;

        // Update the player attributes from the service
        if (playerBound) {
            // Update the volume
            updateVolume(playerService.getPlayerVolume());

            // Update the player with the new attributes from the station list
            if (!Arrays.equals(playerService.getDabRadioStations(),
                    stationListAdapter.getStationList()) &&
                    playerService.getRadioMode() == RadioDevice.Values.STREAM_MODE_DAB) {
                stationListAdapter.updateStationList(playerService.getDabRadioStations());
                stationListAdapter.setCurrentStationIndex(playerService.getCurrentDabChannelIndex());
                stationListAdapter.notifyCurrentStationChanged();
            }

            // Re-Register the callback
            playerService.registerCallback(this);

            if (preferencePlayOnOpen) {
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

        if (!radio.isConnected()) {
            playStatusTextView.setText(getString(R.string.radio_status_connecting));
        }

        radio.getListenerManager().registerDataListener(this);
        playerService.getMediaController().registerCallback(mediaControllerCallback);
        playerService.registerCallback(this);

        setupPlaybackControls();
        setupVolumeControls();
        setupSettingsButton();

        updateVolume(playerService.getPlayerVolume());

        onRadioModeChanged(playerService.getRadioMode());

        // Scrolls to the currently playing track instantly
        stationListLayoutManager.setSnapDuration(1);

        // then sets the animations back to normal
        stationListLayoutManager.setSnapDuration(250);
        stationListRecyclerView.getItemAnimator().setChangeDuration(100);
        stationListRecyclerView.getItemAnimator().setRemoveDuration(0);
        stationListRecyclerView.getItemAnimator().setAddDuration(100);

        updatePlayerAttributesFromMetadata(!isRestartedInstance);

        if (preferencePlayOnOpen) {
            playerService.handlePlayRequest();
        }
    }

    public void setupStationList() {
        stationListLayoutManager = new StationListLayoutManager(this);

        stationListRecyclerView = (RecyclerView) findViewById(R.id.station_list);
        stationListRecyclerView.setLayoutManager(stationListLayoutManager);
        stationListRecyclerView.setAdapter(stationListAdapter);

        ViewCompat.setElevation(findViewById(R.id.station_list_container), 100);
        ViewCompat.setElevation(findViewById(R.id.player_control_panel), 50);
    }

    public void setupPlaybackControls() {
        modeSwitch = (Switch) findViewById(R.id.mode_switch);

        modeSwitch.setChecked(!sharedPreferences.getBoolean(
                getString(R.string.RADIO_MODE_KEY), true)
        );

        modeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                sharedPreferences.edit()
                        .putBoolean(getString(R.string.RADIO_MODE_KEY), !b)
                        .apply();
                if (playerBound) {
                    playerService.handleSetRadioMode(
                            !b ?
                                    RadioDevice.Values.STREAM_MODE_DAB:
                                    RadioDevice.Values.STREAM_MODE_FM
                    );
                    onRadioModeChanged(playerService.getRadioMode());
                }
            }
        });

        fmSeekBar = (SeekBar) findViewById(R.id.fm_seek_bar);
        fmSeekBar.setMax(RadioDevice.Values.MAX_FM_FREQUENCY - RadioDevice.Values.MIN_FM_FREQUENCY);
        fmSeekBar.setProgress(playerService.getCurrentFmFrequency() - RadioDevice.Values.MIN_FM_FREQUENCY);

        fmSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
                Log.v(TAG, ""+ i + RadioDevice.Values.MIN_FM_FREQUENCY);
                if (playerBound && fromUser) {
                    playerService.handleSetFmFrequencyRequest(i + RadioDevice.Values.MIN_FM_FREQUENCY);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                userChangingFmFrequency = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                userChangingFmFrequency = false;
            }
        });
        fmSeekBar.setVisibility(modeSwitch.isChecked() ? View.VISIBLE: View.INVISIBLE);

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


        playButton = (ImageButton) findViewById(R.id.play_pause_button);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

    private Runnable seekBarIdle = new Runnable() {
        @Override
        public void run() {
            onCloseVolumeSeekBar();
        }
    };

    public void setupVolumeControls() {
        volumeButton = (ImageButton) findViewById(R.id.volume_button);
        volumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (volumeSeekBar.getVisibility() == View.VISIBLE) {
                    onCloseVolumeSeekBar();

                } else {
                    if (!playerService.isPlaying()) {
                        updateVolume(playerService.getPlayerVolume());
                    }
                    onOpenVolumeSeekBar();
                }
            }
        });

        volumeSeekBar.setMax(RadioPlayerService.MAX_PLAYER_VOLUME);

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (playerBound) {
                    volumeText.setText(String.valueOf(progress));
                    playerService.setPlayerVolume(progress);
                    updateVolumeIcon(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekBar.removeCallbacks(seekBarIdle);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar.postDelayed(seekBarIdle, 2000);
            }
        });

        if (playerBound) {
            updateVolume(playerService.getPlayerVolume());
        }
    }

    public void onRadioModeChanged(int mode) {
        if (mode == RadioDevice.Values.STREAM_MODE_DAB) {
            fmSeekBar.setVisibility(View.INVISIBLE);
            stationListAdapter.updateStationList(playerService.getDabRadioStations());
            stationListRecyclerView.scrollToPosition(playerService.getCurrentDabChannelIndex());
        } else {
            fmSeekBar.setVisibility(View.VISIBLE);
            if (selectChannelScrollRunnable != null) {
                stationListRecyclerView.removeCallbacks(selectChannelScrollRunnable);
            }
            stationListAdapter.updateStationList(playerService.getFmRadioStations());
            stationListAdapter.setCurrentStationIndex(-1);
            stationListAdapter.notifyCurrentStationChanged();
            fmSeekBar.setProgress(playerService.getCurrentFmFrequency() - RadioDevice.Values.MIN_FM_FREQUENCY);
        }
        clearPlayerAttributes();
    }

    public void onOpenVolumeSeekBar() {
        volumeSeekBar.setVisibility(View.VISIBLE);
        volumeText.setVisibility(View.VISIBLE);
        volumeSeekBar.postDelayed(seekBarIdle, 2000);
    }

    public void onCloseVolumeSeekBar() {
        volumeSeekBar.setVisibility(View.INVISIBLE);
        volumeText.setVisibility(View.INVISIBLE);
        volumeSeekBar.removeCallbacks(seekBarIdle);

    }

    public void updateVolume(int volume){
        volumeSeekBar.setProgress(volume);
        volumeText.setText(String.valueOf(volume));
        updateVolumeIcon(volume);
    }

    public void updateVolumeIcon(int volume) {
        int icon;

        // At full volume
        if (volume > 8) {
            icon = R.drawable.ic_volume_up_white_24dp;
        } else if (volume > 0) {
            icon = R.drawable.ic_volume_down_white_24dp;
        } else {
            icon = R.drawable.ic_volume_mute_white_24dp;
        }

        volumeButton.setImageResource(icon);
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

    public void setupPlayerAttributes(Bundle savedInstanceState) {
        currentChannelView = (TextView) findViewById(R.id.channel_name);
        dataRateTextView = (TextView) findViewById(R.id.data_rate);
        ensembleTextView = (TextView) findViewById(R.id.station_ensemble_name);
        genreTextView = (TextView) findViewById(R.id.station_genre);
        playStatusTextView = (TextView) findViewById(R.id.play_status);
        signalStrengthView = (TextView) findViewById(R.id.signal_strength);
        signalStrengthIcon = (ImageView) findViewById(R.id.signal_strength_icon);
        programTextTextView = (TextView) findViewById(R.id.program_text);
        stereoStateTextView = (TextView) findViewById(R.id.program_stereo_mode);
        volumeSeekBar = (SeekBar) findViewById(R.id.volume_seek_bar);
        volumeText = (TextView) findViewById(R.id.volume_text);

        if (savedInstanceState != null) {
            Log.v(TAG, "Loading previous states");
            currentChannelView.setText(
                    savedInstanceState.getString(String.valueOf(R.id.channel_name))
            );
            dataRateTextView.setText(
                    savedInstanceState.getString(String.valueOf(R.id.data_rate))
            );
            ensembleTextView.setText(
                    savedInstanceState.getString(String.valueOf(R.id.station_ensemble_name))
            );
            genreTextView.setText(
                    savedInstanceState.getString(String.valueOf(R.id.station_genre))
            );
            playStatusTextView.setText(
                    savedInstanceState.getString(String.valueOf(R.id.play_status))
            );

            programTextTextView.setText(
                    savedInstanceState.getString(String.valueOf(R.id.program_text))
            );
            stereoStateTextView.setText(
                    savedInstanceState.getString(String.valueOf(R.id.program_stereo_mode))
            );
            volumeSeekBar.setProgress(
                    savedInstanceState.getInt(String.valueOf(R.id.volume_seek_bar))
            );
            volumeText.setText(
                    savedInstanceState.getString(String.valueOf(R.id.volume_text))
            );

            onDabSignalQualityChanged(savedInstanceState.getInt(String.valueOf(R.id.signal_strength)));
        }
    }

    public void clearPlayerAttributes() {
        signalStrengthView.setText("");
        programTextTextView.setText("");
        stereoStateTextView.setText("");
        onDabProgramDataRateChanged(0);
        onDabSignalQualityChanged(0);
        onPlayStatusChanged(RadioDevice.Values.PLAY_STATUS_STREAM_STOP);
        genreTextView.setText("");
        ensembleTextView.setText("");
        currentChannelView.setText("");
        updatePlayerAttributesFromMetadata();
    }

    public void updatePlayerAttributesFromMetadata(boolean clearProgramText) {
        if (playerBound) {
            RadioStation currentStation = playerService.getCurrentStation();
            if (currentStation != null) {
                updateCurrentChannelName(currentStation.getName());
                updateEnsembleName(currentStation.getEnsemble());
                updateGenreName(RadioDevice.StringValues.getGenreFromId(currentStation.getGenreId()));

                if (playerService.getRadioMode() == RadioDevice.Values.STREAM_MODE_DAB) {
                    updateStationListSelection(playerService.getCurrentDabChannelIndex());
                }

                if (playerService.getRadioMode() == RadioDevice.Values.STREAM_MODE_FM) {
                    if (!userChangingFmFrequency) {
                        fmSeekBar.setProgress(playerService.getCurrentFmFrequency() - RadioDevice.Values.MIN_FM_FREQUENCY);
                    }
                }
            }
        } else {
            updateCurrentChannelName("");
            updateEnsembleName("");
            updateGenreName("");
            if (fmSeekBar != null) {
                fmSeekBar.setProgress(0);
            }
        }

        if (clearProgramText) {
            programTextTextView.setText("");
        }
    }

    public void updatePlayerAttributesFromMetadata() {
        updatePlayerAttributesFromMetadata(true);
    }

    public void updatePlayIcon(int playState) {
        int icon;
        if (playState == PlaybackStateCompat.STATE_PLAYING) {
            icon = R.drawable.ic_pause_white_24dp;
        } else {
            icon = R.drawable.ic_play_arrow_white_24dp;
        }
        playButton.setImageResource(icon);
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
                            stationListRecyclerView.post(new Runnable() {
                                @Override
                                public void run() {
                                    stationListAdapter.notifyCurrentStationChanged();
                                }
                            });
                            stationListRecyclerView.removeOnScrollListener(this);
                        }
                    }
                });

                if (playerBound &&
                        playerService.getRadioMode() == RadioDevice.Values.STREAM_MODE_DAB &&
                        stationListAdapter.getCurrentStationIndex() < stationListAdapter.getItemCount()
                        ) {
                    stationListRecyclerView.smoothScrollToPosition(
                            stationListAdapter.getCurrentStationIndex()
                    );
                }
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
                            stationListRecyclerView.post(new Runnable() {
                                @Override
                                public void run() {
                                    stationListAdapter.setCursorIndex(newCursorIndex);
                                }
                            });
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

    public void handleSetChannel(int channel) {
        if (playerBound) {
            playerService.handleSetDabChannelRequest(channel);
            playerService.handlePlayRequest();
        }
    }

    public void handleNextCursorPosition() {
        int newPosition = stationListAdapter.getCursorIndex() + 1;
        if (newPosition >= stationListAdapter.getItemCount()) {
            if (preferenceCursorScrollWrap) {
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
            if (preferenceCursorScrollWrap) {
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
        playStatusTextView.setText(RadioDevice.StringValues.getPlayStatusFromId(playStatus));
    }

    @Override
    public void onDabSignalQualityChanged(int signalStrength) {
        signalStrengthView.setText(String.valueOf(signalStrength) + "%");

        int iconResId;
        if (signalStrength > 70) {
            iconResId = R.drawable.ic_signal_cellular_4_bar_white_24dp;
        } else if (signalStrength > 60) {
            iconResId = R.drawable.ic_signal_cellular_3_bar_white_24dp;
        } else if (signalStrength > 50) {
            iconResId = R.drawable.ic_signal_cellular_2_bar_white_24dp;
        } else if (signalStrength > 40) {
            iconResId = R.drawable.ic_signal_cellular_1_bar_white_24dp;
        } else {
            iconResId = R.drawable.ic_signal_cellular_0_bar_white_24dp;
        }

        signalStrengthIcon.setImageResource(iconResId);
    }

    @Override
    public void onDabProgramDataRateChanged(int dataRate) {
        dataRateTextView.setText(getString(R.string.program_datarate_placeholder, dataRate));
    }

    @Override
    public void onStereoStateChanged(int stereoState) {
        stereoStateTextView.setText(RadioDevice.StringValues.getStereoModeFromId(stereoState));
    }

    @Override
    public void onFmSignalStrengthChanged(int signalStrength) {
        onDabSignalQualityChanged(signalStrength);
    }

    @Override
    public void onFmSearchFrequencyChanged(int frequency) {

    }

    @Override
    public void onFmProgramNameUpdated(String newFmProgramName) {

    }

    @Override
    public void onFmProgramTypeUpdated(int newFmProgramType) {

    }

    @Override
    public void onRadioVolumeChanged(int volume) {
        int icon = 0;
        if (playerBound) {
            if (playerService.isDucked() && playerService.isPlaying()) { // Ducking
                if (volume == 0) {
                    // Full duck
                    icon = R.drawable.ic_volume_mute_white_24dp;
                } else {
                    // Duck
                    icon = R.drawable.ic_volume_down_white_24dp;
                }
            }
        }

        if (icon != 0) {
            // Sets the icon to the new icon
            volumeButton.setImageResource(icon);
        }
    }

    @Override
    public void onPlayerVolumeChanged(int newVolume) {
        updateVolume(newVolume);
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
            stationListAdapter.updateStationList(playerService.getDabRadioStations());
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.v(TAG, "Saving state");
        outState.putString(String.valueOf(R.id.channel_name),
                currentChannelView.getText().toString());

        outState.putString(String.valueOf(R.id.data_rate),
                dataRateTextView.getText().toString());

        outState.putString(String.valueOf(R.id.station_ensemble_name),
                ensembleTextView.getText().toString());

        outState.putString(String.valueOf(R.id.station_genre),
                genreTextView.getText().toString());

        outState.putString(String.valueOf(R.id.play_status),
                playStatusTextView.getText().toString());

        outState.putString(String.valueOf(R.id.program_text),
                programTextTextView.getText().toString());

        outState.putString(String.valueOf(R.id.program_stereo_mode),
                stereoStateTextView.getText().toString());

        outState.putInt(String.valueOf(R.id.volume_seek_bar),
                volumeSeekBar.getProgress());

        outState.putString(String.valueOf(R.id.volume_text),
                volumeText.getText().toString());

        String signalStrengthText = signalStrengthView.getText().toString();
        outState.putInt(String.valueOf(R.id.signal_strength),
                Integer.valueOf(signalStrengthText.substring(0, signalStrengthText.length() - 1)));

        super.onSaveInstanceState(outState);
    }

    private MediaControllerCompat.Callback mediaControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            updatePlayerAttributesFromMetadata();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            updatePlayIcon(state.getState());
        }
    };

    public boolean handleKeyDown(int keyCode)  {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
                if (stationListAdapter != null) {
                    if (playerBound) {
                        int lastChannel = playerService.getCurrentDabChannelIndex();
                        playerService.handleSetDabChannelRequest(stationListAdapter.getCursorIndex());

                        // Pause the channel if we have not switched channels
                        if (playerService.isPlaying() &&
                                lastChannel == stationListAdapter.getCursorIndex()) {
                            playerService.handlePauseRequest();
                        } else {
                            playerService.handlePlayRequest();
                        }
                    }
                }
                return true;

            case KeyEvent.KEYCODE_TAB:
                handleNextCursorPosition();
                return true;

            case KeyEvent.KEYCODE_DPAD_UP:
                handlePreviousCursorPosition();
                return true;

            case KeyEvent.KEYCODE_BACK:
                finish();
                return true;
        }

        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (playerBound) {
                if (!playerService.isPlaying()) {
                    updateVolume(playerService.getPlayerVolume());
                }
            }
        } else if (!preferenceControllerInput) {
            if (handleKeyDown(keyCode)) {
                return true;
            }
        } else {
            // Custom input has already been handled
            switch(keyCode) {
                case KeyEvent.ACTION_UP:
                    return true;
                case KeyEvent.KEYCODE_TAB:
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    return true;
                case KeyEvent.KEYCODE_ENTER:
                    return true;
             }
        }
        return super.onKeyDown(keyCode, keyEvent);
    }

}
