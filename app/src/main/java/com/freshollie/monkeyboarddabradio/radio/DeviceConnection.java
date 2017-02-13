package com.freshollie.monkeyboarddabradio.radio;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.SystemClock;
import android.util.Log;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by Freshollie on 12/01/2017.
 *
 * Handles all connection interaction with the monkeyboard over the usb serial connection
 *
 */

public class DeviceConnection {

    private final String TAG = this.getClass().getSimpleName();

    static boolean DEBUG_OUTPUT = false;
    static boolean GET_RESPONSE_DEBUG = false;

    public static final int MAX_PACKET_LENGTH = 255;
    public final int COMMUNICATION_TIMEOUT_LENGTH = 20;
    public final int RESPONSE_TIMEOUT_LENGTH = 200;

    private final String ACTION_USB_PERMISSION =
            "com.freshollie.monkeyboarddabradio.radio.deviceconnection.action.USB_PERMISSION";

    private int commandSerialNumber = 0;
    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private UsbDeviceConnection usbDeviceConnection;
    private UsbSerialPort deviceSerialInterface;

    private ListenerManager.ConnectionStateChangeListener connectionStateListener;

    private Context context;

    private boolean running = false;

    public class NotConnectedException extends IOException{
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
                                device.getProductId() == RadioDevice.PRODUCT_ID) {
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
    }

