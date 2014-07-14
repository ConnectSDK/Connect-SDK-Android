/*
 * ConnectableDevice
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.DeviceService.DeviceServiceListener;
import com.connectsdk.service.DeviceService.PairingType;
import com.connectsdk.service.capability.ExternalInputControl;
import com.connectsdk.service.capability.KeyControl;
import com.connectsdk.service.capability.Launcher;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.MouseControl;
import com.connectsdk.service.capability.PowerControl;
import com.connectsdk.service.capability.TVControl;
import com.connectsdk.service.capability.TextInputControl;
import com.connectsdk.service.capability.ToastControl;
import com.connectsdk.service.capability.VolumeControl;
import com.connectsdk.service.capability.WebAppLauncher;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.config.ServiceDescription;

/**
 * ###Overview
 * ConnectableDevice serves as a normalization layer between your app and each of the device's services. It consolidates a lot of key data about the physical device and provides access to underlying functionality.
 *
 * ###In Depth
 * ConnectableDevice consolidates some key information about the physical device, including model name, friendly name, ip address, connected DeviceService names, etc. In some cases, it is not possible to accurately select which DeviceService has the best friendly name, model name, etc. In these cases, the values of these properties are dependent upon the order of DeviceService discovery.
 *
 * To be informed of any ready/pairing/disconnect messages from each of the DeviceService, you must set a listener.
 *
 * ConnectableDevice exposes capabilities that exist in the underlying DeviceServices such as TV Control, Media Player, Media Control, Volume Control, etc. These capabilities, when accessed through the ConnectableDevice, will be automatically chosen from the most suitable DeviceService by using that DeviceService's CapabilityPriorityLevel.
 */
public class ConnectableDevice implements DeviceServiceListener {
	// @cond INTERNAL
	public static final String KEY_ID = "id";
	public static final String KEY_LAST_IP = "lastKnownIPAddress";
	public static final String KEY_FRIENDLY = "friendlyName";
	public static final String KEY_MODEL_NAME = "modelName";
	public static final String KEY_MODEL_NUMBER = "modelNumber";
	public static final String KEY_LAST_SEEN = "lastSeenOnWifi";
	public static final String KEY_LAST_CONNECTED = "lastConnected";
	public static final String KEY_LAST_DETECTED = "lastDetection";
	public static final String KEY_SERVICES = "services";

	private String ipAddress;
	private String friendlyName;
	private String modelName;
	private String modelNumber;

	private String lastKnownIPAddress;
	private String lastSeenOnWifi;
	private long lastConnected;
	private long lastDetection;
	
	private String id;
	
	private ServiceDescription serviceDescription;
	
	CopyOnWriteArrayList<ConnectableDeviceListener> listeners = new CopyOnWriteArrayList<ConnectableDeviceListener>();
	
	Map<String, DeviceService> services;
	
	public boolean featuresReady = false;
	
	public ConnectableDevice() {
		services = new ConcurrentHashMap<String, DeviceService>();
	}

	public ConnectableDevice(String ipAddress, String friendlyName, String modelName, String modelNumber) {
		this();

		this.ipAddress = ipAddress;
		this.friendlyName = friendlyName;
		this.modelName = modelName;
		this.modelNumber = modelNumber;
	}
	
	public ConnectableDevice(ServiceDescription description) {
		this();

		update(description);
	}
	
	public ConnectableDevice(JSONObject json) {
		this();
		
		setId(json.optString(KEY_ID, null));
		setLastKnownIPAddress(json.optString(KEY_LAST_IP, null));
		setFriendlyName(json.optString(KEY_FRIENDLY, null));
		setModelName(json.optString(KEY_MODEL_NAME, null));
		setModelNumber(json.optString(KEY_MODEL_NUMBER, null));
		setLastSeenOnWifi(json.optString(KEY_LAST_SEEN, null));
		setLastConnected(json.optLong(KEY_LAST_CONNECTED, 0));
		setLastDetection(json.optLong(KEY_LAST_DETECTED, 0));
	}
	
	public static ConnectableDevice createFromConfigString(String ipAddress, String friendlyName, String modelName, String modelNumber) {
		return new ConnectableDevice(ipAddress, friendlyName, modelName, modelNumber);
	}
	
	public static ConnectableDevice createWithId(String id, String ipAddress, String friendlyName, String modelName, String modelNumber) {
		ConnectableDevice mDevice = new ConnectableDevice(ipAddress, friendlyName, modelName, modelNumber);
		mDevice.setId(id);
		
		return mDevice;
	}
	
