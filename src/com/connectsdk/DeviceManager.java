package com.connectsdk;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.device.ConnectableDeviceListener;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.utils.ILogger;

import java.util.List;

public class DeviceManager implements ConnectableDeviceListener {
    private ConnectableDevice mDevice;
    private IDeviceStatusListener statusListener;
    private ILogger logger;
    private String TAG = "DeviceManager";
    private CastMediaPlayer mCastMediaPlayer;
    private ConnectedDeviceDialog connectedDeviceDialog;
    private Context mContext;

    public DeviceManager(Context context, ILogger logger) {
        this.logger = logger;
        this.mContext = context;
        this.connectedDeviceDialog = new ConnectedDeviceDialog();
        this.connectedDeviceDialog.setDeviceDialogListener(
                connectedDeviceDialogListener
        );
    }

    public void pickDevice(ConnectableDevice device) {
        if (mCastMediaPlayer != null) {
            mCastMediaPlayer.destroyPlayer(null);
        }
        clearDevice();
        mDevice = device;
        mCastMediaPlayer = new CastMediaPlayer(this.mContext, mDevice.getCapability(MediaPlayer.class), logger);
        mDevice.addListener(this);
        mDevice.connect();
        postDeviceStatus(CastState.CONNECTING, CastDevice.fromConnectableDevice(mDevice));
    }

    @Override
    public void onDeviceReady(ConnectableDevice device) {
        logger.log(Log.INFO, TAG, "onDeviceReady");
        mDevice = device;
        postDeviceStatus(CastState.CONNECTED, CastDevice.fromConnectableDevice(device));
    }

    @Override
    public void onDeviceDisconnected(ConnectableDevice device) {
        logger.log(Log.INFO, TAG, "onDeviceDisconnected");
        mCastMediaPlayer.destroyPlayer(null);
        mDevice.removeListener(this);
        mDevice = null;
        postDeviceStatus(CastState.NOT_CONNECTED, CastDevice.forDeviceNotAvailble());
    }

    @Override
    public void onPairingRequired(ConnectableDevice device, DeviceService service, DeviceService.PairingType pairingType) {
        logger.log(Log.INFO, TAG, "onPairingRequired");
    }

    @Override
    public void onCapabilityUpdated(ConnectableDevice device, List<String> added, List<String> removed) {
        logger.log(Log.INFO, TAG, "onCapabilityUpdated");
    }

    @Override
    public void onConnectionFailed(ConnectableDevice device, ServiceCommandError error) {
        logger.log(Log.INFO, TAG, "onConnectionFailed");
    }

    public void playMedia(CastMediaInfo castMediaInfo, final IMediaLifeCycleListener listener) {
        mCastMediaPlayer.playMedia(castMediaInfo, new IMediaLifeCycleListener() {

                    @Override
                    public void onMediaReady(RemoteMediaControl mediaControl) {
                        mediaControl.setDevice(CastDevice.fromConnectableDevice(mDevice));
                        listener.onMediaReady(mediaControl);
                    }

                    @Override
                    public void onMediaError() {
                        listener.onMediaError();
                    }
                }
        );
    }

    public void setStatusListener(IDeviceStatusListener statusListener) {
        this.statusListener = statusListener;
    }

    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    public AlertDialog getConnectedDeviceDialog(Activity activity) {
        if (mDevice != null) {
            return connectedDeviceDialog.getConnectedDeviceDialog(activity, mDevice.getFriendlyName());
        }
        return null;
    }

    private void postDeviceStatus(int status, CastDevice castDevice) {
        if (statusListener != null) {
            statusListener.onDeviceConnecting(status, castDevice);
        }
    }

    private void clearDevice() {
        logger.log(Log.INFO, TAG, "clearDevice " + (mDevice != null));
        if (mDevice != null) {
            mDevice.cancelPairing();
            mDevice.disconnect();
        }
    }

    private ConnectedDeviceDialog.IConnectedDeviceDialogListener connectedDeviceDialogListener = new ConnectedDeviceDialog.IConnectedDeviceDialogListener() {

        @Override
        public void disconnect() {
            logger.log(Log.INFO, TAG, "disconnect " + (mCastMediaPlayer != null));
            if (mCastMediaPlayer != null) {
                mCastMediaPlayer.destroyPlayer(new ResponseListener<Object>() {

                                                   @Override
                                                   public void onError(ServiceCommandError error) {
                                                       clearDevice();
                                                   }

                                                   @Override
                                                   public void onSuccess(Object object) {
                                                       clearDevice();
                                                   }
                                               }
                );
            }
        }
    };
}
