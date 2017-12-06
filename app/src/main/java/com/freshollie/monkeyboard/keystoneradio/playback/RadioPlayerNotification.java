/*
 * Created by Oliver Bell on 15/01/2017
 * Copyright (c) 2017. by Oliver bell <freshollie@gmail.com>
 *
 * Last modified 14/06/17 23:15
 */

package com.freshollie.monkeyboard.keystoneradio.playback;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import java.text.DecimalFormat;

import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.app.NotificationCompat.MediaStyle;
import android.util.FloatMath;
import android.util.Log;

import com.freshollie.monkeyboard.keystoneradio.radio.RadioDevice;
import com.freshollie.monkeyboard.keystoneradio.ui.PlayerActivity;
import com.freshollie.monkeyboard.keystoneradio.R;

/**
 * Handles the player service notification, will update using the players metadata when
 * requested
 */
public class RadioPlayerNotification extends MediaControllerCompat.Callback {
    private RadioPlayerService playerService;
    private NotificationCompat.Builder mediaNotificationBuilder;
    private NotificationManager notificationManager;

    private boolean wasPlaying = false;
    private int lastFrequency;

    private int NOTIFICATION_ID = 1;

    private static final String CHANNEL_ID = "RADIO";

    public RadioPlayerNotification(RadioPlayerService service) {
        playerService = service;
        notificationManager = (NotificationManager)
                playerService.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create a notification channel for
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Media playback";
            String description = "Media playback controls";

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    name,
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(description);
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            notificationManager.createNotificationChannel(channel);
        }

        Log.d("RadioPlayerNotification", "Starting foreground");
        playerService.getMediaSession().setActive(true);
        playerService.getMediaController().registerCallback(this);