	public ServiceDescription getServiceDescription() {
		return serviceDescription;
	}
	
	public void setServiceDescription(ServiceDescription serviceDescription) {
		this.serviceDescription = serviceDescription;
	}
	// @endcond
	
	/**
	 * Adds a DeviceService to the ConnectableDevice instance. Only one instance of each DeviceService type (webOS, Netcast, etc) may be attached to a single ConnectableDevice instance. If a device contains your service type already, your service will not be added.
	 * 
	 * @param service DeviceService to be added
	 */
	public void addService(DeviceService service) {
		final List<String> added = getMismatchCapabilities(service.getCapabilities(), getCapabilities());
		
		service.setListener(this);
		
		Util.runOnUI(new Runnable() {
			
			@Override
			public void run() {
				for (ConnectableDeviceListener listener : listeners)
					listener.onCapabilityUpdated(ConnectableDevice.this, added, new ArrayList<String>());
			}
		});

		services.put(service.getServiceName(), service);
	}

	/**
	 * Removes a DeviceService from the ConnectableDevice instance.
	 * 
	 * @param service DeviceService to be removed
	 */
	public void removeService(DeviceService service) {
		removeServiceWithId(service.getServiceName());
	}
	
	/**
	 * Removes a DeviceService from the ConnectableDevice instance.
	 * 
	 * @param serviceId ID of the DeviceService to be removed (DLNA, webOS TV, etc)
	 */
	public void removeServiceWithId(String serviceId) {
		DeviceService service = services.get(serviceId);
		
		if (service == null)
			return;
		
		service.disconnect();
		
		services.remove(serviceId);

		final List<String> removed = getMismatchCapabilities(service.getCapabilities(), getCapabilities());

		Util.runOnUI(new Runnable() {
			
			@Override
			public void run() {
				for (ConnectableDeviceListener listener : listeners)
					listener.onCapabilityUpdated(ConnectableDevice.this, new ArrayList<String>(), removed);
			}
		});
	}
	
	private synchronized List<String> getMismatchCapabilities(List<String> capabilities, List<String> allCapabilities) { 
		List<String> list = new ArrayList<String>();
		
		for (String cap: capabilities) {
			if ( !allCapabilities.contains(cap) ) {
				list.add(cap);
			}
		}
		
		return list;
	}
	
	/** Array of all currently discovered DeviceServices this ConnectableDevice has associated with it. */
	public Collection<DeviceService> getServices() {
		return services.values();
	}
	
	/**
	 * Obtains a service from the ConnectableDevice with the provided serviceName
	 *
	 * @param serviceName Service ID of the targeted DeviceService (webOS, Netcast, DLNA, etc)
	 * @return DeviceService with the specified serviceName or nil, if none exists
	 */
	public DeviceService getServiceByName(String serviceName) {
		for (DeviceService service : getServices()) {
			if (service.getServiceName().equals(serviceName)) {
				return service;
			}
		}
		
		return null;
	}
	
	/**
	 * Removes a DeviceService form the ConnectableDevice instance.  serviceName is used as the identifier because only one instance of each DeviceService type may be attached to a single ConnectableDevice instance.
	 * 
	 * @param serviceName Name of the DeviceService to be removed from the ConnectableDevice.
	 */
	public void removeServiceByName(String serviceName) {
		removeService(getServiceByName(serviceName));
	}
	
	/**
	 * Returns a DeviceService from the ConnectableDevice instance. serviceUUID is used as the identifier because only one instance of each DeviceService type may be attached to a single ConnectableDevice instance.
	 * 
	 * @param serviceUUID UUID of the DeviceService to be returned
	 */
	public DeviceService getServiceWithUUID(String serviceUUID) {
		for (DeviceService service : getServices()) {
			if (service.getServiceDescription().getUUID().equals(serviceUUID)) {
				return service;
			}
		}
		
		return null;
	}
	
	/**
	 * Adds the ConnectableDeviceListener to the list of listeners for this ConnectableDevice to receive certain events.
	 * 
	 * @param listener ConnectableDeviceListener to listen to device events (connect, disconnect, ready, etc)
	 */
	public void addListener(ConnectableDeviceListener listener) {
		if (listeners.contains(listener) == false) {
			listeners.add(listener);
		}
	}
	
