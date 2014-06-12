/*
 * AirPlayDiscoveryProvider
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 18 Apr 2014
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

package com.connectsdk.discovery.provider;

import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdManager.DiscoveryListener;
import android.net.nsd.NsdManager.ResolveListener;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.discovery.DiscoveryProviderListener;
import com.connectsdk.service.AirPlayService;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.config.ServiceDescription;

public class ZeroconfDiscoveryProvider implements DiscoveryProvider {
	public static String TAG = "Connect SDK";
	
    private NsdManager mNsdManager;
    
    List<JSONObject> serviceFilters;
    
    private DiscoveryListener mDnsListener;
    private ResolveListener mResolveFoundListener;
    private ResolveListener mResolveLostListener;

    private ConcurrentHashMap<String, ServiceDescription> services;
    private CopyOnWriteArrayList<DiscoveryProviderListener> serviceListeners;
    
	public ZeroconfDiscoveryProvider(Context context) {
		services = new ConcurrentHashMap<String, ServiceDescription>(8, 0.75f, 2);
		serviceListeners = new CopyOnWriteArrayList<DiscoveryProviderListener>();
		serviceFilters = new ArrayList<JSONObject>();

		mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
		initListener();
	}
	
	@Override
	public void start() {
		Iterator<JSONObject> iterator = serviceFilters.iterator();
		
		while (iterator.hasNext()) {
			JSONObject filter = iterator.next();
			String filterName = null;
			
			try {
				filterName = filter.getString("filter");
			} catch (JSONException ex) {
				// do nothing
			}
			
			if (filterName == null)
				continue;
			
			mNsdManager.discoverServices(filterName, NsdManager.PROTOCOL_DNS_SD, mDnsListener);
		}
	}

	@Override
	public void stop() {
		mNsdManager.stopServiceDiscovery(mDnsListener);
	}

	@Override
	public void reset() {
		services.clear();
	}

	@Override
	public void addListener(DiscoveryProviderListener listener) {
		serviceListeners.add(listener);
	}

	@Override
	public void removeListener(DiscoveryProviderListener listener) {
		serviceListeners.remove(listener);
	}

	@Override
	public void addDeviceFilter(JSONObject parameters) {
		if (!parameters.has("filter")) {
			Log.e("Connect SDK", "This device filter does not have zeroconf filter info");
		} else {
			serviceFilters.add(parameters);
		}		
	}

	@Override
	public void removeDeviceFilter(JSONObject parameters) {
		String removalServiceId;
		boolean shouldRemove = false;
		int removalIndex = -1;
		
		try {
			removalServiceId = parameters.getString("serviceId");
			
			for (int i = 0; i < serviceFilters.size(); i++) {
				JSONObject serviceFilter = serviceFilters.get(i);
				String serviceId = (String) serviceFilter.get("serviceId");
				
				if ( serviceId.equals(removalServiceId) ) {
					shouldRemove = true;
					removalIndex = i;
					break;
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		if (shouldRemove) {
			serviceFilters.remove(removalIndex);
		}		
	}

	@Override
	public boolean isEmpty() {
		return serviceFilters.size() == 0;
	}
	
	private void initListener() {
		mResolveFoundListener = new ResolveListener() {
			
			@Override
			public void onServiceResolved(NsdServiceInfo serviceInfo) {
				if (!(serviceInfo.getHost() instanceof Inet4Address)) {
					// Currently, we only support ipv4 for airplay service
					return;
				}
				
				String ipAddress = serviceInfo.getHost().toString();
				
				if (ipAddress.charAt(0) == '/')
					ipAddress = ipAddress.substring(1);
				
	            String uuid = ipAddress;
	            String friendlyName = serviceInfo.getServiceName();
	            
	            try {
	                byte[] utf8 = friendlyName.getBytes("UTF-8");
	                friendlyName = new String(utf8, "UTF-8");
	            } catch (UnsupportedEncodingException e) { }
	            
	            int port = serviceInfo.getPort();
	            
	            Log.d(TAG, "Zeroconf found device " + friendlyName + " with service type " + serviceInfo.getServiceType());
	            
	            ServiceDescription oldService = services.get(uuid);

	            ServiceDescription newService;
	        	if ( oldService == null ) {
	                newService = new ServiceDescription(serviceInfo.getServiceType(), uuid, ipAddress);
		        }
	        	else {
	        		newService = oldService;
	        		newService.setIpAddress(ipAddress);
	        	}
	        	
	            newService.setServiceID(AirPlayService.ID);
	            newService.setFriendlyName(friendlyName);
	            newService.setPort(port);
	            
	            services.put(uuid, newService);
	            
	        	for ( DiscoveryProviderListener listener: serviceListeners) {
	        		listener.onServiceAdded(ZeroconfDiscoveryProvider.this, newService);
	        	}
			}
			
			@Override
			public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) { }
		};
		
		mResolveLostListener = new ResolveListener() {
			
			@Override
			public void onServiceResolved(NsdServiceInfo serviceInfo) {
				String ipAddress = serviceInfo.getHost().toString();
				
				if (ipAddress.charAt(0) == '/')
					ipAddress = ipAddress.substring(1);
				
	        	ServiceDescription service = services.get(ipAddress);
	        	
	        	if ( service != null ) {
	        		services.remove(ipAddress);
	        		
	        		Log.d(TAG, "Zeroconf lost device " + serviceInfo.getServiceName() + " with service type " + serviceInfo.getServiceType());
	        	            	
	        		for ( DiscoveryProviderListener listener: serviceListeners) {
	            		listener.onServiceRemoved(ZeroconfDiscoveryProvider.this, service);
	        		}
	        	}
			}
			
			@Override
			public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) { }
		};
		
		mDnsListener = new DiscoveryListener() {
			
			@Override
			public void onStopDiscoveryFailed(String serviceType, int errorCode) {
				for ( DiscoveryProviderListener listener: serviceListeners) {
	        		listener.onServiceDiscoveryFailed(ZeroconfDiscoveryProvider.this, new ServiceCommandError(errorCode, "Discovery failed", serviceType));
	        	}
			}
			
			@Override
			public void onStartDiscoveryFailed(String serviceType, int errorCode) {
				for ( DiscoveryProviderListener listener: serviceListeners) {
	        		listener.onServiceDiscoveryFailed(ZeroconfDiscoveryProvider.this, new ServiceCommandError(errorCode, "Discovery failed", serviceType));
	        	}
			}
			
			@Override
			public void onServiceFound(NsdServiceInfo serviceInfo) {
				mNsdManager.resolveService(serviceInfo, mResolveFoundListener);
			}
			
			@Override
			public void onServiceLost(NsdServiceInfo serviceInfo) {
				mNsdManager.resolveService(serviceInfo, mResolveLostListener);
			}
			
			@Override
			public void onDiscoveryStopped(String serviceType) { }
			
			@Override
			public void onDiscoveryStarted(String serviceType) { }
		};
	}
}