        update();
    }

    private int getDominantColor(Bitmap bitmap) {
        if (null == bitmap) return Color.TRANSPARENT;

        int redBucket = 0;
        int greenBucket = 0;
        int blueBucket = 0;
        int alphaBucket = 0;

        boolean hasAlpha = bitmap.hasAlpha();
        int pixelCount = bitmap.getWidth() * bitmap.getHeight();
        int[] pixels = new int[pixelCount];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int y = 0, h = bitmap.getHeight(); y < h; y++)
        {
            for (int x = 0, w = bitmap.getWidth(); x < w; x++)
            {
                int color = pixels[x + y * w]; // x + y * width
                redBucket += (color >> 16) & 0xFF; // Color.red
                greenBucket += (color >> 8) & 0xFF; // Color.greed
                blueBucket += (color & 0xFF); // Color.blue
                if (hasAlpha) alphaBucket += (color >>> 24); // Color.alpha
            }
        }

        return Color.argb(
                (hasAlpha) ? (alphaBucket / pixelCount) : 255,
                redBucket / pixelCount,
                greenBucket / pixelCount,
                blueBucket / pixelCount);
    }

    private Bitmap letterboxImage(Bitmap srcBmp) {
        int dim = Math.max(srcBmp.getWidth(), srcBmp.getHeight());
        Bitmap dstBmp = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(dstBmp);
        canvas.drawColor(getDominantColor(srcBmp));
        canvas.drawBitmap(srcBmp, (dim - srcBmp.getWidth()) / 2, (dim - srcBmp.getHeight()) / 2, null);

        return dstBmp;
    }

    private Notification buildNotification() {
        int playIcon;
        String playAction;
        String playDescription;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(playerService, CHANNEL_ID);

        // Used for setting a pause of play icon and action depending on the current play state
        if (playerService.getPlaybackState() == PlaybackStateCompat.STATE_PLAYING) {
            playIcon = R.drawable.ic_notification_pause;
            playAction = RadioPlayerService.ACTION_PAUSE;
            playDescription = "pause";
            builder.setOngoing(true);
        } else {
            playIcon = R.drawable.ic_notification_play;
            playAction = RadioPlayerService.ACTION_PLAY;
            playDescription = "play";
        }

        if (playerService.getRadioMode() == RadioDevice.Values.STREAM_MODE_FM) {

            String trackName = playerService.getMetadata()
                    .getString(MediaMetadataCompat.METADATA_KEY_TITLE);

            if (trackName == null || trackName.isEmpty()) {
                trackName = new DecimalFormat("#.0").format(
                                    playerService.getMetadata().getLong(
                                            MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER
                                    ) / 1000.0
                );
            }

            return builder.setShowWhen(false)
                    .setLargeIcon(
                            ((BitmapDrawable) ResourcesCompat.getDrawableForDensity(
                                    playerService.getResources(),
                                    R.mipmap.ic_launcher,
                                    480,
                                    null
                            )).getBitmap()
                    )
                    .setSmallIcon(R.drawable.ic_notification_radio)
                    .setColor(ContextCompat.getColor(playerService, R.color.colorPrimaryDark))
                    .setContentIntent(
                            PendingIntent.getActivity(
                                    playerService,
                                    0,
                                    new Intent(playerService, PlayerActivity.class),
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            )
                    )
                    .setStyle(
                            new MediaStyle()
                                    .setMediaSession(
                                            playerService.getMediaSession()
                                                    .getSessionToken()
                                    )
                                    .setShowCancelButton(true)
                                    .setShowActionsInCompactView(0, 2, 4)
                                    .setCancelButtonIntent(
                                            getPendingIntentForAction(
                                                    RadioPlayerService.ACTION_STOP
                                            )
                                    )
                    )
                    .setContentTitle(trackName)
                    .setContentInfo(
                            playerService.getMetadata().getString(
                                    MediaMetadataCompat.METADATA_KEY_GENRE
                            )
                    )
                    .setContentText(
                            playerService.getMetadata().getString(
                                    MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION
                            )
                    )
                    .addAction(
                            R.drawable.ic_notification_skip_prev,
                            "prev",
                            getPendingIntentForAction(RadioPlayerService.ACTION_PREVIOUS)
                    )
                    .addAction(
                            R.drawable.ic_notification_rewind,
                            "search_backwards",
                            getPendingIntentForAction(RadioPlayerService.ACTION_SEARCH_BACKWARDS)
                    )
                    .addAction(
                            playIcon,
                            playDescription,
                            getPendingIntentForAction(playAction)
                    )
                    .addAction(
                            R.drawable.ic_notification_fast_forward,
                            "search_forwards",
                            getPendingIntentForAction(RadioPlayerService.ACTION_SEARCH_FORWARDS)
                    )
                    .addAction(
                            R.drawable.ic_notification_skip_next,
                            "next",
                            getPendingIntentForAction(RadioPlayerService.ACTION_NEXT)
                    )
                    .setDeleteIntent(
                            getPendingIntentForAction(
                                    RadioPlayerService.ACTION_STOP
                            )
                    )
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .build();
        } else {
            Bitmap notificationImage =
                    playerService
                    .getMetadata()
                    .getBitmap(MediaMetadataCompat.METADATA_KEY_ART);

            if (notificationImage == null) {
                notificationImage = ((BitmapDrawable) ResourcesCompat.getDrawableForDensity(
                        playerService.getResources(),
                        R.mipmap.ic_launcher,
                        480,
                        null
                )).getBitmap();
            } else {
                // Letterbox the image inside a square so that it displays probably in the
                // notification
                notificationImage = letterboxImage(notificationImage);
            }

            return builder.setShowWhen(false)
                    .setLargeIcon(notificationImage)
                    .setSmallIcon(R.drawable.ic_notification_radio)
                    .setColor(ContextCompat.getColor(playerService, R.color.colorPrimaryDark))
                    .setContentIntent(
                            PendingIntent.getActivity(
                                    playerService,
                                    0,
                                    new Intent(playerService, PlayerActivity.class),
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            )
                    )
                    .setStyle(
                            new MediaStyle()
                                    .setMediaSession(
                                            playerService.getMediaSession()
                                                    .getSessionToken()
                                    )
                                    .setShowCancelButton(true)
                                    .setShowActionsInCompactView(0, 1, 2)
                                    .setCancelButtonIntent(
                                            getPendingIntentForAction(
                                                    RadioPlayerService.ACTION_STOP
                                            )
                                    )
                    )
                    .setContentTitle(playerService.getMetadata()
                            .getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                    )
                    .setContentInfo(
                            playerService.getMetadata().getString(
                                    MediaMetadataCompat.METADATA_KEY_GENRE
                            )
                    )
                    .setContentText(
                            playerService.getMetadata().getString(
                                MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION
                        )
                    )
                    .addAction(
                            R.drawable.ic_notification_skip_prev,
                            "prev",
                            getPendingIntentForAction(RadioPlayerService.ACTION_PREVIOUS)
                    )
                    .addAction(
                            playIcon,
                            playDescription,
                            getPendingIntentForAction(playAction)
                    )
                    .addAction(
                            R.drawable.ic_notification_skip_next,
                            "next",
                            getPendingIntentForAction(RadioPlayerService.ACTION_NEXT)
                    )
                    .setDeleteIntent(
                            getPendingIntentForAction(
                                    RadioPlayerService.ACTION_STOP
                            )
                    )
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .build();
        }
    }

    private PendingIntent getPendingIntentForAction(String action) {
        return PendingIntent.getService(
                playerService,
                0,
                new Intent(playerService, RadioPlayerService.class)
                        .setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    private void update() {
        if (playerService.getPlaybackState() == PlaybackStateCompat.STATE_PLAYING) {
            playerService.startForeground(
                    NOTIFICATION_ID,
                    buildNotification()
            );
        } else {
            playerService.stopForeground(false);
            notificationManager.notify(
                    NOTIFICATION_ID,
                    buildNotification()
            );
        }
    }

    public void cancel() {
        playerService.getMediaSession().setActive(false);
        notificationManager.cancel(NOTIFICATION_ID);
        playerService.stopForeground(true);
        playerService.getMediaController().unregisterCallback(this);
    }

    @Override
    public void onMetadataChanged(MediaMetadataCompat metadata) {
        update();
    }

    @Override
    public void onPlaybackStateChanged(PlaybackStateCompat state) {
        update();
    }
}
