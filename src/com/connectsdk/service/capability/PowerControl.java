/*
 * PowerControl
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics. All rights reserved.
 * Created by Hyun Kook Khang on 19 Jan 2014
 * 
 */

package com.connectsdk.service.capability;

import com.connectsdk.service.capability.listeners.ResponseListener;

public interface PowerControl extends CapabilityMethods {
	public final static String Any = "PowerControl.Any";

	public final static String Off = "PowerControl.Off";

	public final static String[] Capabilities = {
	    Off
	};

	public PowerControl getPowerControl();
	public CapabilityPriorityLevel getPowerControlCapabilityLevel();

	public void powerOff(ResponseListener<Object> listener);
}
