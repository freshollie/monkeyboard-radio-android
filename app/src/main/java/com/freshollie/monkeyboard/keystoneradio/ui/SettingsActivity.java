/*
 * Created by Oliver Bell on 13/02/17
 * Copyright (c) 2017. by Oliver bell <freshollie@gmail.com>
 *
 * Last modified 02/05/17 13:11
 */

package com.freshollie.monkeyboard.keystoneradio.ui;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.freshollie.monkeyboard.keystoneradio.R;

/**
 * Settings activity contains and displays the settings fragment and sets the status bar
 * colours if android version is high enough
 */

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.settings_title));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(R.id.content_settings, new SettingsFragment())
                .commit();
    }
}
