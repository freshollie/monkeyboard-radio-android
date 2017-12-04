package com.freshollie.monkeyboard.keystoneradio.radio.util.mot;

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

        // This seems to be correct
        repetitionCount = MOTObjectsManager.intFromBitsRange(data[0], 5, 3);

        // This is the correct method, but this data appears to be useless
        size = RadioDevice.getIntFromBytes(new byte[] {data[0], data[1]}) & ~(1 << 13) & ~(1 << 14) & ~(1 << 15) & 0xFFFF;

        this.data = Arrays.copyOfRange(data, 9, data.length);
    }
}