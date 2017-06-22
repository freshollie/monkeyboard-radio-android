/*
 * Created by Oliver Bell on 08/02/2017
 * Copyright (c) 2017. by Oliver bell <freshollie@gmail.com>
 *
 * Last modified 14/06/17 23:42
 */

package com.freshollie.monkeyboard.keystoneradio.ui;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.freshollie.monkeyboard.keystoneradio.R;
import com.freshollie.monkeyboard.keystoneradio.radio.RadioDevice;
import com.freshollie.monkeyboard.keystoneradio.radio.RadioStation;

import java.text.DecimalFormat;

/**
 * Station list adapter is used to display the radio stations in a recycler view. It features
 * functionality for the user to delete FM stations, scrolling through stations and highlighting
 * the currently playing station
 */

public class StationListAdapter extends RecyclerView.Adapter<StationListAdapter.StationCard> {
    private RadioStation[] stationList = new RadioStation[0];
    private PlayerActivity playerActivity;
    private int cursorIndex = 0;
    private int currentStationIndex = 0;
    private int lastStationIndex = 0;
    private int lastCursorIndex;
    private boolean deleteMode;
    private RecyclerView recyclerView;
    private int radioMode;

    public static class StationCard extends RecyclerView.ViewHolder {
        public TextView stationName;
        public TextView stationGenre;
        public TextView stationEnsemble;
        public View stationItemBackground;
        public RelativeLayout stationSelectionLayout;
        public View stationTopDivide;
        public View stationBottomDivide;
        public View stationRemoveButton;

        public StationCard(View v) {
            super(v);
            stationName = (TextView) v.findViewById(R.id.station_name_card_text);
            stationGenre = (TextView) v.findViewById(R.id.station_genre_card_text);
            stationEnsemble = (TextView) v.findViewById(R.id.station_ensemble_name_card_text);
            stationItemBackground = v.findViewById(R.id.station_item_background);
            stationSelectionLayout = (RelativeLayout) v.findViewById(R.id.station_item_layout);
            stationTopDivide = v.findViewById(R.id.top_divide);
            stationBottomDivide = v.findViewById(R.id.bottom_divide);
            stationRemoveButton = v.findViewById(R.id.station_remove_button);
        }
    }

