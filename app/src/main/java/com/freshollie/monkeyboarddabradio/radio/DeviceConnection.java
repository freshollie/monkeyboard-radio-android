package com.freshollie.monkeyboarddabradio.radio;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;

import java.util.Arrays;

/**
 * Created by Freshollie on 12/01/2017.
 *
 * Handles all connection interaction with the monkeyboard over the usb serial connection
 *
 */

public class DeviceConnection{

    private final String TAG = this.getClass().getSimpleName();

    public final String ACTION_USB_PERMISSION =
            "com.freshollie.monkeyboarddabradio.radio.deviceconnection.action.USB_PERMISSION";

    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private UsbDeviceConnection usbDeviceConnection;
    private UsbSerialDevice serialInterface;

    private ConnectionStateListener connectionStateListener;

    private Context context;

    private boolean connected = false;

    private class NotConnectedException extends Exception{
    }

    /**
     * Listener used to notify when the Radio Device connection has started
     */
    public interface ConnectionStateListener {
        void onStart();
        void onStop();
    }

    /**
     * This receiver is called when a usb device is detached and when a usb
     * permission is granted or denied.
     */

    private BroadcastReceiver usbBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "Intent Received: " + intent.getAction());

            switch (intent.getAction()) {

                case (ACTION_USB_PERMISSION):
                    synchronized (this) {
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (device != null) {
                                Log.v(TAG, "Permission for device granted");
                                requestConnection();
                            }
                        }
                    }
                    return;

                case (UsbManager.ACTION_USB_DEVICE_DETACHED):
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device != null) {
                        if (device.getVendorId() == RadioDevice.VENDOR_ID &&
                                device.getDeviceId() == RadioDevice.PRODUCT_ID) {
                            closeConnection();
                        }
                    }

            }
        }
    };

    private PendingIntent usbPermissionIntent;

    public DeviceConnection(Context appContext) {
        context = appContext;

        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        usbPermissionIntent =
                PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(usbBroadcastReceiver, filter);
    }

    public void requestConnection() {
        Log.v(TAG, "Requesting connection to device");

        for (UsbDevice device : usbManager.getDeviceList().values()) {
            Log.v(TAG, "Checking connected devices");
            Log.v(TAG, "ProductId: " + String.valueOf(device.getDeviceId()));
            Log.v(TAG, "VendorId: " + String.valueOf(device.getVendorId()));

            if (device.getDeviceId() == RadioDevice.PRODUCT_ID &&
                    device.getVendorId() == RadioDevice.VENDOR_ID) {
                Log.v(TAG, "Found device");
                if (usbManager.hasPermission(device)) {
                    usbDevice = device;
                    openConnection();
                } else {
                    Log.v(TAG, "Requesting permission for device");
                    usbManager.requestPermission(device, usbPermissionIntent);

                }
                return;
            }
        }

        Log.v(TAG, "No devices found");
    }

    public void openConnection() {
        Log.v(TAG, "Opening connection to device");
        usbDeviceConnection = usbManager.openDevice(usbDevice);
        serialInterface = UsbSerialDevice.createUsbSerialDevice(usbDevice, usbDeviceConnection);
        serialInterface.setBaudRate(RadioDevice.BAUD_RATE);
        connected = true;
    }

    public void closeConnection() {
        Log.v(TAG, "Closing connection to device");
        connected = false;
        serialInterface.close();
    }

    /**
     * Sends the given byte buffer to the serialInterface and reads the response
     *
     * This method is synchronized and so can only be run by 1 thread at a time
     *
     * @param buffer command
     * @return response byte
     */
    public synchronized byte[] sendForResponse(byte[] buffer) {
        Log.v(TAG, "Send byte, " + Arrays.toString(buffer));

        serialInterface.syncWrite(buffer, 100);
        byte[] returnBytes = new byte[100];

        serialInterface.syncRead(returnBytes, 100);

        return returnBytes;
    }

    public void setConnectionStateListener(ConnectionStateListener listener) {
        connectionStateListener = listener;
    }

    public boolean isConnected() {
        return connected;
    }
}
