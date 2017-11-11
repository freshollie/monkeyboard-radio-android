package com.freshollie.monkeyboard.keystoneradio.radio.mot;

import android.util.Log;
import android.util.SparseArray;

import java.util.Arrays;

/**
 * Created by freshollie on 09.11.17.
 */

public class SegmentedObject {
    private static final String TAG = SegmentedObject.class.getSimpleName();
    int finalSegmentId = -2;

    SparseArray<Segment> segments = new SparseArray<>();

    public boolean hasAllSegments() {
        return segments.size() == finalSegmentId + 1;
    }

    public byte[] getOrderedData() {
        byte[] bytes = new byte[0];

        if (!hasAllSegments()) {
            return bytes;
        }

        for (int i = 0; i < finalSegmentId + 1; i++) {
            if (MOTObjectsManager.DEBUG) Log.d(TAG,"Assembling segment data: " + Arrays.toString(segments.get(i).data));
            bytes = MOTObjectsManager.concatBytes(bytes, segments.get(i).data);
        }

        return bytes;
    }

    public boolean isComplete() {
        return hasAllSegments();
    }

    public void addSegment(Segment segment) {
        segments.put(segment.id, segment);
        if (segment.isFinal) {
            finalSegmentId = segment.id;
        }

        if (hasAllSegments() && MOTObjectsManager.DEBUG) {
            Log.i(TAG, "Segmented Object Complete");
        }
    }
}
