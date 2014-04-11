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

public class ServiceConfig {
	String serviceUUID;
	
	public ServiceConfig(String serviceUUID) {
		this.serviceUUID = serviceUUID;
	}
	
	public String getServiceUUID() {
		return serviceUUID;
	}

	public String toString() { 
		return serviceUUID;
	}

	public JSONObject toJSONObject() {
		JSONObject jsonObj = new JSONObject();

		try {
			jsonObj.put("serviceUUID", serviceUUID);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return jsonObj;
	}
}
