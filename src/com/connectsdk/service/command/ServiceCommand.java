/*
 * ServiceCommand
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics. All rights reserved.
 * Created by Hyun Kook Khang on 19 Jan 2014
 * 
 */

package com.connectsdk.service.command;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

import com.connectsdk.service.DeviceService;
import com.connectsdk.service.capability.listeners.ResponseListener;

/**
 * Internal implementation of ServiceCommand for URL-based commands
 */
public class ServiceCommand<T extends ResponseListener<? extends Object>> {
	public static final String TYPE_REQ = "request";
	public static final String TYPE_SUB = "subscribe";
	public static final String TYPE_GET = "GET";
	public static final String TYPE_POST = "POST";
	public static final String TYPE_DEL = "DELETE";

	DeviceService service;
    String httpMethod; // WebOSTV: {request, subscribe}, NetcastTV: {GET, POST}
    Object payload;
    String target;		
    
    ResponseListener<Object> responseListener;

    public ServiceCommand(DeviceService service, String targetURL, Object payload, ResponseListener<Object> listener) {
    	this.service = service;
    	this.target = targetURL;
    	this.payload = payload;
    	this.responseListener = listener;
    	this.httpMethod = TYPE_POST;
    }
    
	public void send() {
		service.sendCommand(this);
	}

	public DeviceService getDeviceService() {
		return service;
	}

	public void setDeviceService(DeviceService service) {
		this.service = service;
	}

	public Object getPayload() {
		return payload;
	}
	
	public void setPayload(Object payload) {
		this.payload = payload;
	}
	
	public String getHttpMethod() {
		return httpMethod;
	}
	
	public void setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}

	public String getTarget() { 
		return target;
	}
	
	public void setTarget(String target) {
		this.target = target;
	}
	
	public HttpRequestBase getRequest() {
		if (this.httpMethod.equalsIgnoreCase(TYPE_GET)) {
			return new HttpGet(target);
		} else if (this.httpMethod.equalsIgnoreCase(TYPE_POST)) {
			return new HttpPost(target);
		} else if (this.httpMethod.equalsIgnoreCase(TYPE_DEL)) {
			return new HttpDelete(target);
		} else {
			return null;
		}
	}
	

	public ResponseListener<Object> getResponseListener() {
		return responseListener;
	}
}