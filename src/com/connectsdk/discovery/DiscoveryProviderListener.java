/*
 * DiscoveryProviderListener
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics. All rights reserved.
 * Created by Hyun Kook Khang on 19 Jan 2014
 * 
 */

package com.connectsdk.discovery;

import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.config.ServiceDescription;

/**
 * The DiscoveryProviderListener is mechanism for passing service information to the DiscoveryManager. You likely will not be using the DiscoveryProviderListener class directly, as DiscoveryManager acts as a listener to all of the DiscoveryProviders.
 */
public interface DiscoveryProviderListener {
	
	/**
	 * This method is called when the DiscoveryProvider discovers a service that matches one of its DeviceService filters. The ServiceDescription is created and passed to the listener (which should be the DiscoveryManager). The ServiceDescription is used to create a DeviceService, which is then attached to a ConnectableDevice object.
	 *
	 * @param provider DiscoveryProvider that found the service
	 * @param description ServiceDescription of the service that was found
	 */
	public void onServiceAdded(DiscoveryProvider provider, ServiceDescription serviceDescription);
	
	/**
	 * This method is called when the DiscoveryProvider's internal mechanism loses reference to a service that matches one of its DeviceService filters.
	 *
	 * @param provider DiscoveryProvider that lost the service
	 * @param description ServiceDescription of the service that was lost
	 */
	public void onServiceRemoved(DiscoveryProvider provider, ServiceDescription serviceDescription);
	
	/**
	 * This method is called on any error/failure within the DiscoveryProvider.
	 *
	 * @param provider DiscoveryProvider that failed
	 * @param error ServiceCommandError providing a information about the failure
	 */
	public void onServiceDiscoveryFailed(DiscoveryProvider provider, ServiceCommandError error);
}
