package com.freshollie.monkeyboard.keystoneradio.radio.mot;

import android.util.Log;

import com.freshollie.monkeyboard.keystoneradio.radio.RadioDevice;

import java.util.Arrays;

/**
 * Created by freshollie on 09.11.17.
 */

public class Segment {
    public final int id;
    public final boolean isFinal;
    public final byte[] data;

    public final int repetitionCount;
    public final int size;

    public Segment(int id, boolean isFinal, byte[] data) {
        this.id = id;
        this.isFinal = isFinal;

        repetitionCount = MOTObjectsManager.intFromBitsRange(data[0], 5, 3);

        size = RadioDevice.getIntFromBytes(new byte[] {data[0], data[1]}) & ~(1 << 13) & ~(1 << 14) & ~(1 << 15) & 0xFFFF;

        this.data = Arrays.copyOfRange(data, 2, data.length);

        Log.i("SEGMENT", "Built. Size found: " + String.valueOf(size) + " actual size " + String.valueOf(this.data.length));
    }
}