/*
 * ZeroconfDiscoveryProvider
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.discovery.DiscoveryProviderListener;
import com.connectsdk.service.config.ServiceDescription;

public class ZeroconfDiscoveryProvider implements DiscoveryProvider {
	private static final String HOSTNAME = "connectsdk";

	JmDNS jmdns;
	InetAddress srcAddress;
	
	private final static int RESCAN_INTERVAL = 10000;
	private final static int RESCAN_ATTEMPTS = 3;
	private final static int TIMEOUT = RESCAN_INTERVAL * RESCAN_ATTEMPTS;

	private Timer dataTimer;

    List<JSONObject> serviceFilters;
    
    private ConcurrentHashMap<String, ServiceDescription> foundServices;
    private CopyOnWriteArrayList<DiscoveryProviderListener> serviceListeners;
    
	ServiceListener jmdnsListener = new ServiceListener() {
		
        @Override
        public void serviceResolved(ServiceEvent ev) {
			@SuppressWarnings("deprecation")
			String ipAddress = ev.getInfo().getHostAddress();
			if (!Util.isIPv4Address(ipAddress)) {
				// Currently, we only support ipv4
				return;
			}
			
            String uuid = ipAddress;
            String friendlyName = ev.getInfo().getName();
            int port = ev.getInfo().getPort();
            
        	ServiceDescription foundService = foundServices.get(uuid);

        	boolean isNew = foundService == null;
        	boolean listUpdateFlag = false;
        	
        	if (isNew) {
        		foundService = new ServiceDescription();
        		foundService.setUUID(uuid);
        		foundService.setServiceFilter(ev.getInfo().getType());
        		foundService.setIpAddress(ipAddress);
        		foundService.setServiceID(serviceIdForFilter(ev.getInfo().getType()));
            	foundService.setPort(port);
            	foundService.setFriendlyName(friendlyName);
            	
            	listUpdateFlag = true;
        	}
        	else {
        		if (!foundService.getFriendlyName().equals(friendlyName)) {
        			foundService.setFriendlyName(friendlyName);
        			listUpdateFlag = true;
        		}
        	}
        	
        	if (foundService != null)
        		foundService.setLastDetection(new Date().getTime());
        	
        	foundServices.put(uuid, foundService);
            
        	if (listUpdateFlag) {
            	for ( DiscoveryProviderListener listener: serviceListeners) {
            		listener.onServiceAdded(ZeroconfDiscoveryProvider.this, foundService);
            	}
        	}
        }
        
        @Override
        public void serviceRemoved(ServiceEvent ev) {
			@SuppressWarnings("deprecation")
        	String uuid = ev.getInfo().getHostAddress();
        	final ServiceDescription service = foundServices.get(uuid);
        	
        	if (service != null) {
        		Util.runOnUI(new Runnable() {
					
					@Override
					public void run() {
						for (DiscoveryProviderListener listener : serviceListeners) {
							listener.onServiceRemoved(ZeroconfDiscoveryProvider.this, service);
						}
					}
				});
        	}
        }
        
        @Override
        public void serviceAdded(ServiceEvent event) {
            // Required to force serviceResolved to be called again
            // (after the first search)
            jmdns.requestServiceInfo(event.getType(), event.getName(), 1);
        }
    };

	public ZeroconfDiscoveryProvider(Context context) {
		foundServices = new ConcurrentHashMap<String, ServiceDescription>(8, 0.75f, 2);

		serviceListeners = new CopyOnWriteArrayList<DiscoveryProviderListener>();
		serviceFilters = new ArrayList<JSONObject>();
		
		try {
			srcAddress = Util.getIpAddress(context);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void start() {
		stop();
		
		dataTimer = new Timer();
		MDNSSearchTask sendSearch = new MDNSSearchTask();
		dataTimer.schedule(sendSearch, 100, RESCAN_INTERVAL);
	}
	
	private class MDNSSearchTask extends TimerTask {

		@Override
		public void run() {
			List<String> killKeys = new ArrayList<String>();
			
			long killPoint = new Date().getTime() - TIMEOUT;
			
			for (String key : foundServices.keySet()) {
				ServiceDescription service = foundServices.get(key);
				if (service == null || service.getLastDetection() < killPoint) {
					killKeys.add(key);
				}
			}
			
			for (String key : killKeys) {
				final ServiceDescription service = foundServices.get(key);
				
				if (service != null) {
					Util.runOnUI(new Runnable() {
						
						@Override
						public void run() {
							for (DiscoveryProviderListener listener : serviceListeners) {
								listener.onServiceRemoved(ZeroconfDiscoveryProvider.this, service);
							}
						}
					});
				}
				
				if (foundServices.containsKey(key))
					foundServices.remove(key);
			}
			
			if (srcAddress != null) {
				try {
					if (jmdns != null) {
						jmdns.close();
					}
					jmdns = JmDNS.create(srcAddress, HOSTNAME);
					
			        for (JSONObject searchTarget : serviceFilters) {
						try {
				        	String filter = searchTarget.getString("filter");
							jmdns.addServiceListener(filter, jmdnsListener);
						} catch (JSONException e) {
							e.printStackTrace();
						}
			        };
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void stop() {
		if (dataTimer != null) {
			dataTimer.cancel();
		}
		
		if (jmdns != null) {
	        for (JSONObject searchTarget : serviceFilters) {
				try {
		        	String filter = searchTarget.getString("filter");
					jmdns.removeServiceListener(filter, jmdnsListener);
				} catch (JSONException e) {
					e.printStackTrace();
				}
	        };
		}
	}

	@Override
	public void reset() {
		foundServices.clear();
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
	
	public String serviceIdForFilter(String filter) {
    	String serviceId = "";
    	
    	for (JSONObject serviceFilter : serviceFilters) {
    		String ssdpFilter;
    		try {
    			ssdpFilter = serviceFilter.getString("filter");
    			if (ssdpFilter.equals(filter)) {
    				return serviceFilter.getString("serviceId");
    			}
    		} catch (JSONException e) {
    			e.printStackTrace();
    			continue;
    		}
    	}
    	
    	return serviceId;
    }
}
