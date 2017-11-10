package com.freshollie.monkeyboard.keystoneradio.radio.mot;

/**
 * Created by freshollie on 09.11.17.
 */

public class MOTObjectBody extends SegmentedObject {
    public void addSegment(Segment segment) {
        super.addSegment(segment);

        if (hasAllSegments()) {
            buildFromSegments();
        }
    }

    private void buildFromSegments() {
        byte[] bytes = super.getOrderedData();
    }

    public boolean isComplete() {
        return false;
    }
}
