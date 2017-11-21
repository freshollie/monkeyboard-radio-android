/*
 * Created by Oliver Bell on 13/02/17
 * Copyright (c) 2017. by Oliver bell <freshollie@gmail.com>
 *
 * Last modified 14/06/17 23:47
 */

package com.freshollie.monkeyboard.keystoneradio.ui;


import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;

import com.freshollie.monkeyboard.keystoneradio.R;
import com.freshollie.monkeyboard.keystoneradio.playback.RadioPlayerService;

/**
 * Settings fragment displays all of the settings for the player and allows the user to edit those
 * preferences.
 */
public class SettingsFragment extends PreferenceFragment {
    private String TAG = this.getClass().getSimpleName();
    private SharedPreferences sharedPreferences;

    private RadioPlayerService playerService;
    private boolean playerBound;

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to Player, cast the IBinder and get RadioPlayerService instance
            RadioPlayerService.RadioPlayerBinder binder = (RadioPlayerService.RadioPlayerBinder) service;
            playerService = binder.getService();
            playerBound = true;
            Log.v(TAG, "Player bound");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            playerBound = false;
            playerService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        setupPreferences();

        Intent radioPlayerIntent = new Intent(getActivity(), RadioPlayerService.class);
        getActivity().startService(radioPlayerIntent);
        getActivity().bindService(radioPlayerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void performDABSearch() {
        if (playerBound) {
            Log.v(TAG, "Starting radio dialog");
            FragmentManager fragmentManager = getActivity().getFragmentManager();

            RadioStatusDialog dialog = new RadioStatusDialog();
            dialog.setPlayerService(playerService);
            dialog.show(fragmentManager, "RadioStatusDialog");
        }

    }

    public void confirm(String message, DialogInterface.OnClickListener yesButton) {
        new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(getString(R.string.no), null)
                .setNegativeButton(getString(R.string.yes), yesButton)
                .show();
    }

    public void showModeAlert() {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.mode_alert_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    public void inform(String message) {
        Snackbar.make(getActivity().findViewById(R.id.content_settings), message, Snackbar.LENGTH_SHORT)
                .setAction("Dismiss", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                })
                .show();
    }

    public void setupPreferences() {
        // Search button

        findPreference(getString(R.string.setting_perform_dab_search_button_key))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        confirm(
                                getString(R.string.setting_perform_dab_search_confirmation),

                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        performDABSearch();
                                    }

                                });
                        return true;
                    }
                });

        findPreference(getString(R.string.pref_clear_fm_stations_key))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        confirm(
                                "Are you sure you want to clear all the saved FM stations?",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        if (playerService != null) {
                                            playerService.handleClearFmRadioStations();
                                        }
                                    }
                                });
                        return true;
                    }
                });
        final CheckBoxPreference dabEnabledPreference =
                (CheckBoxPreference) findPreference(getString(R.string.pref_dab_mode_enabled_key));

        final CheckBoxPreference fmEnabledPreference =
                (CheckBoxPreference) findPreference(getString(R.string.pref_fm_mode_enabled_key));

        final CheckBoxPreference fmStereoModeEnabledPreference =
                (CheckBoxPreference) findPreference(getString(R.string.pref_fm_stereo_enabled_key));


        dabEnabledPreference.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (!fmEnabledPreference.isChecked() && !((boolean) o)) {
                    showModeAlert();
                    return false;
                }
                return true;
            }
        });

        fmEnabledPreference.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (!dabEnabledPreference.isChecked() && !((boolean) o)) {
                    showModeAlert();
                    return false;
                }
                return true;
            }
        });

        fmStereoModeEnabledPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                boolean enabled = fmStereoModeEnabledPreference.isChecked();
                // Edit the preference ourselves,
                // so it is up to date ready for the service to handle
                sharedPreferences
                        .edit()
                        .putBoolean(getString(R.string.pref_fm_stereo_enabled_key), enabled)
                        .apply();

                // Tell the player that the value has been changed
                if (playerBound) {
                    playerService.handleUpdateBoardStereoMode();
                }

                return false;
            }
        });

        // Play on open checkbox
        CheckBoxPreference playInputPreference =
                (CheckBoxPreference) findPreference(getString(R.string.setting_play_on_open));

        playInputPreference.setChecked(
                sharedPreferences.getBoolean(getString(R.string.PREF_PLAY_ON_OPEN), false)
        );

        playInputPreference.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(getString(R.string.PREF_PLAY_ON_OPEN), (boolean) o);
                        editor.apply();
                        return true;
                    }
                }
        );

        // Input mode checkbox
        CheckBoxPreference headunitInputPreference =
                (CheckBoxPreference) findPreference(getString(R.string.setting_headunitcontroller_input));

        headunitInputPreference.setChecked(
                sharedPreferences.getBoolean(getString(R.string.PREF_HEADUNIT_CONTROLLER_INPUT), false)
        );

        headunitInputPreference.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                      @Override
                      public boolean onPreferenceChange(Preference preference, Object o) {
                          SharedPreferences.Editor editor = sharedPreferences.edit();
                          editor.putBoolean(getString(R.string.PREF_HEADUNIT_CONTROLLER_INPUT), (boolean) o);
                          editor.apply();
                          return true;
                      }
                }
        );

        // Scroll wrap checkbox
        CheckBoxPreference scrollWrapPreference =
                (CheckBoxPreference) findPreference(getString(R.string.setting_scroll_wrap));

        scrollWrapPreference.setChecked(
                sharedPreferences.getBoolean(getString(R.string.PREF_CURSOR_SCROLL_WRAP), false)
        );

        scrollWrapPreference.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(getString(R.string.PREF_CURSOR_SCROLL_WRAP), (boolean) o);
                        editor.apply();
                        return true;
                    }
                }
        );

        // Duck Volume preference list

        CharSequence[] entrySequence = new CharSequence[17];
        for (int i = 0; i < 17; i++) {
            entrySequence[i] = String.valueOf(i);
        }

        ListPreference duckVolumePreference = (ListPreference)
                findPreference(getString(R.string.setting_duck_volume));

        duckVolumePreference.setEntries(entrySequence);
        duckVolumePreference.setEntryValues(entrySequence);

        duckVolumePreference.setValueIndex(
                sharedPreferences.getInt(getString(R.string.DUCK_VOLUME_KEY), 3)
        );

        duckVolumePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(getString(R.string.DUCK_VOLUME_KEY), Integer.valueOf((String) o));
                editor.apply();
                return true;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (playerBound) {
            getActivity().unbindService(serviceConnection);
        }
    }
}

