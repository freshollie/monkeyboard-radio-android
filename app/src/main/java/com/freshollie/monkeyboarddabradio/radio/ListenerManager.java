package com.freshollie.monkeyboarddabradio.radio;

import android.content.Context;

import java.util.ArrayList;

/**
 * Created by Freshollie on 12/01/2017.
 */

public class ListenerManager {

    /**
     * Listener used to notify when the Radio Device connection has started
     */

    public interface ConnectionStateChangeListener {
        void onStart();
        void onStop();
    }

    private ArrayList<ConnectionStateChangeListener> connectionStateChangeListeners;

    public ListenerManager() {
        unregisterAll();
    }

    public void onConnectionStop() {
        for (ConnectionStateChangeListener listener : connectionStateChangeListeners) {
            listener.onStop();
        }
    }

    public void onConnectionStart() {
        for (ConnectionStateChangeListener listener : connectionStateChangeListeners) {
            listener.onStart();
        }
    }

    public void registerConnectionStateChangedListener(ConnectionStateChangeListener listener) {
        connectionStateChangeListeners.add(listener);
    }

    public void unregisterConnectionStateChangedListener(ConnectionStateChangeListener listener) {
        connectionStateChangeListeners.remove(listener);
    }
    private void unregisterAll() {
        connectionStateChangeListeners = new ArrayList<>();
    }
}
