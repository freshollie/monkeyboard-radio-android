package com.freshollie.monkeyboarddabradio.radio;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Freshollie on 12/01/2017.
 * Used to store information about a radio station
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

    public int getChannelId() {
        return channelNumber;
    }

    public int getGenreId() {
        return genre;
    }

    public String getEnsemble() {
        return ensemble;
    }

    public String toJsonString() {
        try {
            return new JSONObject()
                    .put("name", getName())
                    .put("channelNumber", getChannelId())
                    .put("genreId", getGenreId())
                    .put("ensemble", getEnsemble())
                    .toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "{}";
        }
    }
}