    private UsbDevice getDevice() {
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            if (device.getProductId() == RadioDevice.PRODUCT_ID &&
                    device.getVendorId() == RadioDevice.VENDOR_ID) {
                return device;
            }
        }
        return null;
    }

    private void requestConnection() {
        Log.v(TAG, "Requesting connection to device");
        UsbDevice device = getDevice();
        if (device != null) {
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            filter.addAction(ACTION_USB_PERMISSION);
            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            context.registerReceiver(usbBroadcastReceiver, filter);

            if (usbManager.hasPermission(device)) {
                usbDevice = device;
                openConnection();
            } else {
                Log.v(TAG, "Requesting permission for device");
                usbManager.requestPermission(device, usbPermissionIntent);
            }
        } else {
            Log.v(TAG, "No devices found");
        }
    }

    public void start() {
        requestConnection();
    }

    public void stop() {
        if (isRunning()) {
            closeConnection();
        }
    }

    private void openConnection() {
        Log.v(TAG, "Opening connection to device");

        usbDeviceConnection = usbManager.openDevice(usbDevice);

        UsbSerialDriver driver = new CdcAcmSerialDriver(usbDevice);
        deviceSerialInterface = driver.getPorts().get(0);
        Log.v(TAG, "Device has " + String.valueOf(driver.getPorts().size()) + " ports");

        try {
            deviceSerialInterface.open(usbDeviceConnection);
            deviceSerialInterface.setParameters(RadioDevice.BAUD_RATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            deviceSerialInterface.setDTR(false);
            deviceSerialInterface.setRTS(true);

            running = true;
            connectionStateListener.onStart();
        } catch (IOException e){
            e.printStackTrace();
            closeConnection();
        }

    }

    private void closeConnection() {
        Log.v(TAG, "Closing connection to device");
        if (isRunning()) {
            running = false;
            context.unregisterReceiver(usbBroadcastReceiver);
            connectionStateListener.onStop();
        }

        if (deviceSerialInterface != null) {
            try {
                deviceSerialInterface.close();
            } catch (IOException e){

            }
        }

        deviceSerialInterface = null;
        usbDeviceConnection = null;
        usbDevice = null;
    }

    private byte generateCommandSerialNumber() {
        commandSerialNumber += 1;
        if (commandSerialNumber > 255) {
            commandSerialNumber = 0;
        }
        return (byte) commandSerialNumber;
    }

    /**
     * Gets the returned data from the given command serial number
     * @param serialNumber
     * @return
     */
    private byte[] getResponse(byte serialNumber) throws IOException{
        long startTime = SystemClock.elapsedRealtime();

        // This holds a command while it is being received
        byte[] commandBytes = new byte[MAX_PACKET_LENGTH];
        int commandByteNumber = 0;

        // Keep trying to read for a response byte until we time out
        while ((SystemClock.elapsedRealtime() - startTime) < RESPONSE_TIMEOUT_LENGTH && isRunning()) {
            byte[] readBytes = new byte[MAX_PACKET_LENGTH];
            int numBytesRead = deviceSerialInterface.read(readBytes, COMMUNICATION_TIMEOUT_LENGTH);

            for (int i = 0; i < numBytesRead; i++) {
                byte readByte = readBytes[i];
                if (commandByteNumber > 0) {
                    // We are currently reading a command

                    commandBytes[commandByteNumber] = readByte; // Add the next byte
                    commandByteNumber ++;
                    if (GET_RESPONSE_DEBUG) {
                        Log.d(TAG, "Readbyte: " + String.valueOf(readByte));
                        Log.d(TAG, "EndByte: " + String.valueOf(RadioDevice.ByteValues.END_BYTE));
                    }
                    if (readByte == RadioDevice.ByteValues.END_BYTE) {
                        if (GET_RESPONSE_DEBUG) {
                            Log.d(TAG, "End of command found");
                            Log.d(TAG, Arrays.toString(commandBytes));
                        }

                        // Checked if command is signed with our serial number
                        if (commandBytes[3] == serialNumber) {
                            if (GET_RESPONSE_DEBUG) {
                                Log.v(TAG, "Command is ours");
                            }
                            // The command has finished being read and the command
                            // is the command we are looking for so return it
                            return Arrays.copyOfRange(commandBytes, 0, commandByteNumber);

                        } else {
                            // This was not our command response, ignore it and restart
                            commandByteNumber = 0;
                            commandBytes = new byte[MAX_PACKET_LENGTH];
                        }
                    }
                } else if (readByte == RadioDevice.ByteValues.START_BYTE) {
                    // A new command has started
                    commandByteNumber = 0;
                    commandBytes = new byte[MAX_PACKET_LENGTH];
                    if (GET_RESPONSE_DEBUG) {
                        Log.v(TAG, "Read: New command starting to be read");
                    }

                    commandBytes[0] = readByte;
                    commandByteNumber ++;
                }
            }
        }

        Log.v(TAG, "getResponse timed out");
        return new byte[MAX_PACKET_LENGTH];
    }

    /**
     * Sends the given byte buffer to the serialInterface and reads the response
     *
     * This method is synchronized and so can only be run by 1 thread at a time
     *
     * @param commandBuffer command
     * @return response byte
     */
    public synchronized byte[] sendForResponse(byte[] commandBuffer) throws NotConnectedException{
        byte[] responseBytes = new byte[MAX_PACKET_LENGTH];

        if (isRunning()) {
            try {
                // Sign our command with a serial number so we know the response is correct
                byte serialNumber = generateCommandSerialNumber();
                commandBuffer[3] = serialNumber;

                if (DEBUG_OUTPUT) {
                    Log.v(TAG, "Send bytes, " + Arrays.toString(commandBuffer));
                }

                deviceSerialInterface.write(commandBuffer, COMMUNICATION_TIMEOUT_LENGTH);

                responseBytes = getResponse(serialNumber);

                if (responseBytes[0] != 0 && DEBUG_OUTPUT) {
                    Log.v(TAG, "Response bytes, " + Arrays.toString(responseBytes));
                }

            } catch (IOException e) {
                //e.printStackTrace();
            }
        } else {
            throw new NotConnectedException();
        }

        return responseBytes;
    }

    public void setConnectionStateListener(ListenerManager.ConnectionStateChangeListener listener) {
        connectionStateListener = listener;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isDeviceAttached() {
        return getDevice() != null;
    }
}