	/**
	 * Clears the array of listeners and adds the provided `listener` to the array. If `listener` is null, the array will be empty.
	 * 
	 * @deprecated Since version 1.2.1, use addListener instead
	 * 
	 * @param listener ConnectableDeviceListener to listen to device events (connect, disconnect, ready, etc)
	 */
	public void setListener(ConnectableDeviceListener listener) {
		listeners = new CopyOnWriteArrayList<ConnectableDeviceListener>();
		
		if (listener != null)
			listeners.add(listener);
	}
	
	/**
	 * Removes a previously added ConenctableDeviceListener from the list of listeners for this ConnectableDevice.
	 * 
	 * @param listener ConnectableDeviceListener to be removed
	 */
	public void removeListener(ConnectableDeviceListener listener) {
		listeners.remove(listener);
	}
	
	public List<ConnectableDeviceListener> getListeners() {
		return listeners;
	}
	
	/**
	 * Enumerates through all DeviceServices and attempts to connect to each of them. When all of a ConnectableDevice's DeviceServices are ready to receive commands, the ConnectableDevice will send a onDeviceReady message to its listener.
	 *
	 * It is always necessary to call connect on a ConnectableDevice, even if it contains no connectable DeviceServices.
	 */
	public void connect() {
		for (DeviceService service : services.values()) {
			if (!service.isConnected()) {
				service.connect();
			}
		}
	}
	
	/**
	 * Enumerates through all DeviceServices and attempts to disconnect from each of them.
	 */
	public void disconnect() {
		for (DeviceService service: services.values()) {
			service.disconnect();
		}
		
		Util.runOnUI(new Runnable() {
			
			@Override
			public void run() {
				for (ConnectableDeviceListener listener : listeners)
					listener.onDeviceDisconnected(ConnectableDevice.this);
			}
		});
	}
	
	// @cond INTERNAL
	public boolean isConnected() {
		int connectedCount = 0;
		
		Iterator<DeviceService> iterator = services.values().iterator();
		
		while (iterator.hasNext()) {
			DeviceService service = iterator.next();
			
			if (!service.isConnectable()) {
				connectedCount++;
			} else {
				if (service.isConnected())
					connectedCount++;
			}
		}
		
		return connectedCount >= services.size();
	}
	// @endcond
	
	/**
	 * Whether the device has any DeviceServices that require an active connection (websocket, HTTP registration, etc)
	 */
	public boolean isConnectable() {
		for (DeviceService service: services.values()) {
			if ( service.isConnectable() )
				return true;
		}
		
		return false;
	}
	
	/** 
	 * Sends a pairing key to all discovered device services.
	 * 
	 * @param pairingKey Pairing key to send to services.
	 */
	public void sendPairingKey(String pairingKey) {
		for (DeviceService service: services.values()) {
			service.sendPairingKey(pairingKey);
		}
	}
	
	/** Explicitly cancels pairing on all services that require pairing. In some services, this will hide a prompt that is displaying on the device. */
	public void cancelPairing() {
		for (DeviceService service: services.values()) {
			service.cancelPairing();
		}
	}
	
	/** A combined list of all capabilities that are supported among the detected DeviceServices. */
	public synchronized List<String> getCapabilities() {
		List<String> caps = new ArrayList<String>();
		
		for (DeviceService service: services.values()) {
			for (String capability: service.getCapabilities()) {
				if ( !caps.contains(capability) ) {
					caps.add(capability);
				}
			}
		}
		
		return caps;
	}
	
	/**
	 * Test to see if the capabilities array contains a given capability. See the individual Capability classes for acceptable capability values.
	 *
	 * It is possible to append a wildcard search term `.Any` to the end of the search term. This method will return true for capabilities that match the term up to the wildcard.
	 *
	 * Example: `Launcher.App.Any`
	 *
	 * @param capability Capability to test against
	 */
	public boolean hasCapability(String capability) {
		boolean hasCap = false;

		for (DeviceService service: services.values()) {
			if ( service.hasCapability(capability) ) {
				hasCap = true;
				break;
			}
		}
		
		return hasCap;
	}
	
	/**
	 * Test to see if the capabilities array contains at least one capability in a given set of capabilities. See the individual Capability classes for acceptable capability values.
	 *
	 * See hasCapability: for a description of the wildcard feature provided by this method.
	 *
	 * @param capabilities Array of capabilities to test against
	 */
	public boolean hasAnyCapability(String... capabilities) {
		for (DeviceService service : services.values()) {
			if (service.hasAnyCapability(capabilities))
				return true;
		}
		
		return false;
	}

