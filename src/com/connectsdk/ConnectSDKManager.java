package com.connectsdk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.RequiresApi;

import com.connectsdk.core.Util;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.device.DevicePicker;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.notification.MediaNotificationManager;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.utils.ActivityTracker;
import com.connectsdk.utils.DefaultLogger;
import com.connectsdk.utils.ILogger;

import java.util.ArrayList;
import java.util.List;

public class ConnectSDKManager {
    private static final Object lock = new Object();
    private static ConnectSDKManager instance = null;
    private Context mAppContext;
    private DeviceManager mDeviceManager;
    private DiscoveryManagerWrapper mDiscoveryManager;
    private MediaNotificationManager mNotificationManager;
    private ArrayList<ICastStateListener> castStateListeners = new ArrayList<>();
    private List<String> capabilities = new ArrayList<>();
    private CastStatus castStatus = new CastStatus();
    private ILogger logger;
    private ActivityTracker activityTracker;
    private DevicePicker mDevicePicker;

    private ConnectSDKManager() {
        capabilities.add(MediaPlayer.Play_Video);
        logger = new DefaultLogger();
    }

    public static ConnectSDKManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ConnectSDKManager();
                }
            }
        }
        return instance;
    }

    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void init(Application application, Activity currentActivity) {
        this.mAppContext = application;
        activityTracker = new ActivityTracker(currentActivity);
        application.registerActivityLifecycleCallbacks(activityTracker);
        mDeviceManager = new DeviceManager(application, logger);
        mDiscoveryManager = new DiscoveryManagerWrapper(application, capabilities, logger);
        mDiscoveryManager.setDeviceAvailabilityListener(availabilityListener);
        mDeviceManager.setStatusListener(statusListener);
        mDiscoveryManager.start();
        mNotificationManager = new MediaNotificationManager(application, logger, notificationListener);
    }

    public List<CastDevice> getAvailableDevices() {
        return mDiscoveryManager.getAvailableDevices();
    }

    public void pickDevice(CastDevice device) {
        ConnectableDevice connectableDevice = mDiscoveryManager.getConnectableDevice(device);
        mDeviceManager.pickDevice(connectableDevice);
    }

    public void playMedia(CastMediaInfo mediaInfo, final IMediaLifeCycleListener listener) {
        if (castStatus.getCastState() == CastState.CONNECTED) {
            mDeviceManager.playMedia(mediaInfo, new IMediaLifeCycleListener() {

                        @Override
                        public void onMediaReady(RemoteMediaControl mediaControl) {
                            mNotificationManager.setCurrentRemoteMediaControl(mediaControl);
                            listener.onMediaReady(mediaControl);
                        }

                        @Override
                        public void onMediaError() {
                            listener.onMediaError();
                        }
                    }
            );
        } else {
            listener.onMediaError();
        }
    }

    public void showDeviceSelector() {
        Util.runOnUI(new Runnable() {

                         @Override
                         public void run() {
                             mDiscoveryManager.setScanIntensity(DiscoveryProvider.ScanIntensity.ACTIVE);
                             if (isDialogHostAvailable()) {
                                 if (mDevicePicker == null) {
                                     mDevicePicker = new DevicePicker(activityTracker.getCurrentActivity());
                                 }
                                 final AlertDialog dialog = mDevicePicker.getPickerDialog(
                                         R.layout.header_layout,
                                         R.layout.item_layout,
                                         R.id.deviceName,
                                         new AdapterView.OnItemClickListener() {

                                             @Override
                                             public void onItemClick(
                                                     AdapterView<?> adapter,
                                                     View parent,
                                                     int position,
                                                     long id
                                             ) {
                                                 ConnectableDevice device = (ConnectableDevice) adapter.getItemAtPosition(
                                                         position
                                                 );
                                                 if (device != null) {
                                                     mDevicePicker.pickDevice(device);
                                                     mDeviceManager.pickDevice(
                                                             (ConnectableDevice) adapter.getItemAtPosition(position)
                                                     );
                                                 }
                                                 mDiscoveryManager.setScanIntensity(DiscoveryProvider.ScanIntensity.PASSIVE);
                                             }
                                         }
                                 );
                                 dialog.show();
                             }
                         }
                     }
        );
    }

    public void showConnectedDevice() {
        if (isDialogHostAvailable() && mDevicePicker != null) {
            AlertDialog dialog = mDeviceManager.getConnectedDeviceDialog(activityTracker.getCurrentActivity());
            if (dialog != null) dialog.show();
        }
    }

    public CastStatus getCurrentCastInfo() {
        return castStatus;
    }

    public void addCastStateListener(ICastStateListener listener) {
        castStateListeners.add(listener);
    }

    public void removeCastStateListener(ICastStateListener listener) {
        castStateListeners.remove(listener);
    }

    private IDeviceAvailabilityListener availabilityListener = new IDeviceAvailabilityListener() {

        @Override
        public void onAvailabilityChange(boolean available) {
            if (castStatus.getCastState() == CastState.NO_DEVICE_AVAILABLE && available) {
                castStatus.setCastState(CastState.NOT_CONNECTED);
                postCurrentCastState();
            } else if (castStatus.getCastState() != CastState.NO_DEVICE_AVAILABLE && !available) {
                castStatus.setCastState(CastState.NO_DEVICE_AVAILABLE);
                postCurrentCastState();
            }
        }
    };

    private IDeviceStatusListener statusListener = new IDeviceStatusListener() {

        @Override
        public void onDeviceConnecting(int castState, CastDevice castDevice) {
            if (castStatus.getCastState() != CastState.NO_DEVICE_AVAILABLE) {
                castStatus.setCastState(castState);
                castStatus.setCastDevice(castDevice);
                postCurrentCastState();
            }
        }
    };

    private MediaNotificationManager.INotificationListener notificationListener = new MediaNotificationManager.INotificationListener() {
        @Override
        public void onCastMediaStopped() {
            if (mDeviceManager != null) {
                mDeviceManager.clearDevice();
            }
        }
    };

    private void postCurrentCastState() {
        for (ICastStateListener listener : castStateListeners) {
            listener.onCastStateChanged(castStatus);
        }
    }

    private boolean isDialogHostAvailable() {
        return activityTracker.getCurrentActivity() != null
                && !activityTracker.getCurrentActivity().isDestroyed()
                && !activityTracker.getCurrentActivity().isFinishing();
    }

    public void handleExit() {
        if (mNotificationManager != null) {
            mNotificationManager.handleExit();
        }
        if (mDeviceManager != null) {
            mDeviceManager.clearDevice();
        }
    }

    public void onDestroy() {
        mDiscoveryManager.onDestory();
        mDevicePicker = null;
    }
}
