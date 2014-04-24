/*
 * DefaultConnectableDeviceStore
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 19 Jan 2014
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.connectsdk.device;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;

import com.connectsdk.core.Util;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.config.ServiceConfig;

public class DefaultConnectableDeviceStore implements ConnectableDeviceStore {
	// @cond INTERNAL
	
	public static final String KEY_VERSION = "version";
	public static final String KEY_CREATED = "created";
	public static final String KEY_UPDATED = "updated";
	public static final String KEY_DEVICES = "devices";
	
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
	private JSONObject storedDevices;
	private Map<String, ConnectableDevice> activeDevices = new HashMap<String, ConnectableDevice>();
	
	private boolean waitToWrite = false;
	
	public DefaultConnectableDeviceStore(Context context) { 
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
		if (device == null || device.getServices().size() == 0)
			return;

		if (!activeDevices.containsKey(device.getUUID()))
			activeDevices.put(device.getUUID(), device);
		
		JSONObject storedDevice = storedDevices.optJSONObject(device.getUUID());
		
		if (storedDevice != null) {
			updateDevice(device);
		} else {
			try {
				storedDevices.put(device.getUUID(), device.toJSONObject());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			store();
		}
	}

	@Override
	public void removeDevice(ConnectableDevice device) {
		if (device == null)
			return;

		activeDevices.remove(device.getUUID());
		storedDevices.remove(device.getUUID());

		store();
	}

	@Override
	public void updateDevice(ConnectableDevice device) {
		if (device == null || device.getServices().size() == 0)
			return;

		JSONObject storedDevice = getStoredDevice(device.getUUID());
		
		if (storedDevice == null)
			return;
		
		try {
			storedDevice.put(ConnectableDevice.KEY_LAST_IP, device.getLastKnownIPAddress());
			storedDevice.put(ConnectableDevice.KEY_LAST_SEEN, device.getLastSeenOnWifi());
			storedDevice.put(ConnectableDevice.KEY_LAST_CONNECTED, device.getLastConnected());
			storedDevice.put(ConnectableDevice.KEY_LAST_DETECTED, device.getLastDetection());
			
			JSONObject services = storedDevice.optJSONObject(ConnectableDevice.KEY_SERVICES);
			
			if (services == null)
				services = new JSONObject();
			
			for (DeviceService service : device.getServices()) {
				JSONObject serviceInfo = service.toJSONObject();
	
				if (serviceInfo != null)
					services.put(service.getServiceDescription().getUUID(), serviceInfo);
			}
			
			storedDevice.put(ConnectableDevice.KEY_SERVICES, services);
			
			storedDevices.put(device.getUUID(), storedDevice);
			activeDevices.put(device.getUUID(), device);

			store();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void removeAll() {
		activeDevices.clear();
		storedDevices = new JSONObject();

		store();
	}

	@Override
	public JSONObject getStoredDevices() {
		return storedDevices;
	}
	
	@Override
	public ConnectableDevice getDevice(String uuid) {
		if (uuid == null || uuid.length() == 0)
			return null;
		
		ConnectableDevice foundDevice = getActiveDevice(uuid);
		
		if (foundDevice == null) {
			JSONObject foundDeviceInfo = getStoredDevice(uuid);
			
			if (foundDeviceInfo != null)
				foundDevice = new ConnectableDevice(foundDeviceInfo);
		}

		return foundDevice;
	}
	
	private ConnectableDevice getActiveDevice(String uuid) {
		ConnectableDevice foundDevice = activeDevices.get(uuid);
		
		if (foundDevice == null) {
			for (ConnectableDevice device : activeDevices.values()) {
				for (DeviceService service : device.getServices()) {
					if (uuid.equals(service.getServiceDescription().getUUID())) {
						return foundDevice;
					}
				}
			}
		}
		return foundDevice;
	}
	
	private JSONObject getStoredDevice(String uuid) {
		JSONObject foundDevice = storedDevices.optJSONObject(uuid);

		if (foundDevice == null) {
			@SuppressWarnings("unchecked")
			Iterator<String> iter = storedDevices.keys();
			while (iter.hasNext()) {
				String key = iter.next();
				JSONObject device = storedDevices.optJSONObject(key);
				
				JSONObject services = device.optJSONObject(ConnectableDevice.KEY_SERVICES);
				
				if (services != null && services.has(uuid))
					return device;
			}
		}
		return foundDevice;
	}
	
	@Override
	public ServiceConfig getServiceConfig(String uuid) {
		if (uuid == null || uuid.length() == 0)
			return null;
		
		JSONObject device = getStoredDevice(uuid);
		if (device != null) {
			JSONObject services = device.optJSONObject(ConnectableDevice.KEY_SERVICES);
			if (services != null) {
				JSONObject service = services.optJSONObject(uuid);
				if (service != null) {
					JSONObject serviceConfigInfo = service.optJSONObject(DeviceService.KEY_CONFIG);
					if (serviceConfigInfo != null) {
						return ServiceConfig.getConfig(serviceConfigInfo);
					}
				}
			}
		}

		return null;
	}
	
	// @cond INTERNAL
	private void load() {
		String line;

		BufferedReader in = null;

		File file = new File(fileFullPath);

		if (!file.exists()) {
			version = 0;

			created = Util.getTime();
			updated = Util.getTime();
			
			storedDevices = new JSONObject();
		} else {
			try {
				in = new BufferedReader(new FileReader(file));

				StringBuilder sb = new StringBuilder();

				while ((line = in.readLine()) != null) {
					sb.append(line);
				}
				
				in.close();
				
				JSONObject data = new JSONObject(sb.toString());
				storedDevices = data.optJSONObject(KEY_DEVICES);
				if (storedDevices == null)
					storedDevices = new JSONObject();

				version = data.optInt(KEY_VERSION, 1);
				created = data.optLong(KEY_CREATED, 0);
				updated = data.optLong(KEY_UPDATED, 0);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void store() {

		updated = Util.getTime();

		deviceStore = new JSONObject();
		try {
			deviceStore.put(KEY_VERSION, version);
			deviceStore.put(KEY_CREATED, created);
			deviceStore.put(KEY_UPDATED, updated);
			deviceStore.put(KEY_DEVICES, storedDevices);
		} catch (JSONException e) {
			e.printStackTrace();
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
	// @endcond
}