	/**
	 * Test to see if the capabilities array contains a given set of capabilities. See the individual Capability classes for acceptable capability values.
	 *
	 * See hasCapability: for a description of the wildcard feature provided by this method.
	 *
	 * @param capabilities Array of capabilities to test against
	 */
	public synchronized boolean hasCapabilities(List<String> capabilities) {
		String[] arr = new String[capabilities.size()];
		capabilities.toArray(arr);
		return hasCapabilities(arr);
	}
	
	/**
	 * Test to see if the capabilities array contains a given set of capabilities. See the individual Capability classes for acceptable capability values.
	 *
	 * See hasCapability: for a description of the wildcard feature provided by this method.
	 *
	 * @param capabilities Array of capabilities to test against
	 */
	public synchronized boolean hasCapabilities(String... capabilites) {
		boolean hasCaps = true;
		
		for (String capability : capabilites) {
			if (!hasCapability(capability)) {
				hasCaps = false;
				break;
			}
		}
		
		return hasCaps;
	}

	/** Accessor for highest priority Launcher object */
	public Launcher getLauncher() {
		Launcher foundLauncher = null;
		
		for (DeviceService service: services.values()) {
			if ( service.getAPI(Launcher.class) == null )
				continue;
			
			Launcher launcher = service.getAPI(Launcher.class);

			if ( foundLauncher == null ) {
				foundLauncher = launcher;
			}
			else {
				if ( launcher.getLauncherCapabilityLevel().getValue() > foundLauncher.getLauncherCapabilityLevel().getValue() ) {
					foundLauncher = launcher;
				}
			}
		}
		
		return foundLauncher;
	}

	/** Accessor for highest priority MediaPlayer object */
	public MediaPlayer getMediaPlayer() {
		MediaPlayer foundMediaPlayer = null;
		
		for (DeviceService service: services.values()) {
			if ( service.getAPI(MediaPlayer.class) == null )
				continue;
			
			MediaPlayer mediaPlayer = service.getAPI(MediaPlayer.class);

			if ( foundMediaPlayer == null ) {
				foundMediaPlayer = mediaPlayer;
			}
			else {
				if ( mediaPlayer.getMediaPlayerCapabilityLevel().getValue() > foundMediaPlayer.getMediaPlayerCapabilityLevel().getValue() ) {
					foundMediaPlayer = mediaPlayer;
				}
			}
		}
		
		return foundMediaPlayer;
	}

	/** Accessor for highest priority MediaControl object */
	public MediaControl getMediaControl() {
		MediaControl foundMediaControl = null;
		
		for (DeviceService service: services.values()) {
			if ( service.getAPI(MediaControl.class) == null )
				continue;
			
			MediaControl mediaControl = service.getAPI(MediaControl.class);

			if ( foundMediaControl == null ) {
				foundMediaControl = mediaControl;
			}
			else {
				if ( mediaControl.getMediaControlCapabilityLevel().getValue() > foundMediaControl.getMediaControlCapabilityLevel().getValue() ) {
					foundMediaControl = mediaControl;
				}
			}
		}
		
		return foundMediaControl;
	}

	/** Accessor for highest priority VolumeControl object */
	public VolumeControl getVolumeControl() {
		VolumeControl foundVolumeControl = null;
		
		for (DeviceService service: services.values()) {
			if ( service.getAPI(VolumeControl.class) == null )
				continue;
			
			VolumeControl volumeControl = service.getAPI(VolumeControl.class);

			if ( foundVolumeControl == null ) {
				foundVolumeControl = volumeControl;
			}
			else {
				if ( volumeControl.getVolumeControlCapabilityLevel().getValue() > foundVolumeControl.getVolumeControlCapabilityLevel().getValue() ) {
					foundVolumeControl = volumeControl;
				}
			}
		}
		
		return foundVolumeControl;
	}

