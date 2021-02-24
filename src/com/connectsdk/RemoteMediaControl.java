package com.connectsdk;

import android.util.Log;

import com.connectsdk.core.MediaInfo;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.utils.ILogger;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RemoteMediaControl {
    private MediaControl mediaControl;
    private ILogger logger;
    protected ScheduledFuture<?> updater;
    private String TAG = "RemoteMediaControl";
    private ScheduledExecutorService EXECUTOR;
    private int progressInterval = 1000;
    private long playheadPosition = 0;
    private long duration = 0;
    private boolean userRequestPending = false;
    private boolean playing = false;
    private MediaInfo mediaInfo;
    private CastDevice device;
    private List<WeakReference<IRemoteMediaEventListener>> mediaEventListeners;

    public RemoteMediaControl(MediaControl mediaControl, MediaInfo mediaInfo, ILogger logger) {
        this.mediaControl = mediaControl;
        this.mediaControl.getDuration(remoteDurationListener);
        this.logger = logger;
        this.mediaInfo = mediaInfo;
        EXECUTOR = Executors.newScheduledThreadPool(1);
        this.mediaControl.subscribePlayState(stateListener);
        this.mediaEventListeners = new ArrayList<>();
    }

    public MediaInfo getMediaInfo() {
        return mediaInfo;
    }

    public CastDevice getDevice() {
        return device;
    }

    public void setDevice(CastDevice device) {
        this.device = device;
    }

    private void startUpdater() {
        if (this.updater == null) {
            this.updater =
                    EXECUTOR.scheduleAtFixedRate(new Runnable() {
                                                     @Override
                                                     public void run() {
                                                         if (mediaControl != null && playing) {
                                                             if (!userRequestPending) mediaControl.getPosition(
                                                                     positionListener
                                                             );
                                                             postEvent(MediaEventType.PROGRESS, null);
                                                         }
                                                     }
                                                 },
                            0L,
                            (long) this.progressInterval,
                            TimeUnit.MILLISECONDS
                    );
        }
    }

    protected void stopUpdater() {
        logger.log(Log.INFO, TAG, "stopUpdater: " + this.updater);
        if (this.updater != null) {
            this.updater.cancel(false);
            this.updater = null;
        }
    }

    public void stop(final ResponseListener<Object> listener) {
        stopUpdater();
        userRequestPending = true;
        mediaControl.stop(new ResponseListener<Object>() {
                              @Override
                              public void onError(ServiceCommandError error) {
                                  postError(listener, error);
                              }

                              @Override
                              public void onSuccess(Object object) {
                                  postSuccess(listener, object);
                              }
                          }
        );
    }

    public void seek(final long position, final ResponseListener<Object> listener) {
        logger.log(Log.INFO, TAG, "seek: " + position);
        userRequestPending = true;
        mediaControl.seek(
                position,
                new ResponseListener<Object>() {

                    @Override
                    public void onError(ServiceCommandError error) {
                        postError(listener, error);
                    }

                    @Override
                    public void onSuccess(Object object) {
                        postSuccess(listener, object);
                        RemoteMediaControl.this.playheadPosition = position;
                        postEvent(MediaEventType.PROGRESS, null);
                    }
                }
        );
    }

    public void play(final ResponseListener<Object> listener) {
        logger.log(Log.INFO, TAG, "play");
        userRequestPending = true;
        mediaControl.play(
                new ResponseListener<Object>() {

                    @Override
                    public void onError(ServiceCommandError error) {
                        postError(listener, error);
                    }

                    @Override
                    public void onSuccess(Object object) {
                        mediaControl.getDuration(remoteDurationListener);
                        postSuccess(listener, object);
                    }
                }
        );
    }

    public void pause(final ResponseListener<Object> listener) {
        logger.log(Log.INFO, TAG, "pause");
        userRequestPending = true;
        this.mediaControl.pause(
                new ResponseListener<Object>() {

                    @Override
                    public void onError(ServiceCommandError error) {
                        postError(listener, error);
                    }

                    @Override
                    public void onSuccess(Object object) {
                        postSuccess(listener, object);
                    }
                }
        );
    }

    private MediaControl.DurationListener remoteDurationListener = new MediaControl.DurationListener() {

        @Override
        public void onSuccess(Long duration) {
            RemoteMediaControl.this.duration = duration;
            logger.log(Log.VERBOSE, TAG, "DurationListener: Success" + duration);
        }

        @Override
        public void onError(ServiceCommandError error) {
            logger.log(Log.WARN, TAG, "DurationListener: Error" + error.getMessage());
        }
    };

    private MediaControl.PositionListener positionListener = new MediaControl.PositionListener() {

        @Override
        public void onSuccess(Long position) {
            RemoteMediaControl.this.playheadPosition = position;
        }

        @Override
        public void onError(ServiceCommandError error) {
            logger.log(Log.WARN, TAG, "PositionListener: Error");
        }
    };

    private MediaControl.PlayStateListener stateListener = new MediaControl.PlayStateListener() {

        @Override
        public void onSuccess(MediaControl.PlayStateStatus status) {
            switch (status) {
                case Idle:
                    postEvent(MediaEventType.IDLE, null);
                    break;
                case Paused:
                    playing = false;
                    stopUpdater();
                    postEvent(MediaEventType.PAUSED, null);
                    break;
                case Playing:
                    if (!playing) {
                        startUpdater();
                        playing = true;
                    }
                    postEvent(MediaEventType.PROGRESS, null);
                    break;
                case Buffering:
                    postEvent(MediaEventType.BUFFERING, null);
                    break;
                case Finished:
                    playing = false;
                    stopUpdater();
                    postEvent(MediaEventType.FINISHED, null);
                    break;
            }
        }

        @Override
        public void onError(ServiceCommandError error) {
        }
    };

    public void addRemoteMediaEventListener(IRemoteMediaEventListener mediaEventListener) {
        this.mediaEventListeners.add(new WeakReference<>(mediaEventListener));
    }

    public void removeRemoteMediaEventListener(IRemoteMediaEventListener mediaEventListener) {
        this.mediaEventListeners.remove(new WeakReference<>(mediaEventListener));
    }

    private Map<String, Object> getDefaultEventProperties() {
        Map<String, Object> properties = new HashMap(2);
        properties.put("playheadPosition", this.playheadPosition + 2000);
        properties.put("duration", duration);
        return properties;
    }

    private void postError(ResponseListener<Object> listener, ServiceCommandError error) {
        userRequestPending = false;
        if (listener != null) {
            listener.onError(error);
        }
    }

    private void postSuccess(ResponseListener<Object> listener, Object success) {
        userRequestPending = false;
        if (listener != null) {
            listener.onSuccess(success);
        }
    }

    private void postEvent(String eventType, Map<String, Object> extraProperties) {
        Map<String, Object> basicProperties = getDefaultEventProperties();
        if (extraProperties != null) {
            basicProperties.putAll(extraProperties);
        }
        RemoteMediaEvent mediaEvent = new RemoteMediaEvent(eventType, basicProperties);
        for (WeakReference<IRemoteMediaEventListener> listener : mediaEventListeners) {
            IRemoteMediaEventListener eventListener = listener.get();
            if (eventListener != null)
                eventListener.onEvent(mediaEvent);
        }
    }
}
