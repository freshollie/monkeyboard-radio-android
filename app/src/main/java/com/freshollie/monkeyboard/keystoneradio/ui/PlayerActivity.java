/*
 * Created by Oliver Bell on 15/01/17
 * Copyright (c) 2017. by Oliver bell <freshollie@gmail.com>
 *
 * Last modified 15/06/17 23:07
 */

package com.freshollie.monkeyboard.keystoneradio.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.transition.ChangeBounds;
import android.support.transition.Fade;
import android.support.transition.TransitionManager;
import android.support.transition.TransitionSet;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.transition.ChangeTransform;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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

import java.text.DecimalFormat;
import java.util.Arrays;

/**
 * Player activity is the main activity of the app. It binds the the Playback service and displays
 * details about the radio. It also allows the user to control of the player service.
 */
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
    private ImageButton searchForwardsButton;
    private ImageButton searchBackwardsButton;
    private ImageButton pauseButton;
    private ImageButton playButton;
    private ImageButton volumeButton;
    private ImageButton settingsButton;

    private Switch modeSwitch;

    private SeekBar fmSeekBar;

    private FloatingActionButton addChannelFab;
    private Animation fabForwardsAnimation;
    private Animation fabBackwardsAnimation;

    private boolean userChangingFmFrequency = false;

    private TextView fmFrequencyTextView;

    private TextView channelNameTextView;
    private TextView programTextTextView;
    private ImageView slideshowImageView;

    private TextView signalStrengthView;
    private TextView playStatusTextView;
    private TextView genreTextView;
    private TextView ensembleTextView;
    private TextView dataRateTextView;
    private TextView stereoStateTextView;
    private ImageView signalStrengthIcon;

    private TextView volumeText;
    private SeekBar volumeSeekBar;
    private View volumeSeekbarHolder;

    private TextView noStationsTextView;
    private RecyclerView stationListRecyclerView;
    private StationListAdapter stationListAdapter = new StationListAdapter(this);
    private StationListLayoutManager stationListLayoutManager;

    private Bitmap currentSlideshowImageBitmap;

    private RadioStatusDialog radioStatusDialog = new RadioStatusDialog();

    private SharedPreferences sharedPreferences;

    private RecyclerView.OnScrollListener stationListScrollListener;
    private Runnable cursorScrollRunnable;
    private Runnable selectChannelScrollRunnable;

    private boolean isRestartedInstance = false;

    private BroadcastReceiver controllerInputReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(HEADUNITCONTROLLER_ACTION_SEND_KEYEVENT)) {
            if (intent.hasExtra("keyCode") &&
                    sharedPreferences.getBoolean(
                            getString(R.string.PREF_HEADUNIT_CONTROLLER_INPUT),
                            false
                    )) {
                handleKeyDown(intent.getIntExtra("keyCode", -1));
            }
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
            RadioPlayerService.RadioPlayerBinder binder =
                    (RadioPlayerService.RadioPlayerBinder) service;
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
    private ConstraintLayout channelStatusBarLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Only 1 player activity should be open at a time
        // For some reason some launchers launch multiple
        if (!isTaskRoot()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_player);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        bindPlayerService();

        initialisePlayerAttributesUi(savedInstanceState);
        initialiseStationListUi();

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
        registerReceiver(controllerInputReceiver,
                new IntentFilter(HEADUNITCONTROLLER_ACTION_SEND_KEYEVENT));

        userChangingFmFrequency = false;

        // Update the player attributes from the service
        if (playerBound) {
            // Update the volume
            updateVolume(playerService.getPlayerVolume());

            // Update the station list if it has been changed
            if (!Arrays.equals(playerService.getDabRadioStations(),
                    stationListAdapter.getStationList()) &&
                    playerService.getRadioMode() == RadioDevice.Values.STREAM_MODE_DAB) {
                // if we don't have any stations and the user has the ability to
                // use FM mode, switch to FM mode
                if (playerService.getDabRadioStations().length < 1 &&
                        sharedPreferences.getBoolean(
                            getString(R.string.pref_fm_mode_enabled_key),
                            true)
                            ) {
                    playerService.handleSetRadioMode(RadioDevice.Values.STREAM_MODE_FM);
                } else {
                    updateStationList(playerService.getRadioMode());
                }
            } else if (!Arrays.equals(playerService.getFmRadioStations(),
                    stationListAdapter.getStationList()) &&
                    playerService.getRadioMode() == RadioDevice.Values.STREAM_MODE_FM) {
                updateStationList(playerService.getRadioMode());
            }

            // Re-Register the callback
            playerService.registerCallback(this);

            refreshSwitchControls();

            if (sharedPreferences.getBoolean(
                    getString(R.string.PREF_PLAY_ON_OPEN),
                    false
            )) {
                playerService.handlePlayRequest();
            }
        } else {
            bindPlayerService();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(controllerInputReceiver);
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

        initialisePlayerControls();
        initialiseVolumeControls();
        initialiseSettingsButton();

        updateVolume(playerService.getPlayerVolume());


        onRadioModeChanged(playerService.getRadioMode(), false);

        // Stop the animation from happening when the activity is first created
        if (playerService.getRadioMode() == RadioDevice.Values.STREAM_MODE_DAB) {
            fmSeekBar.clearAnimation();
            fmSeekBar.setVisibility(View.GONE);
            searchBackwardsButton.clearAnimation();
            searchBackwardsButton.setVisibility(View.INVISIBLE);
            searchForwardsButton.clearAnimation();
            searchForwardsButton.setVisibility(View.INVISIBLE);
        }

        // then sets the animations back to normal
        stationListLayoutManager.setSnapDuration(StationListLayoutManager.DEFAULT_SNAP_SPEED);

        updatePlayerAttributesFromMetadata(!isRestartedInstance);

        if (sharedPreferences.getBoolean(
                getString(R.string.PREF_PLAY_ON_OPEN),
                false
            )) {
            playerService.handlePlayRequest();
        }

        // Make sure these attributes are up to date also
        if (radio.isConnected()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final int playStatus = radio.getPlayStatus();
                    final int stereoMode = radio.getStereo();

                    final int programDataRate = radio.getProgramDataRate();
                    final int dabSignalQuality = radio.getSignalQuality();
                    final int fmSignalStrength = radio.getSignalStrength();

                    new Handler(getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            onPlayStatusChanged(playStatus);
                            onStereoStateChanged(stereoMode);
                            if (playerService.getRadioMode() == RadioDevice.Values.STREAM_MODE_DAB) {
                                onDabProgramDataRateChanged(programDataRate);
                                onDabSignalQualityChanged(dabSignalQuality);
                            } else {
                                onFmSignalStrengthChanged(fmSignalStrength);
                            }
                        }
                    });
                }
            }).start();
        }
    }

    public void refreshSwitchControls() {
        boolean fmModeEnabled =
                sharedPreferences.getBoolean(getString(R.string.pref_fm_mode_enabled_key), true);
        boolean dabModeEnabled =
                sharedPreferences.getBoolean(getString(R.string.pref_dab_mode_enabled_key), true);

        if (!fmModeEnabled) {
            if (playerService.getRadioMode() == RadioDevice.Values.STREAM_MODE_FM) {
                playerService.handleSetRadioMode(RadioDevice.Values.STREAM_MODE_DAB);
            }
            modeSwitch.setVisibility(View.GONE);
        } else if (!dabModeEnabled) {
            if (playerService.getRadioMode() == RadioDevice.Values.STREAM_MODE_DAB) {
                playerService.handleSetRadioMode(RadioDevice.Values.STREAM_MODE_FM);
            }
            modeSwitch.setVisibility(View.GONE);
        } else {
            modeSwitch.setChecked(playerService.getRadioMode() ==
                    RadioDevice.Values.STREAM_MODE_FM);
            modeSwitch.setVisibility(View.VISIBLE);
        }
    }

    public void initialiseStationListUi() {
        stationListLayoutManager = new StationListLayoutManager(this);

        stationListRecyclerView = (RecyclerView) findViewById(R.id.station_list);
        stationListRecyclerView.setHasFixedSize(true);
        stationListRecyclerView.setLayoutManager(stationListLayoutManager);
        stationListRecyclerView.setAdapter(stationListAdapter);

        //ViewCompat.setElevation(findViewById(R.id.station_list_container), 100);
        //ViewCompat.setElevation(findViewById(R.id.player_control_panel), 50);
    }

    public void initialisePlayerControls() {
        addChannelFab = (FloatingActionButton) findViewById(R.id.add_channel_fab);
        fabForwardsAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_forwards);
        fabBackwardsAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_backwards);

        modeSwitch = (Switch) findViewById(R.id.mode_switch);
        modeSwitch.setChecked(playerService.getRadioMode() == RadioDevice.Values.STREAM_MODE_FM);
        modeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (playerBound) {
                    playerService.handleSetRadioMode(
                            !b ?
                                    RadioDevice.Values.STREAM_MODE_DAB:
                                    RadioDevice.Values.STREAM_MODE_FM
                    );
                }
            }
        });
        refreshSwitchControls();

        fmSeekBar = (SeekBar) findViewById(R.id.fm_seek_bar);
        fmSeekBar.setMax(
                (RadioDevice.Values.MAX_FM_FREQUENCY - RadioDevice.Values.MIN_FM_FREQUENCY) / 100
        );

        fmSeekBar.setProgress(
                (playerService.getCurrentFmFrequency() - RadioDevice.Values.MIN_FM_FREQUENCY) / 100
        );

        fmSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
                if (playerBound && fromUser) {
                    playerService.handleSetFmFrequencyRequest(
                            i * 100 + RadioDevice.Values.MIN_FM_FREQUENCY
                    );
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                userChangingFmFrequency = true;
                if (stationListAdapter.isDeleteMode()) {
                    stationListAdapter.closeDeleteMode();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                userChangingFmFrequency = false;
            }
        });

        fmSeekBar.setVisibility(modeSwitch.isChecked() ? View.VISIBLE: View.GONE);
        addChannelFab.setVisibility(modeSwitch.isChecked() ? View.VISIBLE: View.GONE);
        addChannelFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (playerBound) {
                    if (stationListAdapter != null && !stationListAdapter.isDeleteMode()) {
                        if (playerService.saveCurrentFmStation()) {
                            updateStationList(playerService.getRadioMode());
                        } else {
                            Snackbar.make(
                                    stationListRecyclerView,
                                    R.string.channel_already_exists_message,
                                    Snackbar.LENGTH_SHORT
                            ).show();
                        }
                    } else {
                        stationListAdapter.closeDeleteMode();
                    }
                }
            }
        });

        nextButton = (ImageButton) findViewById(R.id.skip_next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (stationListAdapter != null) {
                    stationListAdapter.closeDeleteMode();
                }
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
                if (stationListAdapter != null) {
                    stationListAdapter.closeDeleteMode();
                }
                if (!playerBound) {
                    bindPlayerService();
                    sendActionToService(RadioPlayerService.ACTION_NEXT);
                } else {
                    playerService.handlePreviousChannelRequest();
                }
            }
        });

        searchForwardsButton = (ImageButton) findViewById(R.id.search_forward_button);
        searchForwardsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (stationListAdapter != null) {
                    stationListAdapter.closeDeleteMode();
                }
                if (!playerBound) {
                    bindPlayerService();
                    sendActionToService(RadioPlayerService.ACTION_SEARCH_FORWARDS);
                } else {
                    playerService.handleSearchForwards();
                }
            }
        });

        searchBackwardsButton = (ImageButton) findViewById(R.id.search_backwards_button);
        searchBackwardsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (stationListAdapter != null) {
                    stationListAdapter.closeDeleteMode();
                }
                if (!playerBound) {
                    bindPlayerService();
                    sendActionToService(RadioPlayerService.ACTION_SEARCH_BACKWARDS);
                } else {
                    playerService.handleSearchBackwards();
                }
            }
        });


        playButton = (ImageButton) findViewById(R.id.play_pause_button);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (stationListAdapter != null) {
                    stationListAdapter.closeDeleteMode();
                }
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
            closeVolumeUi();
        }
    };

    public void initialiseVolumeControls() {
        volumeButton = (ImageButton) findViewById(R.id.volume_button);
        volumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (stationListAdapter != null) {
                    stationListAdapter.closeDeleteMode();
                }

                if (volumeSeekbarHolder.getVisibility() == View.VISIBLE) {
                    closeVolumeUi();
                } else {
                    // Update the volume if the volume has not been set recently
                    // due to the player being disabled
                    if (!playerService.isPlaying()) {
                        updateVolume(playerService.getPlayerVolume());
                    }
                    openVolumeUi();
                }
            }
        });

        volumeSeekBar.setMax(RadioPlayerService.MAX_PLAYER_VOLUME);

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (playerBound) {
                    volumeText.setText(String.valueOf(progress));
                    if (fromUser) {
                        playerService.setPlayerVolume(progress);
                    }
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
        onRadioModeChanged(mode, true);
    }

    public void onRadioModeChanged(int mode, boolean clearAttributes) {
        if (mode == RadioDevice.Values.STREAM_MODE_DAB) {
            Animation fadeOutAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
            fadeOutAnimation.setDuration(200);
            fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    fmSeekBar.setVisibility(View.GONE);
                    searchBackwardsButton.setVisibility(View.INVISIBLE);
                    searchForwardsButton.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            fmSeekBar.startAnimation(fadeOutAnimation);
            searchBackwardsButton.startAnimation(fadeOutAnimation);
            searchForwardsButton.startAnimation(fadeOutAnimation);
            addChannelFab.hide();
        } else {
            Animation fadeInAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            fadeInAnimation.setDuration(200);
            fadeInAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    fmSeekBar.setVisibility(View.VISIBLE);
                    searchBackwardsButton.setVisibility(View.VISIBLE);
                    searchForwardsButton.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            fmSeekBar.setVisibility(View.INVISIBLE);
            fmSeekBar.startAnimation(fadeInAnimation);
            searchBackwardsButton.startAnimation(fadeInAnimation);
            searchForwardsButton.startAnimation(fadeInAnimation);

            if (selectChannelScrollRunnable != null) {
                stationListRecyclerView.removeCallbacks(selectChannelScrollRunnable);
            }
            addChannelFab.show();
            fmSeekBar.setProgress(playerService.getCurrentFmFrequency() - RadioDevice.Values.MIN_FM_FREQUENCY);
        }
        modeSwitch.setChecked(mode == RadioDevice.Values.STREAM_MODE_FM);
        updateStationList(mode);

        if (clearAttributes) {
            clearPlayerAttributes();
        }
    }

    public void openVolumeUi() {
        volumeSeekbarHolder.setVisibility(View.VISIBLE);
        volumeText.setVisibility(View.VISIBLE);
        volumeSeekbarHolder.postDelayed(seekBarIdle, 2000);
    }

    public void closeVolumeUi() {
        volumeSeekbarHolder.setVisibility(View.INVISIBLE);
        volumeText.setVisibility(View.INVISIBLE);
        volumeSeekbarHolder.removeCallbacks(seekBarIdle);

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

    public void initialiseSettingsButton() {
        settingsButton = (ImageButton) findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (stationListAdapter != null) {
                    stationListAdapter.closeDeleteMode();
                }
                startActivity(
                        new Intent(getApplicationContext(), SettingsActivity.class)
                );
            }
        });
    }

    public void initialisePlayerAttributesUi(Bundle savedInstanceState) {
        channelStatusBarLayout = (ConstraintLayout) findViewById(R.id.channel_status_bar_constraint_layout);
        fmFrequencyTextView = (TextView) findViewById(R.id.fm_frequency_text);
        channelNameTextView = (TextView) findViewById(R.id.channel_name);
        slideshowImageView = (ImageView) findViewById(R.id.slideshow_image);
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
        volumeSeekbarHolder = findViewById(R.id.volume_seekbar_holder);

        noStationsTextView = (TextView) findViewById(R.id.no_saved_stations_text);

        closeVolumeUi();

        if (savedInstanceState != null) {
            Log.v(TAG, "Loading previous states");
            fmFrequencyTextView.setText(
                    savedInstanceState.getString(String.valueOf(R.id.fm_frequency_text))
            );

            if (!fmFrequencyTextView.getText().toString().isEmpty()) {
                fmFrequencyTextView.setVisibility(View.VISIBLE);
            } else {
                fmFrequencyTextView.setVisibility(View.GONE);
            }

            updateChannelNameText(
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

            updateSlideshowImage(
                    (Bitmap) savedInstanceState.getParcelable(String.valueOf(R.id.slideshow_image))
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
        Log.d(TAG, "Clearing player attributes");
        fmFrequencyTextView.setText("");
        fmFrequencyTextView.setVisibility(View.GONE);
        signalStrengthView.setText("");
        programTextTextView.setText("");
        stereoStateTextView.setText("");
        updateSlideshowImage(null);
        onDabProgramDataRateChanged(0);
        onDabSignalQualityChanged(0);
        genreTextView.setText("");
        ensembleTextView.setText("");

        channelNameTextView.setText("");

        updatePlayerAttributesFromMetadata();
    }

    public void updatePlayerAttributesFromMetadata(boolean clearProgramText) {
        if (playerBound) {
            RadioStation currentStation = playerService.getCurrentStation();
            if (currentStation != null) {
                updateChannelNameText(currentStation.getName());
                updateEnsembleName(currentStation.getEnsemble());
                updateGenreName(RadioDevice.StringValues.getGenreFromId(currentStation.getGenreId()));

                updateSlideshowImage(
                        playerService
                                .getMetadata()
                                .getBitmap(
                                        MediaMetadataCompat.METADATA_KEY_ART
                                )
                );
                programTextTextView.setText(
                        playerService
                                .getMetadata()
                                .getString(
                                        MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION
                                )
                );


                if (playerService.getRadioMode() == RadioDevice.Values.STREAM_MODE_DAB) {
                    updateStationListSelection(playerService.getCurrentDabChannelIndex());
                }

                if (playerService.getRadioMode() == RadioDevice.Values.STREAM_MODE_FM) {
                    fmFrequencyTextView.setText(
                            new DecimalFormat("#.0")
                                    .format(currentStation.getFrequency() / 1000.0)
                    );
                    fmFrequencyTextView.setVisibility(View.VISIBLE);

                    if (!userChangingFmFrequency) {
                        fmSeekBar.setProgress((playerService.getCurrentFmFrequency() -
                                RadioDevice.Values.MIN_FM_FREQUENCY) / 100);
                    }

                    updateStationListSelection(playerService.getCurrentSavedFmStationIndex());
                }
            }
        } else {
            updateChannelNameText("");
            updateEnsembleName("");
            updateGenreName("");
            if (fmSeekBar != null) {
                fmSeekBar.setProgress(0);
            }
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

    public void updateChannelNameText(String channelName) {
        final int newVisibility;

        if (channelName.length() > 0) {
            newVisibility = View.VISIBLE;
        } else {
            newVisibility = View.GONE;
        }

        //if (channelNameTextView.getVisibility() != newVisibility) {
        /*Log.e(TAG, "APPLYING TRANSISION?");
        TransitionManager.beginDelayedTransition((ConstraintLayout) findViewById(R.id.player_layout), new TransitionSet()
                .addTransition(new ChangeBounds()).addTransition(new Fade()));
*/
        //TransitionManager.endTransitions((ConstraintLayout) findViewById(R.id.player_layout));
        //}

        channelNameTextView.setVisibility(newVisibility);
        channelNameTextView.setText(channelName);
    }

    public void updateEnsembleName(String ensembleName) {
        ensembleTextView.setText(ensembleName);
    }

    public void updateGenreName(String genre) {
        genreTextView.setText(genre);
    }

    public void updateSlideshowImage(Bitmap slideshowImageBitmap) {
        currentSlideshowImageBitmap = slideshowImageBitmap;
        if (slideshowImageBitmap != null) {
            slideshowImageView.setVisibility(View.VISIBLE);
            slideshowImageView.setImageBitmap(slideshowImageBitmap);
        } else {
            slideshowImageView.setVisibility(View.GONE);
        }
    }

    public void updateStationList(int radioMode) {
        if (radioMode == RadioDevice.Values.STREAM_MODE_FM) {
            stationListAdapter.initialiseNewStationList(playerService.getFmRadioStations(), radioMode);

            if (playerService.getCurrentSavedFmStationIndex() > -1) {
                stationListAdapter.onCurrentStationChanged(playerService.getCurrentSavedFmStationIndex());
            }

            if (playerService.getFmRadioStations().length < 1) {
                noStationsTextView.setVisibility(View.VISIBLE);
            } else {
                noStationsTextView.setVisibility(View.GONE);
            }
        } else {
            stationListAdapter.initialiseNewStationList(playerService.getDabRadioStations(), radioMode);
            if (playerService.getCurrentDabChannelIndex() > -1) {
                stationListAdapter.onCurrentStationChanged(playerService.getCurrentDabChannelIndex());
            }

            if (playerService.getDabRadioStations().length < 1) {
                noStationsTextView.setVisibility(View.VISIBLE);
            } else {
                noStationsTextView.setVisibility(View.GONE);
            }
        }

    }

    public void updateStationListSelection(final int channelIndex) {
        stationListAdapter.onCurrentStationChanged(channelIndex);
    }

    public void updateStationListCursorPosition(final int newCursorIndex) {
        stationListAdapter.onCursorPositionChanged(newCursorIndex);
    }

    public void onChannelListDeleteModeChanged(boolean deleteMode) {
        if (deleteMode) {
            addChannelFab.startAnimation(fabForwardsAnimation);
        } else {
            addChannelFab.startAnimation(fabBackwardsAnimation);
        }
    }

    public void handleChannelClicked(int channelIndex) {
        if (playerBound) {
            if (playerService.getRadioMode() == RadioDevice.Values.STREAM_MODE_FM) {
                playerService.handleSetFmFrequencyRequest(
                        playerService.getFmRadioStations()[channelIndex].getFrequency()
                );
            } else {
                playerService.handleSetDabChannelRequest(channelIndex);
            }

            if (playerService.getPlaybackState() == PlaybackStateCompat.STATE_STOPPED) {
                playerService.handlePlayRequest();
            }
        }
    }

    public void handleRemoveFmChannel(RadioStation radioStation) {
        playerService.removeFmRadioStation(radioStation);
        if (playerService.getFmRadioStations().length < 1) {
            updateStationList(playerService.getRadioMode());
        }
    }

    public void handleNextCursorPosition() {
        int newPosition;

        if (sharedPreferences.getBoolean(getString(R.string.pref_cursor_beta_mode), false)) {
            newPosition = stationListAdapter.getLastScrollIndex() + 1;
        } else {
            newPosition = stationListAdapter.getCurrentScrollIndex() + 1;
        }

        if (newPosition >= stationListAdapter.getItemCount()) {
            if (sharedPreferences.getBoolean(
                    getString(R.string.PREF_CURSOR_SCROLL_WRAP),
                    false
                )) {
                newPosition = 0;
            } else {
                newPosition = -1;
            }
        }

        if (newPosition != -1) {
            updateStationListCursorPosition(newPosition);
        }
    }

    public void handlePreviousCursorPosition() {
        int newPosition;

        if (sharedPreferences.getBoolean(getString(R.string.pref_cursor_beta_mode), false)) {
            newPosition = stationListAdapter.getLastScrollIndex() - 1;
        } else {
            newPosition = stationListAdapter.getCurrentScrollIndex() - 1;
        }

        if (newPosition < 0) {
            if (sharedPreferences.getBoolean(
                    getString(R.string.PREF_CURSOR_SCROLL_WRAP),
                    false
                )) {
                newPosition = stationListAdapter.getItemCount() - 1;
            } else {
                newPosition = -1;
            }
        }
        if (newPosition != -1) {
            updateStationListCursorPosition(newPosition);
        }
    }

    @Override
    public void onNewProgramText(String programText) {
        //programTextTextView.setText(programText);
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
        if (dataRate > 0) {
            dataRateTextView.setText(getString(R.string.program_datarate_placeholder, dataRate));
        } else {
            dataRateTextView.setText("");
        }
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
    public void onNewSlideshowImage(Bitmap bitmap) {

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

    public void onDeviceAttachTimeout() {
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
            if (playerService.getRadioMode() == RadioDevice.Values.STREAM_MODE_DAB) {
                updateStationList(playerService.getRadioMode());
            }
            playerService.handlePlayRequest();
        }
    }

    public void onDismissed() {
        Log.v(TAG, "Received intent radio notification dismissed");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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
        outState.putString(String.valueOf(R.id.fm_frequency_text),
                fmFrequencyTextView.getText().toString());

        outState.putString(String.valueOf(R.id.channel_name),
                channelNameTextView.getText().toString());

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

        outState.putParcelable(String.valueOf(R.id.slideshow_image), currentSlideshowImageBitmap);

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
                    if (playerBound &&
                            playerService.getRadioMode() == RadioDevice.Values.STREAM_MODE_DAB) {
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
                if (stationListAdapter != null && stationListAdapter.isDeleteMode()) {
                    stationListAdapter.closeDeleteMode();
                } else {
                    finish();
                }
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
        } else if (!sharedPreferences.getBoolean(
                getString(R.string.PREF_HEADUNIT_CONTROLLER_INPUT),
                false
            )) {
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