	/** Accessor for highest priority WebAppLauncher object */
	public WebAppLauncher getWebAppLauncher() {
		WebAppLauncher foundWebAppLauncher = null;
		
		for (DeviceService service: services.values()) {
			if ( service.getAPI(WebAppLauncher.class) == null )
				continue;
			
			WebAppLauncher webAppLauncher = service.getAPI(WebAppLauncher.class);

			if ( foundWebAppLauncher == null ) {
				foundWebAppLauncher = webAppLauncher;
			}
			else {
				if ( webAppLauncher.getWebAppLauncherCapabilityLevel().getValue() > foundWebAppLauncher.getWebAppLauncherCapabilityLevel().getValue() ) {
					foundWebAppLauncher = webAppLauncher;
				}
			}
		}
		
		return foundWebAppLauncher;
	}

	/** Accessor for highest priority TVControl object */
	public TVControl getTVControl() {
		TVControl foundTVControl = null;
		
		for (DeviceService service: services.values()) {
			if ( service.getAPI(TVControl.class) == null )
				continue;
			
			TVControl tvControl = service.getAPI(TVControl.class);

			if ( foundTVControl == null ) {
				foundTVControl = tvControl;
			}
			else {
				if ( tvControl.getTVControlCapabilityLevel().getValue() > foundTVControl.getTVControlCapabilityLevel().getValue() ) {
					foundTVControl = tvControl;
				}
			}
		}
		
		return foundTVControl;
	}

	/** Accessor for highest priority ToastControl object */
	public ToastControl getToastControl() {
		ToastControl foundToastControl = null;
		
		for (DeviceService service: services.values()) {
			if ( service.getAPI(ToastControl.class) == null )
				continue;
			
			ToastControl toastControl = service.getAPI(ToastControl.class);

			if ( foundToastControl == null ) {
				foundToastControl = toastControl;
			}
			else {
				if ( toastControl.getToastControlCapabilityLevel().getValue() > foundToastControl.getToastControlCapabilityLevel().getValue() ) {
					foundToastControl = toastControl;
				}
			}
		}
		
		return foundToastControl;
	}

	/** Accessor for highest priority TextInputControl object */
	public TextInputControl getTextInputControl() {
		TextInputControl foundTextInputControl = null;
		
		for (DeviceService service: services.values()) {
			if ( service.getAPI(TextInputControl.class) == null )
				continue;
			
			TextInputControl textInputControl = service.getAPI(TextInputControl.class);

			if ( foundTextInputControl == null ) {
				foundTextInputControl = textInputControl;
			}
			else {
				if ( textInputControl.getTextInputControlCapabilityLevel().getValue() > foundTextInputControl.getTextInputControlCapabilityLevel().getValue() ) {
					foundTextInputControl = textInputControl;
				}
			}
		}
		
		return foundTextInputControl;
	}

	/** Accessor for highest priority MouseControl object */
	public MouseControl getMouseControl() {
		MouseControl foundMouseControl = null;
		
		for (DeviceService service: services.values()) {
			if ( service.getAPI(MouseControl.class) == null )
				continue;
			
			MouseControl mouseControl = service.getAPI(MouseControl.class);

			if ( foundMouseControl == null ) {
				foundMouseControl = mouseControl;
			}
			else {
				if ( mouseControl.getMouseControlCapabilityLevel().getValue() > foundMouseControl.getMouseControlCapabilityLevel().getValue() ) {
					foundMouseControl = mouseControl;
				}
			}
		}
		
		return foundMouseControl;
	}

	/** Accessor for highest priority ExternalInputControl object */
	public ExternalInputControl getExternalInputControl() {
		ExternalInputControl foundExternalInputControl = null;
		
		for (DeviceService service: services.values()) {
			if ( service.getAPI(ExternalInputControl.class) == null )
				continue;
			
			ExternalInputControl externalInputControl = service.getAPI(ExternalInputControl.class);

			if ( foundExternalInputControl == null ) {
				foundExternalInputControl = externalInputControl;
			}
			else {
				if ( externalInputControl.getExternalInputControlPriorityLevel().getValue() > foundExternalInputControl.getExternalInputControlPriorityLevel().getValue() ) {
					foundExternalInputControl = externalInputControl;
				}
			}
		}
		
		return foundExternalInputControl;
	}

	/** Accessor for highest priority PowerControl object */
	public PowerControl getPowerControl() {
		PowerControl foundPowerControl = null;
		
		for (DeviceService service: services.values()) {
			if ( service.getAPI(PowerControl.class) == null )
				continue;
			
			PowerControl powerControl = service.getAPI(PowerControl.class);

			if ( foundPowerControl == null ) {
				foundPowerControl = powerControl;
			}
			else {
				if ( powerControl.getPowerControlCapabilityLevel().getValue() > foundPowerControl.getPowerControlCapabilityLevel().getValue() ) {
					foundPowerControl = powerControl;
				}
			}
		}
		
		return foundPowerControl;
	}

