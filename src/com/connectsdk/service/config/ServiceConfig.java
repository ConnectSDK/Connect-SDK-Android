/*
 * ServiceConfig
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
			jsonObj.put("UUID", serviceUUID);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return jsonObj;
	}
}
