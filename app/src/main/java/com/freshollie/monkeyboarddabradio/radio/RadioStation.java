package com.freshollie.monkeyboarddabradio.radio;

/**
 * Created by Freshollie on 12/01/2017.
 */

public class RadioStation {

    private String name;
    private int channelNumber;
    private String genre;

    public RadioStation(String stationName, int channelNum, String stationGenre) {
        name = stationName;
        channelNumber = channelNum;
        genre = stationGenre;
    }

    public String getName() {
        return name;
    }

    public int getChannelNumber() {
        return channelNumber;
    }

    public String getGenre() {
        return genre;
    }
}
