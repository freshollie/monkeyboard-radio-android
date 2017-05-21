package com.freshollie.monkeyboard.keystoneradio.radio;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Freshollie on 12/01/2017.
 * Used to store information about a radio station
 */

public class RadioStation {

    private class JsonKeys {
        static final String name = "name";
        static final String channelFrequency = "channelFrequency";
        static final String genreId = "genreId";
        static final String ensemble = "ensemble";
    }

    private String name;
    private int channelFrequency;
    private int genre;
    private String ensemble;

    public RadioStation(JSONObject stationJson) throws JSONException {
        this(
                stationJson.getString(JsonKeys.name),
                stationJson.getInt(JsonKeys.channelFrequency),
                stationJson.getInt(JsonKeys.genreId),
                stationJson.getString(JsonKeys.ensemble)
        );
    }

    public RadioStation(String stationName, int channelFreq, int stationGenre, String stationEnsemble) {
        name = stationName;
        channelFrequency = channelFreq;
        genre = stationGenre;
        ensemble = stationEnsemble;
    }

    public String getName() {
        return name;
    }

    public int getChannelFrequency() {
        return channelFrequency;
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
                    .put(JsonKeys.name, getName())
                    .put(JsonKeys.channelFrequency, getChannelFrequency())
                    .put(JsonKeys.genreId, getGenreId())
                    .put(JsonKeys.ensemble, getEnsemble())
                    .toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "{}";
        }
    }
}
