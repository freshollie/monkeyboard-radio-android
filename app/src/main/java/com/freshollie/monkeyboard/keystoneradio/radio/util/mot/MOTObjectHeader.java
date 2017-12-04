package com.freshollie.monkeyboard.keystoneradio.radio.util.mot;

/**
 * Created by freshollie on 09.11.17.
 */

public class MOTObjectHeader extends SegmentedObject {
    private static final String TAG = MOTObjectHeader.class.getSimpleName();

    boolean complete = false;

    public void addSegment(Segment segment) {
        super.addSegment(segment);

        if (hasAllSegments()) {
            buildFromSegments();
        }
    }

    public boolean isComplete() {
        return complete;
    }

    private void buildFromSegments() {
        if (hasAllSegments()) {
            complete = true;
        }

        // todo: Parse header data for fields and populate header fields
        // Currently this is not required for images


    }
}
