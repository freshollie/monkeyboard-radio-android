package com.freshollie.monkeyboarddabradio.ui;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.nshmura.snappysmoothscroller.LinearLayoutScrollVectorDetector;
import com.nshmura.snappysmoothscroller.SnapType;
import com.nshmura.snappysmoothscroller.SnappySmoothScroller;

/**
 * Created by Freshollie on 09/02/2017.
 */

public class StationListLayoutManager extends LinearLayoutManager {
    private static final float MILLISECONDS_PER_INCH = 50f;
    private int snapDuration = 150;

    public void setSnapDuration(int duration) {
        snapDuration = duration;
    }

    public StationListLayoutManager(Context context) {
        super(context);
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView,
                                       RecyclerView.State state, final int position) {

        SnappySmoothScroller scroller = new SnappySmoothScroller.Builder()
                .setPosition(position)
                .setSnapType(SnapType.CENTER)
                .setSnapDuration(snapDuration)
                .setScrollVectorDetector(new LinearLayoutScrollVectorDetector(this))
                .build(recyclerView.getContext());

        startSmoothScroll(scroller);
    }
}