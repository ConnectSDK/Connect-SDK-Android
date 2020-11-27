package com.connectsdk;

import android.content.Context;
import android.util.Log;

import com.connectsdk.core.MediaInfo;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.utils.ILogger;

public class CastMediaPlayer {
    private MediaPlayer mediaPlayer;
    private RemoteMediaControl mediaControl;
    private ILogger logger;
    private static final String TAG = "CastMediaPlayer";
    private Context mContext;

    public CastMediaPlayer(Context context, MediaPlayer mediaPlayer, ILogger logger) {
        this.mediaPlayer = mediaPlayer;
        this.logger = logger;
        this.mContext = context;
    }

    public void playMedia(CastMediaInfo castMediaInfo, final IMediaLifeCycleListener listener) {
        final MediaInfo mediaInfo = new MediaInfo.Builder(castMediaInfo.mediaURL, castMediaInfo.mimeType)
                .setTitle(castMediaInfo.title)
                .setDescription(castMediaInfo.description)
                .setIcon(castMediaInfo.iconURL)
                .build();

        clearCurrentMedia(new ResponseListener<Object>() {
                              @Override
                              public void onError(ServiceCommandError error) {
                                  logger.log(Log.INFO, TAG, "stopCurrentMedia onError " + error.getMessage());
                                  startNewMedia(mediaInfo, listener);
                              }

                              @Override
                              public void onSuccess(Object object) {
                                  logger.log(Log.INFO, TAG, "stopCurrentMedia onSuccess ");
                                  startNewMedia(mediaInfo, listener);
                              }
                          }
        );
    }

    public void clearCurrentMedia(ResponseListener<Object> listener) {
        logger.log(Log.INFO, TAG, "stopCurrentMedia " + (mediaControl != null));
        if (mediaControl != null) {
            mediaControl.stop(listener);
            mediaControl = null;
        } else if (listener != null) {
            listener.onSuccess(null);
        }
    }

    private void startNewMedia(final MediaInfo mediaInfo, final IMediaLifeCycleListener listener) {
        if (mediaPlayer != null) {
            mediaPlayer.playMedia(
                    mediaInfo,
                    false,
                    new MediaPlayer.LaunchListener() {

                        @Override
                        public void onSuccess(MediaPlayer.MediaLaunchObject object) {
                            logger.log(Log.INFO, TAG, "playMedia onSuccess");
                            CastMediaPlayer.this.mediaControl =
                                    new RemoteMediaControl(object.mediaControl, mediaInfo, logger);
                            listener.onMediaReady(CastMediaPlayer.this.mediaControl);
                        }

                        @Override
                        public void onError(ServiceCommandError error) {
                            logger.log(Log.INFO, TAG, "playMedia onError");
                            listener.onMediaError();
                        }
                    }
            );
        } else {
            logger.log(Log.INFO, TAG, "playMedia onError");
            listener.onMediaError();
        }
    }

    public void destroyPlayer(final ResponseListener<Object> listener) {
        logger.log(Log.INFO, TAG, "destroyPlayer Start");
        clearCurrentMedia(
                new ResponseListener<Object>() {

                    @Override
                    public void onError(ServiceCommandError error) {
                        logger.log(Log.INFO, TAG, "destroyPlayer clearCurrentMedia error " + error.getMessage());
                        if (listener != null) listener.onError(error);
                    }

                    @Override
                    public void onSuccess(Object object) {
                        logger.log(Log.INFO, TAG, "destroyPlayer clearCurrentMedia onSuccess ");
                        if (listener != null) listener.onSuccess(object);
                    }
                }
        );
    }
}
