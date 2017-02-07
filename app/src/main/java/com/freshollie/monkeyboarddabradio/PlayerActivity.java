package com.freshollie.monkeyboarddabradio;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.freshollie.monkeyboarddabradio.playback.RadioPlayerService;
import com.freshollie.monkeyboarddabradio.radio.ListenerManager;
import com.freshollie.monkeyboarddabradio.radio.RadioDevice;
import com.freshollie.monkeyboarddabradio.radio.RadioStation;

public class PlayerActivity extends AppCompatActivity implements ListenerManager.DataListener,
        RadioPlayerService.PlayerCallback {
    private String TAG = this.getClass().getSimpleName();

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

    private SharedPreferences sharedPreferences;

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to PLa, cast the IBinder and get LocalService instance
            RadioPlayerService.RadioPlayerBinder binder = (RadioPlayerService.RadioPlayerBinder) service;
            playerService = binder.getService();

            radio = playerService.getRadio();
            playerService.getMediaController().registerCallback(mediaControllerCallback);

            playerBound = true;
            onPlaybackServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            playerBound = false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        sharedPreferences = getSharedPreferences(getString(R.string.SHARED_PREFERENCES_KEY), Context.MODE_PRIVATE);

        startService(new Intent(this, RadioPlayerService.class));

        bindService(
                new Intent(this, RadioPlayerService.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT
        );

    }

    public void onPlaybackServiceConnected() {
        radio.getListenerManager().registerDataListener(this);

        setupPlaybackControls();
        setupVolumeControls();
        setupSettingsButton();
        setupPlayerAttributes();
    }

    public void setupPlaybackControls() {
        nextButton = (ImageButton) findViewById(R.id.skip_next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playerService.handleNextChannelRequest();
            }
        });

        previousButton = (ImageButton) findViewById(R.id.skip_previous_button);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playerService.handlePreviousChannelRequest();
            }
        });

        playPauseButton = (ImageButton) findViewById(R.id.play_pause_button);
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (playerService.getPlaybackState() == PlaybackStateCompat.STATE_PLAYING) {
                    playerService.handlePauseRequest();
                } else {
                    playerService.handlePlayRequest();
                }
            }
        });
        updatePlayIcon(playerService.getPlaybackState());
    }

    public void setupVolumeControls() {
        volumeButton = (ImageButton) findViewById(R.id.volume_button);
        onVolumeChanged(playerService.getPlayerVolume());
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
        currentChannelView.setText("");

        dataRateTextView = (TextView) findViewById(R.id.data_rate);

        ensembleTextView = (TextView) findViewById(R.id.station_ensemble_name);
        ensembleTextView.setText("");

        genreTextView = (TextView) findViewById(R.id.station_genre);
        genreTextView.setText("");

        playStatusView = (TextView) findViewById(R.id.play_status);
        onPlayStatusChanged(RadioDevice.Values.PLAY_STATUS_STREAM_STOP);

        signalStrengthView = (TextView) findViewById(R.id.signal_strength);
        signalStrengthView.setText("");

        signalStrengthIcon = (ImageView) findViewById(R.id.signal_strength_icon);
        onSignalQualityChanged(0);

        programTextTextView = (TextView) findViewById(R.id.program_text);
        programTextTextView.setText("");

        stereoStateTextView = (TextView) findViewById(R.id.program_stereo_mode);
        stereoStateTextView.setText("");
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

    public void clearProgramText() {
        programTextTextView.setText("");
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

    @Override
    public void onNoSavedStations() {
        Dialog noSavedStationsDialog = new Dialog()
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (playerBound) {
            unbindService(serviceConnection);
        }
    }

    private MediaControllerCompat.Callback mediaControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            RadioStation currentStation = playerService.getCurrentStation();
            updateCurrentChannelName(currentStation.getName());
            updateEnsembleName(currentStation.getEnsemble());
            Log.v(TAG, String.valueOf(currentStation.getGenreId()));
            updateGenreName(RadioDevice.StringValues.getGenreFromId(currentStation.getGenreId()));
            programTextTextView.setText("");
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            updatePlayIcon(state.getState());
        }
    };

}
