package com.freshollie.monkeyboarddabradio;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.freshollie.monkeyboarddabradio.radio.DeviceConnection;
import com.freshollie.monkeyboarddabradio.radio.ListenerManager;
import com.freshollie.monkeyboarddabradio.radio.RadioDevice;
import com.freshollie.monkeyboarddabradio.radio.RadioStation;

public class PlayerActivity extends AppCompatActivity {
    private String TAG = this.getClass().getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        final RadioDevice radio = new RadioDevice(getApplicationContext());

        radio.getListenerManager().registerConnectionStateChangedListener(
                new ListenerManager.ConnectionStateChangeListener() {
                    @Override
                    public void onStart() {
                        Log.v(TAG, "Programs: " + String.valueOf(radio.getTotalPrograms()));
                        radio.refreshStationList();
                        for (RadioStation station: radio.getStationList()) {
                            Log.v(TAG, String.valueOf(station.getChannelNumber()) + ": " + station.getName());
                        }
                        radio.play(50);
                        radio.setVolume(1);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while (true) {
                                    Log.v(TAG, "Play state: " + radio.getPlayStatus());
                                    Log.v(TAG, "Signal Strength "+ radio.getSignalQuality());
                                    Log.v(TAG, "Program Text: " + radio.getProgramText());
                                    try {
                                        Thread.sleep(0);
                                    } catch (InterruptedException e){
                                        break;
                                    }
                                }
                            }
                        }).start();
                    }

                    @Override
                    public void onStop() {
                        Log.v(TAG, "Radio Connection Closed");

                    }
                });
        radio.connect();
    }
}
