package com.freshollie.monkeyboard.keystoneradio.radio.util;

import android.content.Context;

import com.freshollie.monkeyboard.keystoneradio.R;

/**
 * Created by freshollie on 29.11.17.
 */

public class RadioTextResources {
    private String[] genres = new String[]{};
    private String[] stereoModes = new String[]{};
    private String[] playStatusValues = new String[]{};

    public RadioTextResources(Context context) {
        genres = context.getResources().getStringArray(R.array.STATION_GENRES);
        stereoModes = context.getResources().getStringArray(R.array.STEREO_AUDIO_NAMES);
        playStatusValues = context.getResources().getStringArray(R.array.PLAYSTATUS_VALUES);
    }

    public String getGenreFromId(int genreId) {
        if (genreId > genres.length - 1 || genreId < 0) {
            return "";
        } else {
            return genres[genreId];
        }
    }

    public String getStereoModeFromId(int stereoModeId) {
        if (stereoModeId > stereoModes.length - 1  || stereoModeId < 0) {
            return "Unknown";
        } else {
            return stereoModes[stereoModeId];
        }
    }

    public String getPlayStatusFromId(int playStatusId) {
        if (playStatusId > playStatusValues.length - 1  || playStatusId < 0) {
            return "N/A";
        } else {
            return playStatusValues[playStatusId];
        }
    }
}
