/*
 * ExternalAppInfo
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics. All rights reserved.
 * Created by Hyun Kook Khang on 19 Jan 2014
 * 
 */

package com.connectsdk.core;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Normalized reference object for information about a DeviceService's external inputs. This object is required to set a DeviceService's external input.
 */
public class ExternalInputInfo implements JSONSerializable {
	String id;
	String name;
	boolean connected;
	String iconURL;
	
	JSONObject rawData;

	/**
	 * Default constructor method.
	 */
	public ExternalInputInfo() {
	}

	/** Gets the ID of the external input on the first screen device. */
	public String getId() {
		return id;
	}
	
	/** Sets the ID of the external input on the first screen device. */
	public void setId(String inputId) {
		this.id = inputId;
	}

	/** Gets the user-friendly name of the external input (ex. AV, HDMI1, etc). */
	public String getName() {
		return name;
	}
	
	/** Sets the user-friendly name of the external input (ex. AV, HDMI1, etc). */
	public void setName(String inputName) {
		this.name = inputName;
	}
	
	/** Sets the raw data from the first screen device about the external input. */
	public void setRawData(JSONObject rawData) {
		this.rawData = rawData;
	}

	/** Gets the raw data from the first screen device about the external input. */
	public JSONObject getRawData() {
		return rawData;
	}
	
	/** Whether the DeviceService is currently connected to this external input. */
	public boolean isConnected() {
		return connected;
	}

	/** Sets whether the DeviceService is currently connected to this external input. */
	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	/** Gets the URL to an icon representing this external input. */
	public String getIconURL() {
		return iconURL;
	}

	/** Sets the URL to an icon representing this external input. */
	public void setIconURL(String iconURL) {
		this.iconURL = iconURL;
	}

	// @cond INTERNAL
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject obj = new JSONObject();
		
		obj.put("id", id);
		obj.put("name", name);
		obj.put("connected", connected);
		obj.put("icon", iconURL);
		obj.put("rawData", rawData);
		
		return obj;
	}
	// @endcond
	
	/**
	 * Compares two ExternalInputInfo objects.
	 *
	 * @param externalInputInfo ExternalInputInfo object to compare.
	 *
	 * @return YES if both ExternalInputInfo id & name values are equal
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof ExternalInputInfo) {
			ExternalInputInfo eii = (ExternalInputInfo) o;
			return this.id.equals(eii.id) &&
					this.name.equals(eii.name);
		}
		return false;
	}
}
