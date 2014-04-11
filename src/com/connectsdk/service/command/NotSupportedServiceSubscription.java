package com.connectsdk.service.command;

import java.util.ArrayList;
import java.util.List;


public class NotSupportedServiceSubscription<T> implements ServiceSubscription<T> {
	private List<T> listeners = new ArrayList<T>();

	@Override
	public void unsubscribe() {
	}
	
	@Override
	public T addListener(T listener) {
		listeners.add(listener);

		return listener;
	}
	
	@Override
	public List<T> getListeners() {
		return listeners;
	}
	
	@Override
	public void removeListener(T listener) {
		listeners.remove(listener);
	}
}
