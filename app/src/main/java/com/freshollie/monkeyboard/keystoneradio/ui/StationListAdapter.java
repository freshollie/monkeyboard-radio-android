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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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

    private int currentScrollIndex = 0;

    private StationListLayoutManager layoutManager;

    private int targetScrollIndex = -1;

    private Runnable scrollRunnable = null;

    private int nextScrollIndex = -1;
    private int lastScrollIndex = -1;

    private boolean waitForIdleScroll;

    private boolean firstScrollDone = false;

    private boolean readyForScroll = false;

    private ViewTreeObserver.OnGlobalLayoutListener
            recyclerViewLayoutDoneListener =
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        onLayoutReady();
                    }
            };

    private OnScrollListener onScrollListener = new OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                if (waitForIdleScroll &&
                        nextScrollIndex != -1
                        ) {
                    startNextScroll(nextScrollIndex);
                }
                currentScrollIndex = lastScrollIndex;
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (nextScrollIndex != -1) {
                recyclerView.stopScroll();
                startNextScroll(nextScrollIndex);
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
        this.recyclerView
                .getViewTreeObserver()
                .addOnGlobalLayoutListener(recyclerViewLayoutDoneListener);
        layoutManager = (StationListLayoutManager) recyclerView.getLayoutManager();
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.recyclerView.removeOnScrollListener(onScrollListener);
        this.recyclerView
                .getViewTreeObserver()
                .removeOnGlobalLayoutListener(recyclerViewLayoutDoneListener);
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

        if (radioMode == RadioDevice.Values.STREAM_MODE_FM) {
            stationCard.stationCardLayout.setOnLongClickListener(new View.OnLongClickListener() {
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
            stationCard.stationCardLayout.setOnClickListener(null);
            stationCard.stationRemoveButton.setVisibility(View.VISIBLE);
            stationCard.stationRemoveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    playerActivity.handleRemoveFmChannel(radioStation);
                    onStationRemoved(stationCard.getAdapterPosition());
                }
            });
        } else {
            stationCard.stationCardLayout.setOnClickListener(new View.OnClickListener() {
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


    private void onLayoutReady() {
        // When the layout has been rendered check if we need to start scrolling
        if (!readyForScroll && nextScrollIndex != -1) {
            recyclerView.post(new Runnable() {
                @Override
                public void run() {
                    startNextScroll(nextScrollIndex);
                }
            });
        }
        readyForScroll = true;
    }

    @Override
    public int getItemCount() {
        return stationList.length;
    }

    public void initialiseNewStationList(RadioStation[] newStationList, int radioMode) {
        this.radioMode = radioMode;

        this.cursorIndex = -1;
        this.currentStationIndex = -1;
        this.lastStationIndex = -1;
        this.lastScrollIndex = -1;
        this.nextScrollIndex = -1;
        this.currentScrollIndex = 0;

        this.readyForScroll = false;
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

    private void startNextScroll(final int nextScroll) {

        if (nextScroll < 0) {
            return;
        }

        if (nextScroll == nextScrollIndex) {
            nextScrollIndex = -1;
        }

        int delay = 0;
        // If the recycler view has just been initialised then we should wait 50 ms,
        // I would rather this was more specific
        if (lastScrollIndex == -1 && firstScrollDone) {
            delay = 300;
        }

        firstScrollDone = true;

        // If we have to move more than 20 items then we first need to probably
        // scroll directly to there
        // and then wait for the scroll to finish before starting a smooth scroll from that point
        if ((layoutManager.findFirstVisibleItemPosition() > nextScroll || layoutManager.findLastVisibleItemPosition() < nextScroll) && !waitForIdleScroll)  {
            recyclerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    lastScrollIndex = nextScroll;

                    // Tell the scroll listener to perform this again
                    // Once we have finished this scroll
                    if (nextScrollIndex == -1) {
                        nextScrollIndex = nextScroll;
                    }
                    waitForIdleScroll = true;

                    recyclerView.scrollToPosition(nextScrollIndex);
                }
            }, delay);
        } else {
            waitForIdleScroll = false;
            recyclerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    lastScrollIndex = nextScroll;
                    recyclerView.smoothScrollToPosition(nextScroll);
                    // Incase it was set to something else, change it back now
                    layoutManager.setSnapDuration(StationListLayoutManager.DEFAULT_SNAP_SPEED);
                }
            }, delay);
        }
    }

    public int getCurrentScrollIndex() {
        return currentScrollIndex;
    }

    public int getLastScrollIndex() {
        return lastScrollIndex;
    }

    private void scrollWhenPossible(int itemIndex) {
        waitForIdleScroll = false;
        if (nextScrollIndex != -1 || !readyForScroll) {
            // The scroll or recyclerview  is currently busy
            // So queue it
            nextScrollIndex = itemIndex;
        } else {
            // Otherwise perform the action right now
            startNextScroll(itemIndex);
        }
    }

    public void onCursorPositionChanged(int newCursorIndex) {
        if (newCursorIndex == -1 || newCursorIndex == cursorIndex) {
            return;
        }

        int lastChannelCursor = cursorIndex;
        cursorIndex = newCursorIndex;

        notifyItemChanged(lastChannelCursor);
        notifyItemChanged(cursorIndex);
        recyclerView.stopScroll();

        layoutManager.setSnapDuration(1);
        scrollWhenPossible(cursorIndex);

    }

    public void onCurrentStationChanged(int newStationIndex) {
        // We are already scrolling to here so ignore
        if (newStationIndex == currentStationIndex) {
            return;
        }

        int lastChannelCursor = cursorIndex;
        cursorIndex = newStationIndex;
        lastStationIndex = currentStationIndex;
        currentStationIndex = newStationIndex;

        if (lastChannelCursor > -1) {
            notifyItemChanged(lastChannelCursor);
        }


        if (lastStationIndex > -1) {
            notifyItemChanged(lastStationIndex);
        }

        if (currentStationIndex > -1) {
            notifyItemChanged(currentStationIndex);
            scrollWhenPossible(currentStationIndex);
        }
    }
}
