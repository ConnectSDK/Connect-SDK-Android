/*
 * ServiceSubscription
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics. All rights reserved.
 * Created by Hyun Kook Khang on 19 Jan 2014
 * 
 */

package com.connectsdk.service.command;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.connectsdk.service.DeviceService;
import com.connectsdk.service.capability.listeners.ResponseListener;

/**
 * Internal implementation of ServiceSubscription for URL-based commands
 */
public class URLServiceSubscription<T extends ResponseListener<?>> extends ServiceCommand<T> implements ServiceSubscription<T> {
	private List<T> listeners = new ArrayList<T>();

	public URLServiceSubscription(DeviceService service, String uri, JSONObject payload, ResponseListener<Object> listener) {
		super(service, uri, payload, listener);
	}
		
	public void send() {
		this.subscribe();
	}
	
	public void subscribe() {
		if ( !(httpMethod.equalsIgnoreCase(TYPE_GET)
				|| httpMethod.equalsIgnoreCase(TYPE_POST)) ) {
			httpMethod = "subscribe";
		}
		service.sendCommand(this);
	}
	
	public void unsubscribe() {
		service.unsubscribe(this);
	}
	
	public T addListener(T listener) {
		listeners.add(listener);
		
		return listener;
	}
	
	public void removeListener(T listener) {
		listeners.remove(listener);
	}
	
	public List<T> getListeners() {
		return listeners;
	}
}
