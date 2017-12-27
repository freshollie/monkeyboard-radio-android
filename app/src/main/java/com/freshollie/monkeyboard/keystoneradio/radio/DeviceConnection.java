/*
 * Created by Oliver Bell on 12/01/2017
 * Copyright (c) 2017. by Oliver bell <freshollie@gmail.com>
 *
 * Last modified 28/05/17 01:07
 */

package com.freshollie.monkeyboard.keystoneradio.radio;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.Ch34xSerialDriver;
import com.hoho.android.usbserial.driver.Cp21xxSerialDriver;
import com.hoho.android.usbserial.driver.FtdiSerialDriver;
import com.hoho.android.usbserial.driver.ProlificSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles all connection interaction with the monkeyboard over the usb serial connection
 */

public class DeviceConnection {

    private final String TAG = this.getClass().getSimpleName();

    static boolean DEBUG_OUTPUT = false;
    static boolean GET_RESPONSE_DEBUG = false;

    public static final int MAX_PACKET_LENGTH = 256;
    private static final int COMMUNICATION_TIMEOUT_LENGTH = 100;
    private static final int RESPONSE_TIMEOUT_LENGTH = 300;

    private final String ACTION_USB_PERMISSION =
            "com.freshollie.monkeyboard.keystoneradio.radio.deviceconnection.action.USB_PERMISSION";

    private int commandSerialNumber = 0;
    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private UsbDeviceConnection usbDeviceConnection;
    private UsbSerialPort deviceSerialInterface;

    private RadioDeviceListenerManager.ConnectionStateChangeListener connectionStateListener;

    private Context context;

    private static final int RECONNECT_TIMEOUT = 5000;

