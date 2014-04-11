/*
 * DefaultConnectableDeviceStore
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics. All rights reserved.
 * Created by Hyun Kook Khang on 19 Jan 2014
 * 
 */

package com.connectsdk.device;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;

import com.connectsdk.core.Util;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.config.NetcastTVServiceConfig;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;
import com.connectsdk.service.config.WebOSTVServiceConfig;

public class DefaultConnectableDeviceStore implements ConnectableDeviceStore {
	// @cond INTERNAL
	static final String DIRPATH = "/android/data/connect_sdk/";
	static final String FILENAME = "StoredDevices";
	
	static final String IP_ADDRESS = "ipAddress";
	static final String FRIENDLY_NAME = "friendlyName";
	static final String MODEL_NAME = "modelName";
	static final String MODEL_NUMBER = "modelNumber";
	static final String SERVICES = "services";
	static final String DESCRIPTION = "description";
	static final String CONFIG = "config";
	
	static final String FILTER = "filter";
	static final String UUID = "uuid";
	static final String PORT = "port";

	static final String SERVICE_UUID = "serviceUUID";
	static final String CLIENT_KEY = "clientKey";
	static final String SERVER_CERTIFICATE = "serverCertificate";
	static final String PAIRING_KEY = "pairingKey";
	
	static final String DEFAULT_SERVICE_WEBOSTV = "WebOSTVService";
	static final String DEFAULT_SERVICE_NETCASTTV = "NetcastTVService";
	
	private List<ConnectableDevice> storedDevices;
	// @endcond

	/** Date (in seconds from 1970) that the ConnectableDeviceStore was created. */
	public long created;
	/** Date (in seconds from 1970) that the ConnectableDeviceStore was last updated. */
	public long updated;
	/** Current version of the ConnectableDeviceStore, may be necessary for migrations */
	public int version;
	
	/**
	 * Max length of time for a ConnectableDevice to remain in the ConnectableDeviceStore without being discovered. Default is 3 days, and modifications to this value will trigger a scan for old devices.
	 */
	public long maxStoreDuration = TimeUnit.DAYS.toSeconds(3);
	
	// @cond INTERNAL
	private String fileFullPath;
	
	private JSONObject deviceStore;
	
	private boolean waitToWrite = false;
	
	public DefaultConnectableDeviceStore(Context context) { 
		storedDevices = new CopyOnWriteArrayList<ConnectableDevice>();
		
		String dirPath;
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			dirPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		}
		else {
			dirPath = Environment.MEDIA_UNMOUNTED;
		}
		fileFullPath = dirPath + DIRPATH + FILENAME;

