/*
 * NetcastTVServiceConfig
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics. All rights reserved.
 * Created by Hyun Kook Khang on 19 Jan 2014
 * 
 */

package com.connectsdk.service.config;

import org.json.JSONException;
import org.json.JSONObject;

public class NetcastTVServiceConfig extends ServiceConfig {
	String pairingKey;

	public NetcastTVServiceConfig(String serviceUUID) {
		super(serviceUUID);
	}
	
	public NetcastTVServiceConfig(String serviceUUID, String pairingKey) {
		super(serviceUUID);
		this.pairingKey = pairingKey;
	}

	public String getPairingKey() {
		return pairingKey;
	}

	public void setPairingKey(String pairingKey) {
		this.pairingKey = pairingKey;
	}
	
	@Override
	public JSONObject toJSONObject() {
		JSONObject jsonObj = super.toJSONObject();

		try {
			jsonObj.put("pairingKey", pairingKey);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return jsonObj;
	}
	
}
