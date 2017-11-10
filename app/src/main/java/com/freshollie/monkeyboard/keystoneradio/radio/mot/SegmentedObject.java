package com.freshollie.monkeyboard.keystoneradio.radio.mot;

import android.util.SparseArray;

/**
 * Created by freshollie on 09.11.17.
 */

public class SegmentedObject {
    int finalSegmentId = -2;

    SparseArray<Segment> segments = new SparseArray<>();

    public boolean hasAllSegments() {
        return segments.size() == finalSegmentId + 1;
    }

    public byte[] getOrderedData() {
        byte[] bytes = new byte[0];

        for (int i = 0; i < segments.size(); i++) {
            bytes = MOTObjectsManager.concatBytes(bytes, segments.get(i).data);
        }

        return bytes;
    }

    public void addSegment(Segment segment) {
        segments.put(segment.id, segment);
        if (segment.isFinal) {
            finalSegmentId = segment.id;
        }
    }
}
