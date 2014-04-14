/*
 * ServiceDescription
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics. All rights reserved.
 * Created by Hyun Kook Khang on 19 Jan 2014
 * 
 */

package com.connectsdk.service.config;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.connectsdk.core.upnp.service.Service;

public class ServiceDescription {
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
	
	Map<String, List<String>> responseHeaders;
	
	String serviceID;
	
	long lastDetection = Long.MAX_VALUE;
	
	public ServiceDescription() { }

	public ServiceDescription(String serviceFilter, String UUID, String ipAddress) {
		this.serviceFilter = serviceFilter;
		this.UUID = UUID;
		this.ipAddress = ipAddress;
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

	public JSONObject toJSONObject() {
		JSONObject jsonObj = new JSONObject();
		
		try {
			jsonObj.putOpt("filter", serviceFilter);
			jsonObj.putOpt("ipAddress", ipAddress);
			jsonObj.putOpt("uuid", UUID);
			jsonObj.putOpt("friendlyName", friendlyName);
			jsonObj.putOpt("modelName", modelName);
			jsonObj.putOpt("modelNumber", modelNumber);
			jsonObj.putOpt("port", port);
			jsonObj.putOpt("version", version);
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