	/** Accessor for highest priority KeyControl object */
	public KeyControl getKeyControl() {
		KeyControl foundKeyControl = null;
		
		for (DeviceService service: services.values()) {
			if ( service.getAPI(KeyControl.class) == null )
				continue;
			
			KeyControl keyControl = service.getAPI(KeyControl.class);

			if ( foundKeyControl == null ) {
				foundKeyControl = keyControl;
			}
			else {
				if ( keyControl.getKeyControlCapabilityLevel().getValue() > foundKeyControl.getKeyControlCapabilityLevel().getValue() ) {
					foundKeyControl = keyControl;
				}
			}
		}
		
		return foundKeyControl;
	}
	
	/** 
	 * Sets the IP address of the ConnectableDevice.
	 * 
	 * @param ipAddress IP address of the ConnectableDevice
	 */
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	
	/** Gets the Current IP address of the ConnectableDevice. */
	public String getIpAddress() {
		return ipAddress;
	}

	/**
	 * Sets an estimate of the ConnectableDevice's current friendly name.
	 * 
	 * @param friendlyName Friendly name of the device
	 */
	public void setFriendlyName(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	/** Gets an estimate of the ConnectableDevice's current friendly name. */
	public String getFriendlyName() {
		return friendlyName;
	}

	/**
	 * Sets the last IP address this ConnectableDevice was discovered at.
	 *
	 * @param lastKnownIPAddress Last known IP address of the device & it's services
	 */
	public void setLastKnownIPAddress(String lastKnownIPAddress) {
		this.lastKnownIPAddress = lastKnownIPAddress;
	}
	
	/** Gets the last IP address this ConnectableDevice was discovered at. */
	public String getLastKnownIPAddress() {
		return lastKnownIPAddress;
	}

	/**
	 * Sets the name of the last wireless network this ConnectableDevice was discovered on.
	 * 
	 * @param lastSeenOnWifi Last Wi-Fi network this device & it's services were discovered on
	 */
	public void setLastSeenOnWifi(String lastSeenOnWifi) {
		this.lastSeenOnWifi = lastSeenOnWifi;
	}

	/** Gets the name of the last wireless network this ConnectableDevice was discovered on. */
	public String getLastSeenOnWifi() {
		return lastSeenOnWifi;
	}

	/**
	 * Sets the last time (in milli seconds from 1970) that this ConnectableDevice was connected to.
	 * 
	 * @param lastConnected Last connected time 
	 */
	public void setLastConnected(long lastConnected) {
		this.lastConnected = lastConnected;
	}

	/** Gets the last time (in milli seconds from 1970) that this ConnectableDevice was connected to. */
	public long getLastConnected() {
		return lastConnected;
	}

	/**
	 * Sets the last time (in milli seconds from 1970) that this ConnectableDevice was detected.
	 * 
	 * @param lastDetection Last detected time
	 */
	public void setLastDetection(long lastDetection) {
		this.lastDetection = lastDetection;
	}

	/** Gets the last time (in milli seconds from 1970) that this ConnectableDevice was detected. */
	public long getLastDetection() {
		return lastDetection;
	}

	/**
	 * Sets an estimate of the ConnectableDevice's current model name.
	 * 
	 * @param modelName Model name of the ConnectableDevice
	 */
	public void setModelName(String modelName) {
		this.modelName = modelName;
	}
	
	/** Gets an estimate of the ConnectableDevice's current model name. */
	public String getModelName() {
		return modelName;
	}

	/**
	 * Sets an estimate of the ConnectableDevice's current model number.
	 * 
	 * @param modelNumber Model number of the ConnectableDevice
	 * */
	public void setModelNumber(String modelNumber) {
		this.modelNumber = modelNumber;
	}
	
	/** Gets an estimate of the ConnectableDevice's current model number. */
	public String getModelNumber() {
		return modelNumber;
	}
	
	/**
	 * Sets the universally unique id of this particular ConnectableDevice object. This is used internally in the SDK and should not be used.
	 * @param id New id for the ConnectableDevice
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/** 
	 * Universally unique id of this particular ConnectableDevice object, persists between sessions in ConnectableDeviceStore for connected devices
	 */
	public String getId() {
		if (this.id == null)
			this.id = java.util.UUID.randomUUID().toString();

		return this.id;
	}

	// @cond INTERNAL
	public String getConnectedServiceNames() {
		int serviceCount = getServices().size();
		
		if (serviceCount <= 0)
			return null;
		
		String[] serviceNames = new String[serviceCount];
		int serviceIndex = 0;
		
		for (DeviceService service : getServices()) {
			serviceNames[serviceIndex] = service.getServiceName();
			
			serviceIndex++;
		}
		
		// credit: http://stackoverflow.com/a/6623121/2715
		StringBuilder sb = new StringBuilder();
		
		for (String serviceName : serviceNames) { 
			if (sb.length() > 0)
				sb.append(", ");
			
			sb.append(serviceName);
		}
		
		return sb.toString();
		////
	}
	
	public void update(ServiceDescription description) {
		setIpAddress(description.getIpAddress());
		setFriendlyName(description.getFriendlyName());
		setModelName(description.getModelName());
		setModelNumber(description.getModelNumber());
		setLastConnected(description.getLastDetection());
	}

	public JSONObject toJSONObject() {
		JSONObject deviceObject = new JSONObject();
		
		try {
			deviceObject.put(KEY_ID, getId());
			deviceObject.put(KEY_LAST_IP, getIpAddress());
			deviceObject.put(KEY_FRIENDLY, getFriendlyName());
			deviceObject.put(KEY_MODEL_NAME, getModelName());
			deviceObject.put(KEY_MODEL_NUMBER, getModelNumber());
			deviceObject.put(KEY_LAST_SEEN, getLastSeenOnWifi());
			deviceObject.put(KEY_LAST_CONNECTED, getLastConnected());
			deviceObject.put(KEY_LAST_DETECTED, getLastDetection());
			
			JSONObject jsonServices = new JSONObject();
			for (DeviceService service: services.values()) {
				JSONObject serviceObject = service.toJSONObject();
				
				jsonServices.put(service.getServiceConfig().getServiceUUID(), serviceObject);
			}
			deviceObject.put(KEY_SERVICES, jsonServices);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return deviceObject;
	}
	
	public String toString() {
		return toJSONObject().toString();
	}
	
	@Override
	public void onCapabilitiesUpdated(DeviceService service, List<String> added, List<String> removed) {
		DiscoveryManager.getInstance().onCapabilityUpdated(this, added, removed);
	}

	@Override public void onConnectionFailure(DeviceService service, Error error) {
	} 

	@Override public void onConnectionRequired(DeviceService service) {
	} 

	@Override
	public void onConnectionSuccess(DeviceService service) {
		//  TODO: iOS is passing to a function for when each service is ready on a device.  This is not implemented on Android.
		
		if (isConnected()) {
			ConnectableDeviceStore deviceStore = DiscoveryManager.getInstance().getConnectableDeviceStore();
			if (deviceStore != null) {
				deviceStore.addDevice(this);
			}
			
			Util.runOnUI(new Runnable() {
				
				@Override
				public void run() {
					for (ConnectableDeviceListener listener : listeners)
						listener.onDeviceReady(ConnectableDevice.this);
				}
			});

			setLastConnected(Util.getTime());
		}
	} 

	@Override
	public void onDisconnect(DeviceService service, Error error) {
		if (getConnectedServiceCount() == 0 || services.size() == 0) {
			for (ConnectableDeviceListener listener : listeners) {
				listener.onDeviceDisconnected(this);
			}
		}
	}

	@Override
	public void onPairingFailed(DeviceService service, Error error) {
		for (ConnectableDeviceListener listener : listeners)
			listener.onConnectionFailed(this, new ServiceCommandError(0, "Failed to pair with service " + service.getServiceName(), null));
	} 

	@Override
	public void onPairingRequired(DeviceService service, PairingType pairingType, Object pairingData) {
		for (ConnectableDeviceListener listener : listeners)
			listener.onPairingRequired(this, service, pairingType);
	} 

	@Override public void onPairingSuccess(DeviceService service) {
	}
	
	private int getConnectedServiceCount() {
		int count = 0;
		
		for (DeviceService service : services.values()) {
			if (service.isConnectable()) {
				if (service.isConnected())
					count++;
			} else {
				count++;
			}
		}
		
		return count;
	}
	
	// @endcond
}
