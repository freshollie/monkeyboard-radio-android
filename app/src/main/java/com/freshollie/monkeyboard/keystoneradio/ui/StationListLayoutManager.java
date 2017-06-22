/*
 * Created by Oliver Bell on 09/02/17
 * Copyright (c) 2017. by Oliver bell <freshollie@gmail.com>
 *
 * Last modified 02/05/17 13:11
 */

package com.freshollie.monkeyboard.keystoneradio.ui;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.nshmura.snappysmoothscroller.LinearLayoutScrollVectorDetector;
import com.nshmura.snappysmoothscroller.SnapType;
import com.nshmura.snappysmoothscroller.SnappySmoothScroller;

/**
 * Layoutmanager used to control the animation of the recyclerview stationlist
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