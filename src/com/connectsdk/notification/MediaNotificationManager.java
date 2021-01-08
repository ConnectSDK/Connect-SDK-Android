package com.connectsdk.notification;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;

import com.connectsdk.CastState;
import com.connectsdk.CastStatus;
import com.connectsdk.ICastStateListener;
import com.connectsdk.IRemoteMediaEventListener;
import com.connectsdk.MediaEventType;
import com.connectsdk.R;
import com.connectsdk.RemoteMediaControl;
import com.connectsdk.RemoteMediaEvent;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.utils.ILogger;

import java.net.URL;

public class MediaNotificationManager implements IRemoteMediaEventListener, ICastStateListener {
    private Application application;
    private static int notificationId = 1111;
    NotificationManager notificationManager;
    private RemoteMediaControl remoteMediaControl;
    private ILogger logger;
    private static final String TAG = "MediaNotificationManager";
    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_STOP = "action_stop";
    private static final String channelId = "FanCodeCast";
    private boolean isPlayingUpdated = true;
    private Notification.Action stopAction;
    private INotificationListener notificationListener;

    public MediaNotificationManager(Application application, ILogger logger, INotificationListener listener) {
        this.logger = logger;
        this.application = application;
        this.notificationListener = listener;
        notificationManager = (NotificationManager) application.getSystemService(Context.NOTIFICATION_SERVICE);
        this.stopAction = generateAction(android.R.drawable.ic_menu_delete, "Stop", ACTION_STOP);
    }

    public void setCurrentRemoteMediaControl(RemoteMediaControl remoteMediaControl) {
        if (remoteMediaControl != null
                && remoteMediaControl.getDevice() != null
                && "firetv".equalsIgnoreCase(remoteMediaControl.getDevice().getType())) {
            if (this.remoteMediaControl != null) {
                this.remoteMediaControl.removeRemoteMediaEventListener(this);
            }
            this.remoteMediaControl = remoteMediaControl;
            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
            this.remoteMediaControl.addRemoteMediaEventListener(this);
        }
    }


    private Notification buildNotification(Notification.Action action) {
        String title = this.remoteMediaControl.getMediaInfo().getTitle();
        String subTitle = "Casting on " + this.remoteMediaControl.getDevice().getName();
        Intent intent = new Intent(application, MediaNotificationService.class);
        intent.setAction(ACTION_STOP);
        Notification.BigPictureStyle notificationStyle = new Notification.BigPictureStyle();
        PendingIntent pendingIntent = PendingIntent.getService(application, 1, intent, 0);

        Notification.Builder builder = new Notification.Builder(application)
                .setContentTitle(title)
                .setContentText(subTitle)
                .setDeleteIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_notification)
                .setOnlyAlertOnce(true)
                .addAction(action)
                .setColor(Color.argb(100, 255, 80, 0))
                .addAction(stopAction);

        try {
            URL url = new URL(this.remoteMediaControl.getMediaInfo().getImages().get(0).getUrl());
            Bitmap image = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            builder.setStyle(notificationStyle.bigPicture(image));
        } catch (Exception e) {
            System.out.println(e);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(channelId);
        }
        Notification notification = builder.build();
        MediaNotificationService.notificationManager = this;
        MediaNotificationService.currentNotification = notification;
        notificationManager.notify(notificationId, notification);
        return notification;
    }

    private Notification.Action generateAction(int icon, String title, String intentAction) {
        Intent intent = new Intent(application, MediaNotificationService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(application, 1, intent, 0);
        return new Notification.Action.Builder(icon, title, pendingIntent).build();
    }


    public void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null || this.remoteMediaControl == null) return;
        String action = intent.getAction();
        if (action.equalsIgnoreCase(ACTION_PLAY)) {
            this.remoteMediaControl.play(null);
        } else if (action.equalsIgnoreCase(ACTION_PAUSE)) {
            this.remoteMediaControl.pause(null);
        } else if (action.equalsIgnoreCase(ACTION_STOP)) {
            stopMedia();
        }
    }

    private void stopMedia() {
        if (this.remoteMediaControl != null) {
            this.remoteMediaControl.stop(new ResponseListener<Object>() {
                @Override
                public void onError(ServiceCommandError error) {
                    stopCasting();
                }

                @Override
                public void onSuccess(Object object) {
                    stopCasting();
                }
            });
        }
    }


    @Override
    public void onEvent(RemoteMediaEvent mediaEvent) {
        if (mediaEvent.getEventType() == MediaEventType.PROGRESS && !isPlayingUpdated) {
            isPlayingUpdated = true;
            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE)
            );
        } else if (mediaEvent.getEventType() == MediaEventType.PAUSED) {
            isPlayingUpdated = false;
            buildNotification(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY));
        } else if (mediaEvent.getEventType() == MediaEventType.FINISHED) {
            isPlayingUpdated = false;
            notificationManager.cancel(notificationId);
        }
    }

    @Override
    public void onCastStateChanged(CastStatus castStatus) {
        if (castStatus.getCastState() == CastState.NOT_CONNECTED || castStatus.getCastState() == CastState.NO_DEVICE_AVAILABLE) {
            isPlayingUpdated = false;
            notificationManager.cancel(notificationId);
        }
    }

    private void stopCasting() {
        isPlayingUpdated = false;
        notificationManager.cancel(notificationId);
        if (notificationListener != null)
            notificationListener.onCastMediaStopped();
    }

    public static class MediaNotificationService extends Service {
        private static MediaNotificationManager notificationManager;
        private static Notification currentNotification;

        @Override
        public void onCreate() {
            super.onCreate();
            if (currentNotification != null) {
                startForeground(notificationId, currentNotification);
            }
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            if (notificationManager != null) {
                notificationManager.handleIntent(intent);
            }
            return START_NOT_STICKY;
        }

        @Override
        public boolean onUnbind(Intent intent) {
            return super.onUnbind(intent);
        }

    }

    public void onDestroy() {
        stopMedia();
        stopCasting();
    }

    public interface INotificationListener {
        public void onCastMediaStopped();
    }
}