    // Used in case the device disconnects for 5000ms
    private Runnable reconnectRunnable = new Runnable() {
        @Override
        public void run() {
            Log.v(TAG, "Attempting to reconnect to device");

            long startTime = System.currentTimeMillis();

            while ((System.currentTimeMillis() - startTime) < RECONNECT_TIMEOUT && !Thread.interrupted()) {
                if (getDevice() != null) {
                    Log.v(TAG, "Success, reconnecting");
                    start();
                    return;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
            }

            Log.v(TAG, "Reconnecting failed, closing connection");

            running = true;
            stop();
        }
    };

    private Thread reconnectThread;

    private Runnable connectRunnable = new Runnable() {
        @Override
        public void run() {
            requestConnection();
        }
    };

    private Thread connectThread;

    private final ArrayList<Byte> readBuffer = new ArrayList<>();

    private boolean running = false;

    private Runnable readBufferFillerRunnable = new Runnable() {
        @Override
        public void run() {
            while (!Thread.interrupted() && isConnectionOpen()) {
                byte[] readBytes = new byte[MAX_PACKET_LENGTH];
                try {
                    int numRead = deviceSerialInterface.read(readBytes, COMMUNICATION_TIMEOUT_LENGTH);
                    for (int i = 0; i < numRead; i++) {
                        readBuffer.add(readBytes[i]);
                    }
                } catch (IOException e) {
                }
            }
        }
    };

    private Thread readBufferFillerThread;
    private boolean readRunning;
    private byte[] lastResponse;

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

            if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (device != null) {
                        Log.v(TAG, "Permission for device granted");
                        start();
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction())) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    if (device.getVendorId() == RadioDevice.VENDOR_ID &&
                            device.getProductId() == RadioDevice.PRODUCT_ID) {
                        attemptReconnect();
                    }
                }
            }
        }
    };

    private PendingIntent usbPermissionIntent;

    /**
     * Initialises connection object. Requires context.
     * @param appContext
     */
    public DeviceConnection(Context appContext) {
        context = appContext;

        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        usbPermissionIntent =
                PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
    }

    /**
     * @return the radio device
     */
    private UsbDevice getDevice() {
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            if (device.getProductId() == RadioDevice.PRODUCT_ID &&
                    device.getVendorId() == RadioDevice.VENDOR_ID) {
                return device;
            }
        }
        return null;
    }

    /**
     * Will try an reconnect for 5000ms after disconnect.
     */
    private void attemptReconnect() {
        Log.v(TAG, "Lost connection, attempting to reestablish");

        running = false;
        closeConnection();

        if (reconnectThread != null) {
            reconnectThread.interrupt();
            reconnectThread = null;
        }
        reconnectThread = new Thread(reconnectRunnable);
        reconnectThread.start();
    }

    /**
     * Checks the devices connected to android and finds the radio. It then attempts to get
     * permission to connect to the radio.
     */
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
            if (connectionStateListener != null) {
                connectionStateListener.onFail();
            }
            Log.v(TAG, "No device found");
        }
    }

    /**
     * Used as API to start the connection
     */
    public void start() {
        Log.v(TAG, "Start");
        if (!running) {
            connectThread = new Thread(connectRunnable);
            connectThread.start();
        }
    }

    /**
     * API used to stop the connection
     */
    public void stop() {
        Log.v(TAG, "Stop");
        if (isConnectionOpen()) {
            closeConnection();
            running = false;
            context.unregisterReceiver(usbBroadcastReceiver);
            if (connectionStateListener != null) {
                connectionStateListener.onStop();
            }
        }

        if (reconnectThread != null) {
            reconnectThread.interrupt();
            reconnectThread = null;
        }

        if (connectThread != null) {
            connectThread.interrupt();
            connectThread = null;
        }
    }

    /**
     * Called once we have permission, this will open a connection to the radio
     */
    private void openConnection() {
        Log.v(TAG, "Opening connection to device");

        usbDeviceConnection = usbManager.openDevice(usbDevice);
        UsbSerialDriver driver = new CdcAcmSerialDriver(usbDevice);

        Log.v(TAG, "Device has " + String.valueOf(driver.getPorts().size()) + " ports");
        deviceSerialInterface = driver.getPorts().get(0);

        try {
            deviceSerialInterface.open(usbDeviceConnection);
            deviceSerialInterface.setParameters(RadioDevice.BAUD_RATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            deviceSerialInterface.setDTR(false);
            deviceSerialInterface.setRTS(true);

            readBuffer.clear();

            readBufferFillerThread = new Thread(readBufferFillerRunnable);
            //readBufferFillerThread.start();

            running = true;
            if (connectionStateListener != null) {
                connectionStateListener.onStart();
            }
        } catch (IOException e) {
            e.printStackTrace();
            closeConnection();
            if (connectionStateListener != null) {
                connectionStateListener.onFail();
            }
        }
    }


    /**
     * Ends the connection to the radio
     */
    private void closeConnection() {
        Log.v(TAG, "Closing connection to device");

        if (readBufferFillerThread != null) {
            readBufferFillerThread.interrupt();
            readBufferFillerThread = null;
        }

        if (deviceSerialInterface != null) {
            try {
                deviceSerialInterface.close();
            } catch (IOException ignored){}
        }

        deviceSerialInterface = null;
        usbDeviceConnection = null;
        usbDevice = null;
    }

    /**
     * Generates a unique serial byte (0-255) for the command
     * @return
     */
    private byte generateCommandSerialNumber() {
        commandSerialNumber += 1;
        if (commandSerialNumber > 255) {
            commandSerialNumber = 0;
        }
        return (byte) commandSerialNumber;
    }

    private byte[] takeAllFromReadBuffer() {
        byte[] bytes = new byte[readBuffer.size()];

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = readBuffer.remove(0);
        }

        return bytes;
    }

    /**
     * Reads the response data until it finds the returned data from the given command serial number
     *
     * @param serialNumber
     * @return
     */
    private byte[] getResponse(byte serialNumber) {
        /*
            It does this by reading the in bytes until it gets a start command
            byte, then once a full command is received it checks the serial number of the command to
            verify it is the correct command
        */

        long startTime = System.currentTimeMillis();

        // This holds a command while it is being received
        byte[] commandBytes = new byte[MAX_PACKET_LENGTH];
        int commandByteNumber = 0;
        int payloadLength = -1;

        // Keep trying to read for a response byte until we time out
        while ((System.currentTimeMillis() - startTime) < RESPONSE_TIMEOUT_LENGTH && isConnectionOpen()) {
            byte[] readBytes = new byte[4096];
            int numBytesRead = 0;

            try {
                numBytesRead = deviceSerialInterface.read(readBytes, COMMUNICATION_TIMEOUT_LENGTH);
            } catch (Exception e) {
                break;
            }

            for (int i = 0; i < numBytesRead; i++) {
                byte readByte = readBytes[i];
                if (commandByteNumber > 0) {
                    // We are currently reading a command

                    // Add the next byte
                    commandBytes[commandByteNumber] = readByte;

                    if (GET_RESPONSE_DEBUG) {
                        Log.d(TAG, "Readbyte: " + String.valueOf(readByte));
                    }

                    // We are at what should be the payload length byte, so read the payload length
                    if (commandByteNumber == 5) {
                        payloadLength = readByte & 0xFF;
                        if (payloadLength > (255 - 6)) {
                            Log.e(TAG, "Received a packet with a bad payload length: " + payloadLength);
                            commandByteNumber = 0;
                            payloadLength = -1;
                            commandBytes = new byte[MAX_PACKET_LENGTH];
                            continue;
                        }
                    }

                    if (commandByteNumber == 3) {
                        if (readByte != serialNumber) {
                            // This was not our command response, ignore it and restart
                            commandByteNumber = 0;
                            payloadLength = -1;
                            commandBytes = new byte[MAX_PACKET_LENGTH];
                            continue;
                        }

                        if (GET_RESPONSE_DEBUG) {
                            Log.v(TAG, "Command is ours");
                        }
                    }

                    if (commandByteNumber > 5 && (commandByteNumber - 6) >= payloadLength) {
                        // We at at least our required payload length
                        if (readByte == RadioDevice.ByteValues.END_BYTE) {
                            if (GET_RESPONSE_DEBUG) {
                                Log.d(TAG, "End of command found");
                                Log.d(TAG, Arrays.toString(commandBytes));
                            }

                            return Arrays.copyOfRange(commandBytes, 0, commandByteNumber + 1);

                        } else {
                            Log.v(TAG,String.valueOf(readByte));
                            Log.v(TAG, "BAD PACKET " + Arrays.toString(commandBytes));

                            // We are larger than the buffer size and we didn't get an end byte
                            // So maybe continue trying to read, but drop our received buffer
                            commandByteNumber = 0;
                            payloadLength = -1;
                            commandBytes = new byte[MAX_PACKET_LENGTH];
                            continue;
                        }
                    }

                    commandByteNumber++;
                } else if (readByte == RadioDevice.ByteValues.START_BYTE) {
                    // A new command has started
                    commandByteNumber = 0;
                    payloadLength = -1;

                    commandBytes = new byte[MAX_PACKET_LENGTH];
                    if (GET_RESPONSE_DEBUG) {
                        Log.v(TAG, "Read: New command starting to be read");
                    }

                    commandBytes[0] = readByte;

                    commandByteNumber++;
                }
            }
        }

        if (DEBUG_OUTPUT) {
            Log.v(TAG, "getResponse timed out");
        }
        if (payloadLength > -1 &&
                commandBytes[commandByteNumber - 1] == RadioDevice.ByteValues.END_BYTE) {
            Log.e(TAG, "Timed out trying to read entire payload, but the last byte received was a response end byte");
            Log.e(TAG, "Expected payload length: " + payloadLength + ", Actual length: " + (commandByteNumber - 6));
        }
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

        if (isConnectionOpen()) {
            // Sign our command with a serial number so we know the response is correct
            final byte serialNumber = generateCommandSerialNumber();
            commandBuffer[3] = serialNumber;

            if (DEBUG_OUTPUT) {
                Log.v(TAG, "Send bytes, " + Arrays.toString(commandBuffer));
            }

            // New method to help read more values
            // start reading response before we even send the request

            // This is a bit of a hack
            final AtomicBoolean threadStarted = new AtomicBoolean();
            threadStarted.set(false);

            final byte[][] threadResponse = new byte[1][MAX_PACKET_LENGTH];

            // Make a new thread for the read request
            Thread getResponseThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    threadStarted.set(true);
                    threadResponse[0] = getResponse(serialNumber);
                }
            });

            getResponseThread.start();

            // Wait until we know the thread has started before continuing
            while (getResponseThread.isAlive()) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ignored) {}
            }

            // Write the command
            try {
                deviceSerialInterface.write(commandBuffer, COMMUNICATION_TIMEOUT_LENGTH);
            } catch (Exception ignored) {}

            // Wait until we have a response, or the response has timed out
            try {
                getResponseThread.join();
            } catch (InterruptedException ignored) {}

            // Fill the response with what we got from the thread
// Write the command
            if (responseBytes[0] != 0 && DEBUG_OUTPUT) {
                Log.v(TAG, "Response bytes, " + Arrays.toString(responseBytes));
            }
        } else {
            throw new NotConnectedException();
        }

        return responseBytes;
    }

    public void setConnectionStateListener(RadioDeviceListenerManager.ConnectionStateChangeListener listener) {
        connectionStateListener = listener;
    }

    public boolean isConnectionOpen() {
        return running;
    }

    public boolean isDeviceAttached() {
        return getDevice() != null;
    }
}
