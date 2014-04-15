/*
 * ResponseListener
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics. All rights reserved.
 * Created by Hyun Kook Khang on 19 Jan 2014
 * 
 */

package com.connectsdk.service.capability.listeners;

/**
 * Generic asynchronous operation response success handler block. If there is any response data to be processed, it will be provided via the responseObject parameter.
 * 
 * @param responseObject Contains the output data as a generic object reference. This value may be any of a number of types as defined by T in subclasses of ResponseListener. It is also possible that responseObject will be nil for operations that don't require data to be returned (move mouse, send key code, etc).
 */
public interface ResponseListener<T> extends ErrorListener {
	
	/**
	 * Returns the success of the call of type T.
	 * 
	 * @param object Response object, can be any number of object types, depending on the protocol/capability/etc
	 */
	abstract public void onSuccess(T object);
}
