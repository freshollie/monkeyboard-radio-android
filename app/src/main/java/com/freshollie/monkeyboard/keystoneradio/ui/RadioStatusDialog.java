package com.freshollie.monkeyboard.keystoneradio.ui;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.freshollie.monkeyboard.R;
import com.freshollie.monkeyboard.keystoneradio.playback.RadioPlayerService;
import com.freshollie.monkeyboard.keystoneradio.radio.ListenerManager;
import com.freshollie.monkeyboard.keystoneradio.radio.RadioDevice;
import com.github.rahatarmanahmed.cpv.CircularProgressView;

/**
 * Created by Freshollie on 11/02/2017.
 * Dialog used to show progress of a DAB search or channel copy
 */

public class RadioStatusDialog extends DialogFragment{
    private final String TAG = getClass().getSimpleName();
    private RadioPlayerService playerService;
    private RadioDevice radio;

    public enum State {
        Connecting,
        Searching,
        Copying,
        Failed
    }

    private State currentState = State.Connecting;

    private CircularProgressView progressIcon;
    private TextView statusText;
    private TextView progressText;

    public void setPlayerService(RadioPlayerService service) {
        playerService = service;
        radio = playerService.getRadio();
    }

    public void setInitialState(State initialState) {
        currentState = initialState;
    };

    private void setState(State newState) {
        currentState = newState;
        onStateUpdated();
    }

    private void onStateUpdated() {
        if (currentState == State.Connecting) {
            statusText.setText(getString(R.string.dialog_dab_search_status_connecting));
            progressIcon.setIndeterminate(true);
            progressIcon.setVisibility(View.VISIBLE);
            progressText.setText("");
            if (radio.isConnected()) {
                setState(State.Searching);
            } else {
                playerService.openConnection();
            }

        } else if (currentState == State.Searching) {
            statusText.setText(getString(R.string.dialog_dab_search_status_searching));
            if (!progressIcon.isIndeterminate()) {
                progressIcon.setIndeterminate(true);
            }
            progressIcon.setVisibility(View.VISIBLE);
            progressIcon.setProgress(0);
            progressIcon.setMaxProgress(RadioDevice.Values.MAX_CHANNEL_BAND);
            progressText.setText(getString(R.string.dialog_dab_search_found_channels_progress, 0));
            if (radio.isConnected()) {
                playerService.startChannelSearchTask();
            }

        } else if (currentState == State.Copying) {
            statusText.setText(getString(R.string.dialog_dab_search_status_copying));
            progressIcon.setVisibility(View.VISIBLE);
            progressIcon.setIndeterminate(false);
            progressIcon.setProgress(0);
            progressText.setText(getString(R.string.dialog_dab_search_copying_channels_progress, 0));
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View view = inflater.inflate(R.layout.search_dialog_content, null);

        progressIcon = (CircularProgressView) view.findViewById(R.id.search_dialog_progress_icon);
        progressText = (TextView) view.findViewById(R.id.search_dialog_progress_text);
        statusText = (TextView) view.findViewById(R.id.search_dialog_status_text);

        builder.setView(view);

        builder.setCancelable(false);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dismiss();
            }
        });

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();    //super.onStart() is where dialog.show()
        // is actually called on the underlying dialog, so we have to do it after this point

        if (playerService == null) {
            throw new RuntimeException("PlayerService needs to be given");
        }
        getDialog().setCanceledOnTouchOutside(false);

        playerService.registerCallback(playerCallback);

        radio.getListenerManager()
                .registerConnectionStateChangedListener(connectionStateChangeListener);
        onStateUpdated();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        playerService.unregisterCallback(playerCallback);
        radio.getListenerManager()
                .unregisterConnectionStateChangedListener(connectionStateChangeListener);
        if (radio.isConnected() &&
                radio.getPlayStatus() == RadioDevice.Values.PLAY_STATUS_SEARCHING) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        if (radio.stopSearch() ||
                                !radio.isConnected() ||
                                radio.getPlayStatus() != RadioDevice.Values.PLAY_STATUS_SEARCHING) {
                            break;
                        }
                    }
                }
            }).start();

        }
    }

    private RadioPlayerService.PlayerCallback playerCallback = new RadioPlayerService.PlayerCallback() {
        @Override
        public void onNoStoredStations() {
            if (currentState != State.Connecting) {
                statusText.setText(getString(R.string.dialog_dab_search_status_failed));
                progressIcon.setVisibility(View.INVISIBLE);
                progressText.setText(getString(R.string.dialog_dab_search_failed_no_channels_found));
                new AlertDialog.Builder(getActivity())
                        .setMessage(getString(R.string.dialog_dab_search_try_again_message))
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                setState(State.Connecting);
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (getActivity().getClass().getSimpleName()
                                        .equals("PlayerActivity")) {
                                    playerService.closeConnection();
                                    dismiss();
                                    getActivity().finish();
                                }
                            }
                        })
                        .show();
            } else {
                setState(State.Searching);
            }
        }

        @Override
        public void onAttachTimeout() {
            statusText.setText(getString(R.string.dialog_dab_search_status_failed));
            progressIcon.setVisibility(View.INVISIBLE);
            progressText.setText(getString(R.string.dialog_dab_search_failed_timed_out));

            new AlertDialog.Builder(getActivity())
                    .setMessage(getString(R.string.device_connection_timed_out_try_again))
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            playerService.openConnection();
                            setState(State.Connecting);
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dismiss();
                        }
                    })
                    .show();
        }

        @Override
        public void onSearchStart() {
            if (currentState != State.Searching) {
                setState(State.Searching);
            }
        }

        @Override
        public void onSearchProgressUpdate(int numChannels, int progress) {
            if (currentState != State.Searching) {
                setState(State.Searching);
            }
            progressText.setText(
                    getString(R.string.dialog_dab_search_found_channels_progress,
                            numChannels)
            );
            if (progressIcon.isIndeterminate()) {
                progressIcon.setIndeterminate(false);
            }
            progressIcon.setProgress(progress);
            Log.v(TAG, "Search progress updated "+ String.valueOf(numChannels) + " " + String.valueOf(progress));
        }

        @Override
        public void onSearchComplete(int numChannels) {
            playerService.startStationListCopyTask();
        }

        @Override
        public void onStationListCopyStart() {
            Log.v(TAG, "list copy start");
            if (currentState != State.Copying) {
                setState(State.Copying);
            }
        }

        @Override
        public void onStationListCopyProgressUpdate(int progress, int max) {
            if (currentState != State.Copying) {
                setState(State.Copying);
            }
            progressText.setText(
                    getString(R.string.dialog_dab_search_copying_channels_progress,
                            progress)
            );
            if (progressIcon.getMaxProgress() != max) {
                progressIcon.setMaxProgress(max);
            }

            progressIcon.setProgress(max);
        }

        @Override
        public void onStationListCopyComplete() {
            dismiss();
        }

        @Override
        public void onDismissed() {
            dismiss();
        }
    };

    private ListenerManager.ConnectionStateChangeListener connectionStateChangeListener =
            new ListenerManager.ConnectionStateChangeListener() {
        @Override
        public void onStart() {
            setState(State.Searching);
            onStateUpdated();
        }

        @Override
        public void onStop() {
            dismiss();
        }
    };
}
