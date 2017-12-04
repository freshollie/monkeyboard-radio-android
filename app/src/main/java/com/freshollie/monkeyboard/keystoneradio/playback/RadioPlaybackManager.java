package com.freshollie.monkeyboard.keystoneradio.playback;

import android.content.Intent;
import android.media.AudioManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import com.freshollie.monkeyboard.keystoneradio.R;
import com.freshollie.monkeyboard.keystoneradio.radio.RadioDevice;
import com.freshollie.monkeyboard.keystoneradio.radio.RadioStation;

/**
 * Created by freshollie on 27.11.17.
 */

public class RadioPlaybackManager {


    private RadioStation currentStation;

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat playbackState;

    private AudioManager audioManager;
    private VolumeManager volumeManager;
    private RadioPlayerService playerService;
    private RadioPlayback radioPlayback;

    public RadioPlaybackManager(RadioPlayerService service) {
        playerService = service;
        radioPlayback = new RadioPlayback(playerService);

        volumeManager = new VolumeManager(radioPlayback);
    }

    public RadioPlayerService getPlayerService() {
        return playerService;
    }

    public void handleSetVolume(int newVolume) {

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
            handleAction(new Runnable() {
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

            });
        }
    }

    public void handleUpdateBoardStereoMode() {
        handleAction(new Runnable() {
            @Override
            public void run() {
                updateBoardStereoModeAction();
            }
        });
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
        if (!radio.isAttached()) {
            updatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
            return;
        }

        handleAction(new Runnable() {
            @Override
            public void run() {
                muted = true;

                // Used to make sure that the volume is lowered, command
                // is executed until the volume is confirmed lowered

                //noinspection StatementWithEmptyBody
                while (!radio.setVolume(0) && radio.isConnected()) ;

                if (radio.isConnected() && radio.getPlayStatus() == RadioDevice.Values.PLAY_STATUS_SEARCHING) {
                    radio.stopSearch();
                }
                updatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
            }
        });
    }

    private void handleMoveChannelRequest(int direction) {
        if (direction == 0) {
            return;
        }

        if (getRadioMode() == RadioDevice.Values.STREAM_MODE_DAB) {
            if (getDabRadioStations().length > 0) {
                int nextDabChannel =
                        modulus(
                                currentDabChannelIndex + direction,
                                getDabRadioStations().length
                        );
                handleSetDabChannelRequest(nextDabChannel);
            }

        } else {
            RadioStation[] radioStations = getFmRadioStations();
            // We have saved stations
            if (radioStations.length > 0) {
                int currentFmChannelIndex = getCurrentSavedFmStationIndex();

                int nextChannelIndex = radioStations.length;

                // We are not currently on a station
                if (currentFmChannelIndex < 0) {
                    // So find what would be the next station
                    for (int i = 0; i < radioStations.length; i++) {
                        if (radioStations[i].getFrequency() >
                                getCurrentStation().getFrequency()) {
                            if (direction > 0) {
                                nextChannelIndex = i;
                            }
                            break;
                        }
                        if (direction < 0) {
                            nextChannelIndex = i;
                        }
                    }
                } else {
                    nextChannelIndex = currentFmChannelIndex + direction;
                }

                // Wrap around
                nextChannelIndex = modulus(nextChannelIndex, radioStations.length);
                handleSetFmFrequencyRequest(
                        radioStations[nextChannelIndex].getFrequency()
                );
            }
        }
    }

    public void handleNextChannelRequest() {
        handleMoveChannelRequest(+1);
    }

    public void handlePreviousChannelRequest() {
        handleMoveChannelRequest(-1);
    }

    private void handleFocusDuck() {
        ducked = true;
        radio.setVolume(
                sharedPreferences.getInt(
                        getString(R.string.DUCK_VOLUME_KEY),
                        3
                )
        );
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
        handleUpdateBoardStereoMode();
        notifyRadioModeChanged(newRadioMode);
        if (newRadioMode == RadioDevice.Values.STREAM_MODE_DAB) {
            currentFmRadioStation = null;
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

    public void handleStopSearch() {
        handleAction(new Runnable() {
            @Override
            public void run() {
                radio.stopDabSearch();

            }
        });
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onFastForward() {
            super.onFastForward();
            handleSearchForwards();
        }

        @Override
        public void onRewind() {
            super.onRewind();
            handleSearchBackwards();
        }

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
            if (sharedPreferences.getBoolean(getString(R.string.pref_skip_buttons_control_key),
                    true)) {
                handleNextChannelRequest();
            } else {
                handleSearchForwards();
            }
        }

        @Override
        public void onSkipToPrevious() {
            if (sharedPreferences.getBoolean(getString(R.string.pref_skip_buttons_control_key),
                    true)) {
                handlePreviousChannelRequest();
            } else {
                handleSearchBackwards();
            }
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
            Log.v(TAG, "Got media button intent");
            KeyEvent event = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

            if (event.getAction() == KeyEvent.ACTION_UP) {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        if (sharedPreferences.getBoolean(
                                getString(R.string.PREF_HEADUNIT_CONTROLLER_INPUT),
                                false
                        )) {
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
                        onPlay();
                        return true;

                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        onSkipToNext();
                        return true;

                    case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                        onFastForward();
                        return true;

                    case KeyEvent.KEYCODE_MEDIA_REWIND:
                        onRewind();
                        return true;

                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        onSkipToPrevious();
                        return true;

                    case KeyEvent.KEYCODE_MEDIA_STOP:
                        onPause();
                        return true;

                    default:
                        return false;
                }
            } else {
                return false;
            }
        }
    }
}
