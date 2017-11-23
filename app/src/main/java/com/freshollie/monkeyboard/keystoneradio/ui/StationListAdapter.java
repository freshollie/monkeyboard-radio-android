/*
 * Created by Oliver Bell on 08/02/2017
 * Copyright (c) 2017. by Oliver bell <freshollie@gmail.com>
 *
 * Last modified 14/06/17 23:42
 */

package com.freshollie.monkeyboard.keystoneradio.ui;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private boolean deleteMode;
    private RecyclerView recyclerView;
    private int radioMode;

    private int currentScrollIndex = 0;

    private StationListLayoutManager layoutManager;

    private int targetScrollIndex = -1;

    private Runnable scrollRunnable = null;

    private int nextScrollIndex = -1;
    private int scrollingToIndex = -1;

    private boolean waitForIdleScroll = false;

    private OnScrollListener onScrollListener = new OnScrollListener() {

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            if (recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE && scrollingToIndex != -1) {
                currentScrollIndex = scrollingToIndex;
            }

            if (waitForIdleScroll && recyclerView.getScrollState() != RecyclerView.SCROLL_STATE_IDLE) {
                return;
            }

            if (nextScrollIndex != -1) {
                scrollingToIndex = -1;
                startNextScroll();
            }
        }
    };

    public static class StationCard extends RecyclerView.ViewHolder {
        TextView stationName;
        TextView stationGenre;
        TextView stationEnsemble;
        CardView stationCardLayout;
        View stationRemoveButton;

        StationCard(View v) {
            super(v);
            stationName = (TextView) v.findViewById(R.id.station_name_card_text);
            stationGenre = (TextView) v.findViewById(R.id.station_genre_card_text);
            stationEnsemble = (TextView) v.findViewById(R.id.station_ensemble_name_card_text);
            stationCardLayout = (CardView) v.findViewById(R.id.station_item_layout);
            stationRemoveButton = v.findViewById(R.id.station_remove_button);
        }
    }

    public StationListAdapter(PlayerActivity playerActivity) {
        this.playerActivity = playerActivity;
    }

    // Create a new station card for the station list
    @Override
    public StationCard onCreateViewHolder(ViewGroup parent,
                                                             int viewType) {
        // create a new view
        CardView stationCardView = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.station_card_layout, parent, false);
        return new StationCard(stationCardView);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
        this.recyclerView.addOnScrollListener(onScrollListener);
        layoutManager = (StationListLayoutManager) recyclerView.getLayoutManager();

        setAnimations(true);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.recyclerView.removeOnScrollListener(onScrollListener);
        this.recyclerView = null;
        this.layoutManager = null;
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

        stationCard.stationCardLayout.setCardBackgroundColor(ContextCompat.getColor(playerActivity, R.color.backgroundGrey));
        if (position == currentStationIndex) {
            stationCard.stationCardLayout.setCardBackgroundColor(ContextCompat
                    .getColor(playerActivity, R.color.colorPrimaryDark)
            );
        }

        if (position == cursorIndex && !deleteMode) {

            if (position != currentStationIndex) {
                stationCard.stationCardLayout.setCardBackgroundColor(ContextCompat
                        .getColor(playerActivity, R.color.colorAccent)
                );
                stationCard.stationCardLayout.setCardBackgroundColor(
                        stationCard.stationCardLayout.getCardBackgroundColor().withAlpha(80)
                );
            }

        }

        if (position != currentStationIndex && position != cursorIndex) {
            //stationCard.stationItemBackground.setBackgroundColor(0);
        }

        if (deleteMode) {
            stationCard.stationCardLayout.setCardBackgroundColor(ContextCompat
                    .getColor(playerActivity, R.color.colorHighlight)
            );
            stationCard.stationCardLayout.setCardBackgroundColor(
                    stationCard.stationCardLayout.getCardBackgroundColor().withAlpha(80)
            );
        }


        stationCard.stationCardLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (radioMode == RadioDevice.Values.STREAM_MODE_FM) {
                    if (deleteMode) {
                        closeDeleteMode();
                    } else {
                        openDeleteMode();
                    }
                    return true;
                }
                return false;
            }
        });

        if (deleteMode && radioMode == RadioDevice.Values.STREAM_MODE_FM) {
            stationCard.stationCardLayout.setOnClickListener(null);
            stationCard.stationRemoveButton.setVisibility(View.VISIBLE);
        } else {
            stationCard.stationRemoveButton.setVisibility(View.INVISIBLE);
        }

        stationCard.stationCardLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (deleteMode) {
                    playerActivity.handleRemoveFmChannel(radioStation);
                    onStationRemoved(stationCard.getAdapterPosition());
                } if (currentStationIndex == stationCard.getAdapterPosition()) {
                    scrollWhenPossible(stationCard.getAdapterPosition());
                } else {
                    playerActivity.handleChannelClicked(stationCard.getAdapterPosition());
                }
            }
        });
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

    public void setAnimations(boolean on) {
        if (on) {
            recyclerView.getItemAnimator().setChangeDuration(0);
            recyclerView.getItemAnimator().setRemoveDuration(0);
            recyclerView.getItemAnimator().setMoveDuration(0);
            recyclerView.getItemAnimator().setAddDuration(0);
        } else {
            recyclerView.getItemAnimator().setChangeDuration(0);
            recyclerView.getItemAnimator().setRemoveDuration(0);
            recyclerView.getItemAnimator().setMoveDuration(0);
            recyclerView.getItemAnimator().setAddDuration(0);
        }
    }

    public void initialiseNewStationList(RadioStation[] newStationList, int radioMode) {
        this.radioMode = radioMode;

        this.cursorIndex = -1;
        this.currentStationIndex = -1;
        this.scrollingToIndex = -1;
        this.nextScrollIndex = -1;
        this.currentScrollIndex = 0;

        this.waitForIdleScroll = false;

        if (radioMode != RadioDevice.Values.STREAM_MODE_FM && isDeleteMode()) {
            closeDeleteMode();
        }

        int lastSize = getItemCount();
        stationList = newStationList.clone();

        if (stationList.length < lastSize) {
            notifyItemRangeRemoved(stationList.length, lastSize - stationList.length);
        }

        notifyItemRangeChanged(0, stationList.length);
    }

    public int getCursorIndex() {
        return cursorIndex;
    }

    public RadioStation[] getStationList() {
        return stationList;
    }

    private void startNextScroll() {
        if (scrollingToIndex == nextScrollIndex) {
            return;
        }

        recyclerView.stopScroll();
        final int nextScroll = nextScrollIndex;

        if (nextScroll < 0) {
            return;
        }

        nextScrollIndex = -1;

        // As the next item is not on screen, we are going to scroll directly to that page
        // and then do a smooth scroll after we have reached near that items
        if ((layoutManager.findFirstVisibleItemPosition() > nextScroll || layoutManager.findLastVisibleItemPosition() < nextScroll))  {
            scrollingToIndex = nextScroll;

            // Setup a listener to scroll when this is done
            waitForIdleScroll = true;
            if (nextScrollIndex == -1) {
                nextScrollIndex = nextScroll;
            }

            // Perform a jump scroll
            recyclerView.scrollToPosition(nextScrollIndex);
        } else {
            scrollingToIndex = nextScroll;
            recyclerView.smoothScrollToPosition(nextScroll);
            // In case it was set to something else, change it back now
            layoutManager.setSnapDuration(StationListLayoutManager.DEFAULT_SNAP_SPEED);
        }
    }

    public int getCurrentScrollIndex() {
        return currentScrollIndex;
    }

    public int getScrollingToIndex() {
        return scrollingToIndex;
    }

    private void scrollWhenPossible(int itemIndex) {
        waitForIdleScroll = false;
        nextScrollIndex = itemIndex;

        // If we are not scrolling start the next scroll now
        if (recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
            startNextScroll();
        }
    }

    public void onCursorPositionChanged(int newCursorIndex) {
        if (newCursorIndex == -1 || newCursorIndex == cursorIndex) {
            return;
        }

        notifyItemChanged(cursorIndex);
        notifyItemChanged(newCursorIndex);

        cursorIndex = newCursorIndex;

        layoutManager.setSnapDuration(1);
        scrollWhenPossible(cursorIndex);
    }


    public void onCurrentStationChanged(int newStationIndex) {
        if (currentStationIndex == newStationIndex) {
            return;
        }

        int lastCursorIndex = cursorIndex;
        int lastStationIndex = currentStationIndex;

        cursorIndex = newStationIndex;
        currentStationIndex = newStationIndex;


        if (lastCursorIndex > -1 && cursorIndex != lastCursorIndex) {
            notifyItemChanged(lastCursorIndex);
        }


        if (lastStationIndex > -1  &&
                lastCursorIndex != lastStationIndex) {
            notifyItemChanged(lastStationIndex);
        }

        if (currentStationIndex > -1) {
            notifyItemChanged(currentStationIndex);
            scrollWhenPossible(currentStationIndex);
        }
    }
}