    public StationListAdapter(PlayerActivity activity) {
        playerActivity = activity;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public StationListAdapter.StationCard onCreateViewHolder(ViewGroup parent,
                                                             int viewType) {
        // create a new view
        RelativeLayout stationCardView = (RelativeLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.station_card_layout, parent, false);
        return new StationCard(stationCardView);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.recyclerView = null;
    }

    @Override
    public void onBindViewHolder(final StationCard stationCard, final int position) {
        final RadioStation radioStation = stationList[position];

        stationCard.stationName.setText(radioStation.getName());

        if (radioStation.getFrequency() >= RadioDevice.Values.MIN_FM_FREQUENCY) {
            // We are an FM station but we have a name so set the ensemble text to the frequency
            // text
            stationCard.stationEnsemble.setText(
                    new DecimalFormat("#.0")
                            .format(radioStation.getFrequency() / 1000.0)
            );
        } else {
            stationCard.stationEnsemble.setText(radioStation.getEnsemble());
        }

        stationCard.stationGenre.setText(
                RadioDevice.StringValues.getGenreFromId(radioStation.getGenreId()
                )
        );

        stationCard.stationItemBackground.setAlpha(1f);
        if (position == currentStationIndex) {
            stationCard.stationItemBackground.setBackgroundColor(ContextCompat
                    .getColor(playerActivity, R.color.colorPrimaryDark)
            );
        }

        if (position == cursorIndex && !deleteMode) {
            stationCard.stationBottomDivide.setBackgroundColor(ContextCompat
                    .getColor(playerActivity, R.color.colorPrimaryDark)
            );

            stationCard.stationTopDivide.setBackgroundColor(ContextCompat
                    .getColor(playerActivity, R.color.colorPrimaryDark)
            );

            if (position != currentStationIndex) {
                stationCard.stationItemBackground.setBackgroundColor(ContextCompat
                        .getColor(playerActivity, R.color.colorAccent)
                );
                stationCard.stationItemBackground.setAlpha(0.3f);
            }

        } else {
            stationCard.stationBottomDivide.setBackgroundColor(ContextCompat
                    .getColor(playerActivity, R.color.backgroundDarker)
            );
            stationCard.stationTopDivide.setBackgroundColor(ContextCompat
                    .getColor(playerActivity, R.color.backgroundDarker)
            );
        }

        if (position != currentStationIndex && position != cursorIndex) {
            stationCard.stationItemBackground.setBackgroundColor(0);
        }

        if (deleteMode) {
            stationCard.stationItemBackground.setBackgroundColor(ContextCompat
                    .getColor(playerActivity, R.color.colorHighlight)
            );
            stationCard.stationItemBackground.setAlpha(0.3f);
        }

        if (radioMode == RadioDevice.Values.STREAM_MODE_FM) {
            stationCard.stationSelectionLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (deleteMode) {
                        closeDeleteMode();
                    } else {
                        openDeleteMode();
                    }
                    return true;
                }
            });
        }

        if (deleteMode && radioMode == RadioDevice.Values.STREAM_MODE_FM) {
            stationCard.stationSelectionLayout.setOnClickListener(null);
            stationCard.stationRemoveButton.setVisibility(View.VISIBLE);
            stationCard.stationRemoveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    playerActivity.handleRemoveFmChannel(radioStation);
                    onStationRemoved(stationCard.getAdapterPosition());
                }
            });
        } else {
            stationCard.stationSelectionLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    playerActivity.handleChannelClicked(stationCard.getAdapterPosition());
                }
            });
            stationCard.stationRemoveButton.setVisibility(View.INVISIBLE);
        }
    }

    public void openDeleteMode() {
        if (!isDeleteMode()) {
            deleteMode = true;
            if (recyclerView != null) {
                recyclerView.getItemAnimator().setChangeDuration(200);
                recyclerView.getItemAnimator().setRemoveDuration(200);
                recyclerView.getItemAnimator().setMoveDuration(200);
                recyclerView.getItemAnimator().setAddDuration(200);
                notifyItemRangeChanged(0, getItemCount());
            }
            playerActivity.onChannelListDeleteModeChanged(deleteMode);
        }
    }

    public void closeDeleteMode() {
        if (isDeleteMode()) {
            deleteMode = false;
            if (recyclerView != null) {
                notifyItemRangeChanged(0, getItemCount());
                recyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        recyclerView.getItemAnimator().setChangeDuration(0);
                        recyclerView.getItemAnimator().setRemoveDuration(0);
                        recyclerView.getItemAnimator().setMoveDuration(0);
                        recyclerView.getItemAnimator().setAddDuration(0);
                    }
                }, 300);
            }
            playerActivity.onChannelListDeleteModeChanged(deleteMode);
        }
    }

    public boolean isDeleteMode() {
        return deleteMode;
    }

    private void onStationRemoved(int index) {
        if (stationList.length > 0) {
            RadioStation[] stations = new RadioStation[stationList.length - 1];
            int j = 0;
            for (int i = 0; i < stationList.length; i++) {
                if (i != index) {
                    stations[j] = stationList[i];
                    j++;
                }
            }
            stationList = stations;
        } else {
            stationList = new RadioStation[0];


        }


        notifyItemRemoved(index);

        if (stationList.length < 1) {
            closeDeleteMode();
        }
    }

    @Override
    public int getItemCount() {
        return stationList.length;
    }

    public void updateStationList(RadioStation[] newStationList, int radioMode) {
        stationList = newStationList.clone();
        this.radioMode = radioMode;
        if (radioMode != RadioDevice.Values.STREAM_MODE_FM && isDeleteMode()) {
            closeDeleteMode();
        }
        notifyDataSetChanged();
    }

    public RadioStation[] getStationList() {
        return stationList;
    }


    public void setCursorIndex(int channelIndex) {
        Log.v("StationListAdapter", "Setting new cursorPosition " + String.valueOf(channelIndex));
        cursorIndex = channelIndex;
    }

    public void notifyCursorPositionChanged() {
        notifyItemChanged(lastCursorIndex);
        notifyItemChanged(cursorIndex);
        notifyCurrentStationChanged();
        lastCursorIndex = cursorIndex;
    }

    public void setCurrentStationIndex(int channelIndex) {
        currentStationIndex = channelIndex;
    }

    public int getCurrentStationIndex() {
        return currentStationIndex;
    }

    public void notifyCurrentStationChanged() {
        Log.v("StationListAdapter", "Updating current playing station " + String.valueOf(currentStationIndex));

        if (currentStationIndex == lastStationIndex) {
            return;
        }
        int lastChannelCursor = cursorIndex;
        cursorIndex = currentStationIndex;

        notifyItemChanged(lastChannelCursor);
        notifyItemChanged(currentStationIndex);
        notifyItemChanged(lastStationIndex);

        lastStationIndex = currentStationIndex;
    }

    public int getCursorIndex() {
        return cursorIndex;
    }
}
