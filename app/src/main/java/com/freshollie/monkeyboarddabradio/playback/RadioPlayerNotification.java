package com.freshollie.monkeyboarddabradio.playback;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.session.PlaybackState;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.freshollie.monkeyboarddabradio.PlayerActivity;
import com.freshollie.monkeyboarddabradio.R;

/**
 * Created by Freshollie on 15/01/2017.
 */

public class RadioPlayerNotification {
    private RadioPlayerService playerService;
    private NotificationCompat.Builder mediaNotificationBuilder;
    private NotificationManager notificationManager;

    private int NOTIFICATION_ID = 1;

    public RadioPlayerNotification(RadioPlayerService service) {
        playerService = service;
        notificationManager = (NotificationManager)
                playerService.getSystemService(Context.NOTIFICATION_SERVICE);


        Log.d("RadioPlayerNotification", "Starting foreground");
        update();
    }

    public Notification buildNotification() {
        int playIcon;
        String playAction;
        String playDescription;

        // Used for setting a pause of play icon and action depending on the current play state
        if (playerService.getPlaybackState() == PlaybackStateCompat.STATE_PLAYING) {
            playIcon = R.drawable.ic_pause_white_24dp;
            playAction = RadioPlayerService.ACTION_PAUSE;
            playDescription = "pause";
        } else {
            playIcon = R.drawable.ic_play_arrow_white_24dp;
            playAction = RadioPlayerService.ACTION_PLAY;
            playDescription = "play";
        }

        return new NotificationCompat.Builder(playerService)
                .setShowWhen(false)
                .setSmallIcon(R.drawable.ic_radio_black_24dp)
                .setLargeIcon(
                        ((BitmapDrawable) playerService.getResources().
                                getDrawableForDensity(R.mipmap.ic_launcher, 480, null)).getBitmap()
                )
                .setColor(ContextCompat.getColor(playerService, R.color.colorPrimaryDark))
                .setOngoing(true)
                .setContentIntent(
                        PendingIntent.getActivity(
                                playerService,
                                0,
                                new Intent(playerService, PlayerActivity.class),
                                PendingIntent.FLAG_UPDATE_CURRENT
                        )
                )
                .setStyle(
                        new NotificationCompat.MediaStyle()
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
                .addAction(
                        R.drawable.ic_skip_previous_white_24dp,
                        "prev",
                        getPendingIntentForAction(RadioPlayerService.ACTION_PREVIOUS)
                )
                .addAction(
                        playIcon,
                        playDescription,
                        getPendingIntentForAction(playAction)
                )
                .addAction(
                        R.drawable.ic_skip_next_white_24dp,
                        "next",
                        getPendingIntentForAction(RadioPlayerService.ACTION_NEXT)
                )
                .build();
    }

    private PendingIntent getPendingIntentForAction(String action) {
        return PendingIntent.getBroadcast(
                playerService,
                0,
                new Intent(action)
                        .setPackage(playerService.getPackageName()),
                PendingIntent.FLAG_CANCEL_CURRENT
        );
    }

    public void update() {
        playerService.startForeground(
                NOTIFICATION_ID,
                buildNotification()
        );
    }

    public void cancel() {
        notificationManager.cancel(NOTIFICATION_ID);
        playerService.stopForeground(true);
    }
}
