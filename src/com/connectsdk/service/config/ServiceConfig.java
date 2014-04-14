/*
 * ServiceConfig
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics. All rights reserved.
 * Created by Hyun Kook Khang on 19 Jan 2014
 * 
 */

package com.connectsdk.service.config;

import org.json.JSONException;
import org.json.JSONObject;

import com.connectsdk.core.Util;

public class ServiceConfig {
	private String serviceUUID;
	private long lastDetected = Long.MAX_VALUE;
	
	public ServiceConfig(String serviceUUID) {
		this.serviceUUID = serviceUUID;
	}
	
	public String getServiceUUID() {
		return serviceUUID;
	}

	public String toString() { 
		return serviceUUID;
	}
	
	public long getLastDetected() {
		return lastDetected;
	}
	
	public void setLastDetected(long value) {
		lastDetected = value;
	}
	
	public void detect() {
		lastDetected = Util.getTime();
	}

	public JSONObject toJSONObject() {
		JSONObject jsonObj = new JSONObject();

		try {
			jsonObj.put("class", this.getClass().toString());
			jsonObj.put("lastDetection", lastDetected);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return jsonObj;
	}
}
