package com.freshollie.monkeyboarddabradio.radio;

/**
 * Created by Freshollie on 12/01/2017.
 */

public class RadioStation {

    private String name;
    private int channelNumber;
    private int genre;
    private String ensemble;

    public RadioStation(String stationName, int channelNum, int stationGenre, String stationEnsemble) {
        name = stationName;
        channelNumber = channelNum;
        genre = stationGenre;
        ensemble = stationEnsemble;
    }

    public String getName() {
        return name;
    }

    public int getChannelNumber() {
        return channelNumber;
    }

    public int getGenre() {
        return genre;
    }

    public String getEnsemble() {
        return ensemble;
    }
}
