package com.connectsdk;

import android.content.Context;
import android.util.Log;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.CapabilityFilter;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryManagerListener;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.discovery.provider.CastDiscoveryProvider;
import com.connectsdk.service.CastService;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.utils.ILogger;

import java.util.ArrayList;
import java.util.List;

class DiscoveryManagerWrapper implements DiscoveryManagerListener {
    private ArrayList<ConnectableDevice> availableDevices = new ArrayList<>();
    private DiscoveryManager mDiscoveryManager;
    private IDeviceAvailabilityListener availabilityListener;
    private boolean isDeviceAvailable = false;
    private ILogger logger;
    private String TAG = "DiscoveryManager";

    DiscoveryManagerWrapper(Context context, List<String> capabilities, ILogger logger) {
        this.logger = logger;
        DiscoveryManager.init(context);
        mDiscoveryManager = DiscoveryManager.getInstance();
        mDiscoveryManager.registerDeviceService(CastService.class, CastDiscoveryProvider.class);
        mDiscoveryManager.setPairingLevel(DiscoveryManager.PairingLevel.ON);
        mDiscoveryManager.setServiceIntegration(true);
        mDiscoveryManager.addListener(this);
        mDiscoveryManager.setCapabilityFilters(new CapabilityFilter(capabilities));
        mDiscoveryManager.start();
    }

    public void start() {
        mDiscoveryManager.start();
    }

    public void stop() {
        mDiscoveryManager.stop();
    }

    public void rescan() {
        mDiscoveryManager.rescan();
    }

    public void setScanIntensity(DiscoveryProvider.ScanIntensity intensity) {
        mDiscoveryManager.setScanIntensity(intensity);
    }

    public void onDestory() {
        mDiscoveryManager.onDestroy();
    }

    @Override
    public void onDeviceAdded(DiscoveryManager manager, ConnectableDevice device) {
        this.logger.log(Log.INFO, TAG, "OnDeviceAdded " + device.getFriendlyName());
        int index = -1;
        for (int i = 0; i < availableDevices.size(); i++) {
            ConnectableDevice d = availableDevices.get(i);

            String newDeviceName = device.getFriendlyName();
            String dName = d.getFriendlyName();

            if (newDeviceName == null) {
                newDeviceName = device.getModelName();
            }

            if (dName == null) {
                dName = d.getModelName();
            }

            if (d.getIpAddress().equals(device.getIpAddress())) {
                if (d.getFriendlyName().equals(device.getFriendlyName())) {
                    if (!manager.isServiceIntegrationEnabled() && d.getServiceId().equals(device.getServiceId())) {
                        availableDevices.remove(d);
                        availableDevices.add(i, device);
                        return;
                    }
                }
            }

            if (newDeviceName.compareToIgnoreCase(dName) < 0) {
                index = i;
                availableDevices.add(index, device);
                break;
            }
        }

        if (index == -1) availableDevices.add(device);
        setDeviceAvailability();
    }

    @Override
    public void onDeviceUpdated(DiscoveryManager manager, ConnectableDevice device) {
        this.logger.log(Log.INFO, TAG, "onDeviceUpdated " + device.getFriendlyName());
        for (String capability : device.getCapabilities()) {
            this.logger.log(Log.INFO, TAG, capability);
        }
        setDeviceAvailability();
    }

    @Override
    public void onDeviceRemoved(DiscoveryManager manager, ConnectableDevice device) {
        this.logger.log(Log.INFO, TAG, "onDeviceRemoved " + device.getFriendlyName());
        for (String capability : device.getCapabilities()) {
            this.logger.log(Log.INFO, TAG, capability);
        }
        availableDevices.remove(device);
        setDeviceAvailability();
    }

    @Override
    public void onDiscoveryFailed(DiscoveryManager manager, ServiceCommandError error) {
        availableDevices.clear();
        this.logger.log(Log.INFO, TAG, "onDiscoveryFailed " + error.getMessage());
        setDeviceAvailability();
    }

    public void setDeviceAvailabilityListener(IDeviceAvailabilityListener listener) {
        this.availabilityListener = listener;
    }

    private void setDeviceAvailability() {
        boolean available = this.availableDevices.size() > 0;
        if (available != isDeviceAvailable) {
            isDeviceAvailable = available;
            if (this.availabilityListener != null) {
                this.availabilityListener.onAvailabilityChange(isDeviceAvailable);
            }
        }
    }

    public List<CastDevice> getAvailableDevices() {
        List<CastDevice> devices = new ArrayList<>();
        for (ConnectableDevice connectableDevice : availableDevices) {
            CastDevice device = CastDevice.fromConnectableDevice(connectableDevice);
            devices.add(device);
        }
        return devices;
    }

    public ConnectableDevice getConnectableDevice(CastDevice device) {
        if (device != null) {
            for (ConnectableDevice connectableDevice : availableDevices) {
                if (connectableDevice.getId() != null && connectableDevice.getId().equals(device.getId())) {
                    return connectableDevice;
                }
            }
        }
        return null;
    }
}
