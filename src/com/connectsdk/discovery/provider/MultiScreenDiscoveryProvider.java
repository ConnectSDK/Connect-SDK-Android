package com.connectsdk.discovery.provider;

import java.util.List;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.discovery.DiscoveryProviderListener;
import com.connectsdk.service.MultiScreenService;
import com.connectsdk.service.config.MultiScreenServiceDescription;
import com.connectsdk.service.config.ServiceDescription;
import com.samsung.multiscreen.device.Device;
import com.samsung.multiscreen.device.DeviceAsyncResult;
import com.samsung.multiscreen.device.DeviceError;

public class MultiScreenDiscoveryProvider implements DiscoveryProvider {
    private ConcurrentHashMap<String, ServiceDescription> services;
    private CopyOnWriteArrayList<DiscoveryProviderListener> serviceListeners;
    
	private final static int RESCAN_INTERVAL = 10000;
    private Timer dataTimer;

    public MultiScreenDiscoveryProvider(Context context) {
		services = new ConcurrentHashMap<String, ServiceDescription>(8, 0.75f, 2);
		serviceListeners = new CopyOnWriteArrayList<DiscoveryProviderListener>();
    }

    DeviceAsyncResult<List<Device>> discoveryListener = new DeviceAsyncResult<List<Device>>() {
		
    	@Override
		public void onResult(List<Device> devices) {
            Log.d(Util.T, "findLocalDevices() -> DONE: count: " + devices.size());
	
//            int i = 0;
//            for (Device device: devices) {
//            	System.out.println("[DEBUG] [" + i++ + "]");
//            	System.out.println("[DEBUG] ip: " + device.getIPAddress());
//            	System.out.println("[DEBUG] name: " + device.getName());
//            	System.out.println("[DEBUG] network type: " + device.getNetworkType());
//            	System.out.println("[DEBUG] ssid: " + device.getSSID());
//            	System.out.println("[DEBUG] id: " + device.getId());
//            	System.out.println("[DEBUG] =============");
//            }
            
//          handleLostDevices(devices);
            handleNewDevices(devices);
		}
		
		@Override
		public void onError(DeviceError error) {
            Log.d(Util.T, "Error: " + error.getMessage() + ", msg: " + error.getMessage());
			
		}
		
	};
    
	@Override
	public void start() {
		Device.search(discoveryListener);
//		dataTimer = new Timer();
//		dataTimer.schedule(new TimerTask() {
//			
//			@Override
//			public void run() {
//				Device.search(discoveryListener);
//			}
//		}, 100, RESCAN_INTERVAL);
	}

	@Override
	public void stop() {
//		Device.search(null);
	}

	@Override
	public void reset() {
		services.clear();
	}

	@Override
	public void addListener(DiscoveryProviderListener listener) {
		serviceListeners.add(listener);
	}

	@Override
	public void removeListener(DiscoveryProviderListener listener) {
		serviceListeners.remove(listener);
	}

	@Override
	public void addDeviceFilter(JSONObject parameters) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeDeviceFilter(JSONObject parameters) {
		// TODO Auto-generated method stub
		
	}
	
	private void handleLostDevices(List<Device> devices) {
//		for (ServiceDescription sd: services.){
//			
//		}
//		
//		for (Device device: devices) {
//			ServiceDescription sd = services.get(device.getId());
//			
//			if (sd != null) {
//				
//			}
//		}
	}
	
	private void handleNewDevices(List<Device> devices) {
		for (Device device: devices) { 
	    	String ipAddress = device.getIPAddress();
	        String uuid = device.getId();
	        String friendlyName = device.getName();
	        
	        String serviceFilter = "MultiScreen";

	        ServiceDescription oldService = services.get(uuid);

	        ServiceDescription newService;
	    	if (oldService == null) {
	            newService = new MultiScreenServiceDescription(serviceFilter, uuid, ipAddress, device);
	            newService.setFriendlyName(friendlyName);
	            newService.setServiceID(MultiScreenService.ID);
	            
	            services.put(uuid, newService);
	        }
	    	else {
	    		newService = oldService;

	    		newService.setIpAddress(ipAddress);
	            newService.setFriendlyName(friendlyName);
	            newService.setServiceID(MultiScreenService.ID);
	            ((MultiScreenServiceDescription)newService).setMultiScreenDevice(device);
	    		
	    		services.put(uuid, newService);
	    	}
	        
	    	for ( DiscoveryProviderListener listenter: serviceListeners) {
	    		listenter.onServiceAdded(MultiScreenDiscoveryProvider.this, newService);
	    	}
		}
	}

	@Override
	public boolean isEmpty() {
		return false;
	}
}
