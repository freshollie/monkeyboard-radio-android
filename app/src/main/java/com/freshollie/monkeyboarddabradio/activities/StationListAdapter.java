package com.freshollie.monkeyboarddabradio.activities;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.freshollie.monkeyboarddabradio.R;
import com.freshollie.monkeyboarddabradio.radio.RadioDevice;
import com.freshollie.monkeyboarddabradio.radio.RadioStation;

/**
 * Created by Freshollie on 08/02/2017.
 */

public class StationListAdapter extends RecyclerView.Adapter<StationListAdapter.StationCard> {
    private RadioStation[] stationList = new RadioStation[0];
    private PlayerActivity playerActivity;
    private int cursorIndex = 0;
    private int currentStationIndex = 0;
    private int lastStationIndex = 0;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class StationCard extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView stationName;
        public TextView stationGenre;
        public TextView stationEnsemble;
        public View stationItemBackground;
        public RelativeLayout stationSelectionLayout;
        public View stationTopDivide;
        public View stationBottomDivide;

        public StationCard(View v) {
            super(v);
            stationName = (TextView) v.findViewById(R.id.station_name_card_text);
            stationGenre = (TextView) v.findViewById(R.id.station_genre_card_text);
            stationEnsemble = (TextView) v.findViewById(R.id.station_ensemble_name_card_text);
            stationItemBackground = v.findViewById(R.id.station_item_background);
            stationSelectionLayout = (RelativeLayout) v.findViewById(R.id.station_item_layout);
            stationTopDivide = v.findViewById(R.id.top_divide);
            stationBottomDivide = v.findViewById(R.id.bottom_divide);
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
    public void onBindViewHolder(final StationCard stationCard, int position) {
        RadioStation radioStation = stationList[position];

        stationCard.stationName.setText(radioStation.getName());
        stationCard.stationEnsemble.setText(radioStation.getEnsemble());
        stationCard.stationGenre.setText(
                RadioDevice.StringValues.getGenreFromId(radioStation.getGenreId()
                )
        );

        stationCard.stationSelectionLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playerActivity.handleSetChannel(stationCard.getAdapterPosition());
            }
        });

        stationCard.stationItemBackground.setAlpha(1f);
        if (position == currentStationIndex) {
            stationCard.stationItemBackground.setBackgroundColor(
                    stationCard.stationItemBackground.getResources()
                            .getColor(R.color.colorPrimaryDark, null)
            );
        }

        if (position == cursorIndex) {
            stationCard.stationBottomDivide.setBackgroundColor(
                    stationCard.stationItemBackground.getResources()
                            .getColor(R.color.colorPrimaryDark, null)
            );

            stationCard.stationTopDivide.setBackgroundColor(
                    stationCard.stationItemBackground.getResources()
                            .getColor(R.color.colorPrimaryDark, null)
            );

            if (position != currentStationIndex) {
                stationCard.stationItemBackground.setBackgroundColor(
                        stationCard.stationItemBackground.getResources()
                                .getColor(R.color.colorAccent, null)
                );
                stationCard.stationItemBackground.setAlpha(0.3f);
            }
        } else {
            stationCard.stationBottomDivide.setBackgroundColor(
                    stationCard.stationItemBackground.getResources()
                            .getColor(R.color.backgroundDarker, null)
            );
            stationCard.stationTopDivide.setBackgroundColor(
                    stationCard.stationItemBackground.getResources()
                            .getColor(R.color.backgroundDarker, null)
            );
        }

        if (position != currentStationIndex && position != cursorIndex) {
            stationCard.stationItemBackground.setBackgroundColor(0);
        }
    }

    @Override
    public int getItemCount() {
        return stationList.length;
    }

    public void updateStationList(RadioStation[] newStationList) {
        stationList = newStationList.clone();
        notifyDataSetChanged();
    }

    public RadioStation[] getStationList() {
        return stationList;
    }


    public void setCursorIndex(int channelIndex) {
        Log.v("StationListAdapter", "Setting new cursorPosition " + String.valueOf(channelIndex));
        int lastChannel = cursorIndex;
        cursorIndex = channelIndex;
        notifyItemChanged(channelIndex);
        notifyItemChanged(lastChannel);
        refreshCurrentStation();
    }

    public void setCurrentStationIndex(int channelIndex) {
        currentStationIndex = channelIndex;
    }

    public int getCurrentStationIndex() {
        return currentStationIndex;
    }

    public void refreshCurrentStation() {
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
