/*
 * Created by Oliver Bell on 08/02/2017
 * Copyright (c) 2017. by Oliver bell <freshollie@gmail.com>
 *
 * Last modified 14/06/17 23:42
 */

package com.freshollie.monkeyboard.keystoneradio.ui;

import android.graphics.Color;
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
import java.util.List;

/**
 * Station list adapter is used to display the radio stations in a recycler view. It features
 * functionality for the user to delete FM stations, scrolling through stations and highlighting
 * the currently playing station
 */

public class StationListAdapter extends RecyclerView.Adapter<StationListAdapter.StationCard> {
    private static final String TAG = StationListAdapter.class.getSimpleName();

    private RadioStation[] stationList = new RadioStation[0];

    private PlayerActivity playerActivity;

    private int cursorIndex = 0;
    private int currentStationIndex = 0;
    private boolean deleteMode;
    private RecyclerView recyclerView;
    private int radioMode;

    private int currentScrollIndex = 0;

    private StationListLayoutManager layoutManager;

    private int nextScrollIndex = -1;
    private int scrollingToIndex = -1;

    private boolean waitForIdleScroll = false;

    private static String SELECTION_CHANGED_EVENT = "selection_changed";
    private static String CURSOR_CHANGED_EVENT = "cursor_changed";

    private int SELECTED_BACKGROUND_COLOR;
    private int HIGHLIGHTED_BACKGROUND_COLOR;
    private int REGULAR_CARD_COLOR;
    private final int DELETE_MODE_BACKGROUND_COLOR;

    private OnScrollListener onScrollListener = new OnScrollListener() {

        private void checkScrollsQueue() {
            if (recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE && scrollingToIndex != -1) {
                currentScrollIndex = scrollingToIndex;
            }

            if (waitForIdleScroll && recyclerView.getScrollState() != RecyclerView.SCROLL_STATE_IDLE) {
                return;
            }

            scrollingToIndex = -1;
            if (nextScrollIndex != -1) {
                startNextScroll();
            }
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                checkScrollsQueue();
            }

        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            checkScrollsQueue();
        }
    };
    private int currentlyHighlightedIndex;

    public static class StationCard extends RecyclerView.ViewHolder {
        TextView stationName;
        TextView stationGenre;
        TextView stationEnsemble;
        CardView stationCardLayout;
        View stationRemoveButton;
        int cardColour;

        StationCard(View v) {
            super(v);
            stationName = (TextView) v.findViewById(R.id.station_name_card_text);
            stationGenre = (TextView) v.findViewById(R.id.station_genre_card_text);
            stationEnsemble = (TextView) v.findViewById(R.id.station_ensemble_name_card_text);
            stationCardLayout = (CardView) v.findViewById(R.id.station_item_layout);
            stationRemoveButton = v.findViewById(R.id.station_remove_button);
        }
    }

    private int addAlpha(int color, int alpha) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        return Color.argb(alpha, red, green, blue);
    }

    public StationListAdapter(PlayerActivity playerActivity) {
        this.playerActivity = playerActivity;
        setHasStableIds(true);

        SELECTED_BACKGROUND_COLOR = ContextCompat
                .getColor(playerActivity, R.color.colorPrimaryDark);

        HIGHLIGHTED_BACKGROUND_COLOR = addAlpha(ContextCompat
                .getColor(playerActivity, R.color.colorAccent), 80);

        REGULAR_CARD_COLOR = ContextCompat
                .getColor(playerActivity, R.color.backgroundGrey);

        DELETE_MODE_BACKGROUND_COLOR = addAlpha(ContextCompat
                .getColor(playerActivity, R.color.colorHighlight), 80);
    }

    // Create a new station card for the station list
    @Override
    public StationCard onCreateViewHolder(ViewGroup parent, int viewType) {
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

    private void colorCard(StationCard stationCard, int position) {
        int newColor;

        if (currentlyHighlightedIndex == position) {
            currentlyHighlightedIndex = -1;
        }
        if (deleteMode) {
            newColor = DELETE_MODE_BACKGROUND_COLOR;

        } else if (position == currentStationIndex) {
            newColor = SELECTED_BACKGROUND_COLOR;

        } else if (position == cursorIndex) {
            newColor = HIGHLIGHTED_BACKGROUND_COLOR;

            currentlyHighlightedIndex = position;

        } else {
            newColor = REGULAR_CARD_COLOR;

        }

        if (stationCard.cardColour != newColor) {
            stationCard.stationCardLayout.setCardBackgroundColor(newColor);
            stationCard.cardColour = newColor;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onBindViewHolder(final StationCard stationCard, final int position, List<Object> payloads) {
        if(!payloads.isEmpty()) {
            if (payloads.get(payloads.size() - 1) instanceof String) {
                colorCard(stationCard, position);
                return;
            }
        }
        onBindViewHolder(stationCard, position);
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

        colorCard(stationCard, position);

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
            recyclerView.getItemAnimator().setChangeDuration(10);
            recyclerView.getItemAnimator().setRemoveDuration(0);
            recyclerView.getItemAnimator().setMoveDuration(100);
            recyclerView.getItemAnimator().setAddDuration(100);
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
        this.currentlyHighlightedIndex = -1;

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

        final int nextScroll = nextScrollIndex;

        if (nextScroll < 0) {
            return;
        }

        nextScrollIndex = -1;
        recyclerView.stopScroll();
        
        // As the next item is not on screen, we are going to scroll directly to that page
        // and then do a smooth scroll after we have reached near that items
        if ((layoutManager.findFirstVisibleItemPosition() > nextScroll || layoutManager.findLastVisibleItemPosition() < nextScroll))  {
            // Setup a listener to scroll when this is done
            waitForIdleScroll = true;
            if (nextScrollIndex == -1) {
                nextScrollIndex = nextScroll;
            }

            scrollingToIndex = nextScroll;

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

    public void onCursorPositionChanged(final int newCursorIndex) {
        if (newCursorIndex == -1 || newCursorIndex == cursorIndex) {
            return;
        }
        cursorIndex = newCursorIndex;

        // Let the view know that the last highlighted index has changed, if we have one
        if (currentlyHighlightedIndex != -1) {
            if (layoutManager.findFirstVisibleItemPosition() <= currentlyHighlightedIndex &&
                    layoutManager.findLastVisibleItemPosition() >= currentlyHighlightedIndex) {
                notifyItemChanged(currentlyHighlightedIndex, CURSOR_CHANGED_EVENT);
            }
        }

        // Only notify if we can actually see the card on the screen
        // otherwise its going to be updated automatically anyway
        if (layoutManager.findFirstVisibleItemPosition() <= cursorIndex &&
                layoutManager.findLastVisibleItemPosition() >= cursorIndex) {
            notifyItemChanged(cursorIndex, CURSOR_CHANGED_EVENT);
        }

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
            notifyItemChanged(lastCursorIndex, SELECTION_CHANGED_EVENT);
        }


        if (lastStationIndex > -1  &&
                lastCursorIndex != lastStationIndex) {
            notifyItemChanged(lastStationIndex, SELECTION_CHANGED_EVENT);
        }

        if (currentStationIndex > -1) {
            notifyItemChanged(currentStationIndex, SELECTION_CHANGED_EVENT);
            scrollWhenPossible(currentStationIndex);
        }
    }
}
