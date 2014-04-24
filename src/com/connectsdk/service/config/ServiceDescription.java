/*
 * ServiceDescription
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.connectsdk.core.upnp.service.Service;

public class ServiceDescription {
	
	public static final String KEY_FILTER = "filter";
	public static final String KEY_IP_ADDRESS = "ipAddress";
	public static final String KEY_UUID = "uuid";
	public static final String KEY_FRIENDLY = "friendlyName";
	public static final String KEY_MODEL_NAME = "modelName";
	public static final String KEY_MODEL_NUMBER = "modelNumber";
	public static final String KEY_PORT = "port";
	public static final String KEY_VERSION = "version";
	public static final String KEY_SERVICE_ID = "serviceId";

	String UUID;
	String ipAddress;
	String friendlyName;
	String modelName;
	String modelNumber;
	String manufacturer;
	String modelDescription;
	String serviceFilter;
	int port;
	String applicationURL;
	String version;
	List<Service> serviceList; 
	String locationXML;
	
	Map<String, List<String>> responseHeaders;
	
	String serviceID;
	
	long lastDetection = Long.MAX_VALUE;
	
	public ServiceDescription() { }

	public ServiceDescription(String serviceFilter, String UUID, String ipAddress) {
		this.serviceFilter = serviceFilter;
		this.UUID = UUID;
		this.ipAddress = ipAddress;
	}
	
	public ServiceDescription(JSONObject json) {
		serviceFilter = json.optString(KEY_FILTER, null);
		ipAddress = json.optString(KEY_IP_ADDRESS, null);
		UUID = json.optString(KEY_UUID, null);
		friendlyName = json.optString(KEY_FRIENDLY, null);
		modelName = json.optString(KEY_MODEL_NAME, null);
		modelNumber = json.optString(KEY_MODEL_NUMBER, null);
		port = json.optInt(KEY_PORT, -1);
		version = json.optString(KEY_VERSION, null);
		serviceID = json.optString(KEY_SERVICE_ID, null);
	}
	
	public static ServiceDescription getDescription(JSONObject json) {
		return new ServiceDescription(json);
	}
	
	public String getServiceFilter() {
		return serviceFilter;
	}
	
	public void setServiceFilter(String serviceFilter) {
		this.serviceFilter = serviceFilter;
	}
	
	public String getUUID() {
		return UUID;
	}

	public void setUUID(String uUID) {
		UUID = uUID;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String getIpAddress) {
		this.ipAddress = getIpAddress;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public int getPort() {
		return port;
	}
	
	public String getFriendlyName() {
		return friendlyName;
	}

	public void setFriendlyName(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public String getModelNumber() {
		return modelNumber;
	}

	public void setModelNumber(String modelNumber) {
		this.modelNumber = modelNumber;
	}
	
	public String getManufacturer() {
		return manufacturer;
	}

	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}

	public String getModelDescription() {
		return modelDescription;
	}

	public void setModelDescription(String modelDescription) {
		this.modelDescription = modelDescription;
	}

	public void setServiceList(List<Service> serviceList) {
		this.serviceList = serviceList;
	}
	
	public String getApplicationURL() {
		return applicationURL;
	}

	public void setApplicationURL(String applicationURL) {
		this.applicationURL = applicationURL;
	}

	public List<Service> getServiceList() {
		return serviceList;
	}
	
	public long getLastDetection() {
		return lastDetection;
	}
	
	public void setLastDetection(long last) {
		lastDetection = last;
	}
	
	public String getServiceID() {
		return serviceID;
	}

	public void setServiceID(String serviceID) {
		this.serviceID = serviceID;
	}
	
	public Map<String, List<String>> getResponseHeaders() {
		return responseHeaders;
	}
	
	public void setResponseHeaders(Map<String, List<String>> responseHeaders) {
		this.responseHeaders = responseHeaders;
	}
	
	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
	
	public String getLocationXML() {
		return locationXML;
	}
	
	public void setLocationXML(String locationXML) {
		this.locationXML = locationXML;
	}

	public JSONObject toJSONObject() {
		JSONObject jsonObj = new JSONObject();
		
		try {
			jsonObj.putOpt(KEY_FILTER, serviceFilter);
			jsonObj.putOpt(KEY_IP_ADDRESS, ipAddress);
			jsonObj.putOpt(KEY_UUID, UUID);
			jsonObj.putOpt(KEY_FRIENDLY, friendlyName);
			jsonObj.putOpt(KEY_MODEL_NAME, modelName);
			jsonObj.putOpt(KEY_MODEL_NUMBER, modelNumber);
			jsonObj.putOpt(KEY_PORT, port);
			jsonObj.putOpt(KEY_VERSION, version);
			jsonObj.putOpt(KEY_SERVICE_ID, serviceID);
//			if (responseHeaders != null) {
//				jsonObj.putOpt("responseHeaders", new JSONObject() {{
//					for (final String key : responseHeaders.keySet()) {
//						putOpt(key, new JSONArray(){{
//							List<String> items = responseHeaders.get(key);
//							for (String item : items)
//								put(item);
//						}});
//					}
//				}});
//			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return jsonObj;
	}
}
