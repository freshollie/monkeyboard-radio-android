package com.freshollie.monkeyboarddabradio.radio;

import java.util.ArrayList;

/**
 * Created by Freshollie on 12/01/2017.
 */

public class ListenerManager {

    private ArrayList<DeviceConnection.ConnectionStateListener> connectionStateListeners;

    public ListenerManager() {
        unregisterAll();
    }

    public void onConnectionStop() {
        for (DeviceConnection.ConnectionStateListener listener : connectionStateListeners) {
            listener.onStop();
        }
    }

    public void onConnectionStart() {
        for (DeviceConnection.ConnectionStateListener listener : connectionStateListeners) {
            listener.onStart();
        }
    }

    public void registerConnectionStateListener
            (DeviceConnection.ConnectionStateListener listener) {
        connectionStateListeners.add(listener);
    }

    public void unregisterConnectionStateListener
            (DeviceConnection.ConnectionStateListener listener) {
        connectionStateListeners.remove(listener);
    }
    private void unregisterAll() {
        connectionStateListeners = new ArrayList<>();
    }
}