		try {
			fileFullPath = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).applicationInfo.dataDir + "/" + FILENAME;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		load();
	}
	// @endcond
	
	@Override
	public void addDevice(ConnectableDevice device) {
		storedDevices.add(device);
		store();
	}

	@Override
	public void removeDevice(ConnectableDevice device) {
		storedDevices.remove(device);
		store();
	}

	@Override
	public void updateDevice(ConnectableDevice newDevice) {
		boolean found = false;
		
		for (ConnectableDevice myDevice : storedDevices) {
			for (DeviceService newService : newDevice.getServices()) {
				ServiceConfig newConfig = newService.getServiceConfig();
				
				for (DeviceService oldService : myDevice.getServices()) {
					ServiceConfig oldConfig = oldService.getServiceConfig();
					
					if ( newConfig.getServiceUUID().equals(oldConfig.getServiceUUID()) ) {
						storedDevices.remove(myDevice);
						
						myDevice.setIpAddress(newDevice.getIpAddress());
						myDevice.setFriendlyName(newDevice.getFriendlyName());
						myDevice.setModelName(newDevice.getModelName());
						myDevice.setModelNumber(newDevice.getModelNumber());
						
						myDevice.addService(newService);
						
						found = true;
						break;
					}
				}
			}
			
			if ( found == true )  {
				storedDevices.add(myDevice);
			}
		}
		
		store();
	}
	
	@Override
	public void removeAll() {
		storedDevices.clear();
		store();
	}

	@Override
	public List<ConnectableDevice> getStoredDevices() {
		return storedDevices;
	}
	
	// @cond INTERNAL
	private void load() {
		String line;

		BufferedReader in = null;

		File file = new File(fileFullPath);

		if (!file.exists()) {
			version = 1;

			created = Util.getTime();
			updated = Util.getTime();
		} else {
			try {
				in = new BufferedReader(new FileReader(file));

				StringBuilder sb = new StringBuilder();

				while ((line = in.readLine()) != null) {
					sb.append(line);
				}
				
				in.close();
				
				deviceStore = new JSONObject(sb.toString());
				JSONArray deviceList = deviceStore.getJSONArray("devices");

				for (int i = 0; i < deviceList.length(); i++) {
					JSONObject device = deviceList.getJSONObject(i);
					
			        ConnectableDevice d = new ConnectableDevice();
			        d.setIpAddress(device.optString(IP_ADDRESS));
			        d.setFriendlyName(device.optString(FRIENDLY_NAME));
			        d.setModelName(device.optString(MODEL_NAME));
			        d.setModelNumber(device.optString(MODEL_NUMBER));
					
			        JSONArray jsonServices = device.optJSONArray(SERVICES); 

			        if (jsonServices != null) {
			        	for (int j = 0; j < jsonServices.length(); j++) {
			        		JSONObject jsonService = (JSONObject) jsonServices.optJSONObject(j);
			        		
			        		ServiceDescription sd = createServiceDescription(jsonService.optJSONObject(DESCRIPTION));

			        		ServiceConfig sc = createServiceConfig(jsonService.optJSONObject(CONFIG));
			        		
				        	DeviceService myService = new DeviceService(sd, sc, this);
				        	d.addService(myService);
			        	}
			        }
			        
			        storedDevices.add(d);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void store() {

		JSONArray deviceList = new JSONArray();

		for (ConnectableDevice d : storedDevices) {
			JSONObject device = d.toJSONObject();
			deviceList.put(device);
		}
		
		updated = Util.getTime();

		deviceStore = new JSONObject();
		
		try {
			deviceStore.put("version", version);
			deviceStore.put("created", created);
			deviceStore.put("updated", updated);
			deviceStore.put("devices", deviceList);
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
	
		if (!waitToWrite)
			writeStoreToDisk();
	}

	private void writeStoreToDisk() {
		final double lastUpdate = updated;
		waitToWrite = true;
		
		Util.runInBackground(new Runnable() {
			
			@Override
			public void run() {
		        FileWriter out;
				try {
					File output = new File(fileFullPath);
					
					if (!output.exists())
						output.getParentFile().mkdirs();
					
					out = new FileWriter(output);
					out.write(deviceStore.toString());
			        out.close();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					waitToWrite = false;
				}
				
				if (lastUpdate != updated)
					writeStoreToDisk();
			}
		});
	}
	
	private ServiceDescription createServiceDescription(JSONObject desc) {
		if (desc == null)
			return null;

	    ServiceDescription sd = new ServiceDescription();
	    
	   	sd.setServiceFilter(desc.optString(FILTER));
	   	sd.setIpAddress(desc.optString(IP_ADDRESS));
	   	sd.setUUID(desc.optString(UUID));
	   	sd.setModelName(desc.optString(MODEL_NAME));
	   	sd.setModelNumber(desc.optString(MODEL_NUMBER));
	   	sd.setFriendlyName(desc.optString(FRIENDLY_NAME));
	   	sd.setPort(desc.optInt(PORT));
	    
		return sd;
	}
	
	private ServiceConfig createServiceConfig(JSONObject config) {
		ServiceConfig sc = null;
		
		String uuid;
		
		try {
			uuid = config.getString("serviceUUID");
			
			if ( config.has("clientKey") ) {
				String clientKey = null;
				String cert = null;
				
				if ( config.has("clientKey") ) {
					clientKey = config.getString("clientKey");
				}
				if ( config.has("serverCertificate") ) {
					cert = config.getString("serverCertificate");
				}
				
				sc = new WebOSTVServiceConfig(uuid, clientKey);
	            // TODO importing serverCertificate has some issue, need to fix it later
//	        	sc = new WebOSTVServiceConfig(uuid, clientKey, serverCertificate);
			}
			else if ( config.has("pairingKey") ) {
				String pairingKey = config.getString("pairingKey");
				
				sc = new NetcastTVServiceConfig(uuid, pairingKey);
			}
			else {
				sc = new ServiceConfig(uuid);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return sc;
	}
	// @endcond
}
