/*
 * SSDPDiscoveryProvider
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.connectsdk.core.Util;
import com.connectsdk.core.upnp.Device;
import com.connectsdk.core.upnp.ssdp.SSDP;
import com.connectsdk.core.upnp.ssdp.SSDP.ParsedDatagram;
import com.connectsdk.core.upnp.ssdp.SSDPSearchMsg;
import com.connectsdk.core.upnp.ssdp.SSDPSocket;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.discovery.DiscoveryProviderListener;
import com.connectsdk.service.config.ServiceDescription;

public class SSDPDiscoveryProvider implements DiscoveryProvider {
	Context context;
	
	private final static int RESCAN_INTERVAL = 10000;
	private final static int RESCAN_ATTEMPTS = 3;
	private final static int SSDP_TIMEOUT = RESCAN_INTERVAL * RESCAN_ATTEMPTS;
	
    boolean needToStartSearch = false;

    private ConcurrentHashMap<String, ServiceDescription> services;
    private CopyOnWriteArrayList<DiscoveryProviderListener> serviceListeners;
    
    private ConcurrentHashMap<String, ServiceDescription> foundServices = new ConcurrentHashMap<String, ServiceDescription>();
    private ConcurrentHashMap<String, ServiceDescription> discoveredServices = new ConcurrentHashMap<String, ServiceDescription>();
    
    List<JSONObject> serviceFilters;

    private SSDPSocket mSSDPSocket;
    
    private Timer dataTimer;
    
    private Pattern uuidReg;
    
    private Thread responseThread;
    private Thread notifyThread;

	public SSDPDiscoveryProvider(Context context) {
		this.context = context;

		uuidReg = Pattern.compile("(?<=uuid:)(.+?)(?=(::)|$)");

		services = new ConcurrentHashMap<String, ServiceDescription>(8, 0.75f, 2);
		serviceListeners = new CopyOnWriteArrayList<DiscoveryProviderListener>();
		serviceFilters = new ArrayList<JSONObject>();
	}
	
	private void openSocket() {
		if (mSSDPSocket != null && mSSDPSocket.isConnected())
			return;

		try {
			InetAddress source = Util.getIpAddress(context);
			if (source == null) 
				return;
			
			mSSDPSocket = new SSDPSocket(source);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void start() {
		openSocket();

		dataTimer = new Timer();
		dataTimer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				sendSearch();
			}
		}, 100, RESCAN_INTERVAL);
        
		responseThread = new Thread(mResponseHandler);
		notifyThread = new Thread(mRespNotifyHandler);
		
		responseThread.start();
		notifyThread.start();
	}
	
	public void sendSearch() {
		List<String> killKeys = new ArrayList<String>();
		
		long killPoint = new Date().getTime() - SSDP_TIMEOUT;
		
		for (String key : foundServices.keySet()) {
			ServiceDescription service = foundServices.get(key);
			if (service.getLastDetection() < killPoint) {
				killKeys.add(key);
			}
		}
		
		for (String key : killKeys) {
			final ServiceDescription service = foundServices.get(key);
			
			Util.runOnUI(new Runnable() {
				
				@Override
				public void run() {
					for (DiscoveryProviderListener listener : serviceListeners) {
						listener.onServiceRemoved(SSDPDiscoveryProvider.this, service);
					}
				}
			});
			
			foundServices.remove(key);
		}

        for (JSONObject searchTarget : serviceFilters) {
        	SSDPSearchMsg search = null;
        	try {
        		search = new SSDPSearchMsg(searchTarget.getString("filter"));
        	} catch (JSONException e) {
        		e.printStackTrace();
        		return;
        	}
        	
        	final String message = search.toString();
	        
        	Timer timer = new Timer();
	        /* Send 3 times like WindowsMedia */
        	for (int i = 0; i < 3; i++) {
	        	TimerTask task = new TimerTask() {
					
					@Override
					public void run() {
						try {
							if (mSSDPSocket != null)
								mSSDPSocket.send(message);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
	        	};
	        	
	        	timer.schedule(task, i * 1000);
        	}
        };
	}

	@Override
	public void stop() {
		if (dataTimer != null) { 
			dataTimer.cancel();
		}
		
		if (responseThread != null) {
			responseThread.interrupt();
		}
		
		if (notifyThread != null) {
			notifyThread.interrupt();
		}

		if (mSSDPSocket != null) {
			mSSDPSocket.close();
			mSSDPSocket = null;
		}
	}
	
	@Override
	public void reset() {
		stop();
		services.clear();
		foundServices.clear();
		discoveredServices.clear();
	}

	@Override
	public void addDeviceFilter(JSONObject parameters) {
		if ( !parameters.has("filter") ) {
			Log.e("Connect SDK", "This device filter does not have ssdp filter info");
		} else {
//			String newFilter = null;
//			try {
//				newFilter = parameters.getString("filter");
//				for ( int i = 0; i < serviceFilters.size(); i++) { 
//					String filter = serviceFilters.get(i).getString("filter");
//					
//					if ( newFilter.equals(filter) ) 
//						return;
//				}
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
			
			serviceFilters.add(parameters);
			
//			if ( newFilter != null )
//			controlPoint.addFilter(newFilter);
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
		
		if ( shouldRemove ) {
			serviceFilters.remove(removalIndex);
		}
	}
	
	@Override
	public boolean isEmpty() {
		return serviceFilters.size() == 0;
	}

    private Runnable mResponseHandler = new Runnable() {
        @Override
        public void run() {
            while (mSSDPSocket != null) {
                try {
                    handleDatagramPacket(SSDP.convertDatagram(mSSDPSocket.responseReceive()));
                } catch (IOException e) {
                	e.printStackTrace();
                	break;
                }
            }
        }
    };
    
    private Runnable mRespNotifyHandler = new Runnable() {
        @Override
        public void run() {
            while (mSSDPSocket != null) {
                try {
                    handleDatagramPacket(SSDP.convertDatagram(mSSDPSocket.notifyReceive()));
                } catch (IOException e) {
                	e.printStackTrace();
                	break;
                }
            }
        }
    };
    
    private void handleDatagramPacket(final ParsedDatagram pd) {
        // Debugging stuff
//        Util.runOnUI(new Runnable() {
//			
//			@Override
//			public void run() {
//		        Log.d("Connect SDK Socket", "Packet received | type = " + pd.type);
//		        
//		        for (String key : pd.data.keySet()) {
//		        	Log.d("Connect SDK Socket", "    " + key + " = " + pd.data.get(key));
//		        }
//		        Log.d("Connect SDK Socket", "__________________________________________");
//			}
//		});
        // End Debugging stuff
        
        String serviceFilter = pd.data.get(pd.type.equals(SSDP.SL_NOTIFY) ? SSDP.NT : SSDP.ST);

    	if (serviceFilter == null || SSDP.SL_MSEARCH.equals(pd.type) || !isSearchingForFilter(serviceFilter))
    		return;
    	
    	String usnKey = pd.data.get(SSDP.USN);
    	
    	if (usnKey == null || usnKey.length() == 0)
    		return;

    	Matcher m = uuidReg.matcher(usnKey);

    	if (!m.find())
    		return;

        String uuid = m.group();
        
        if (SSDP.NTS_BYEBYE.equals(pd.data.get(SSDP.NTS))) {
        	final ServiceDescription service = foundServices.get(uuid);
        	
        	if (service != null) {
        		Util.runOnUI(new Runnable() {
					
					@Override
					public void run() {
						for (DiscoveryProviderListener listener : serviceListeners) {
							listener.onServiceRemoved(SSDPDiscoveryProvider.this, service);
						}
					}
				});
        	}
        } else {
        	String location = pd.data.get(SSDP.LOCATION);
        	
        	if (location == null || location.length() == 0)
        		return;
        	
        	ServiceDescription foundService = foundServices.get(uuid);
        	ServiceDescription discoverdService = discoveredServices.get(uuid);
        	
        	boolean isNew = foundService == null && discoverdService == null;
        	
        	if (isNew) {
        		foundService = new ServiceDescription();
        		foundService.setUUID(uuid);
        		foundService.setServiceFilter(serviceFilter);
        		foundService.setIpAddress(pd.dp.getAddress().getHostAddress());
        		foundService.setPort(3001);
        		
        		discoveredServices.put(uuid, foundService);
        		
        		getLocationData(location, uuid, serviceFilter);
        	}
        	
        	if (foundService != null)
        		foundService.setLastDetection(new Date().getTime());
        }
    	
//    	for (JSONObject filterObj : serviceFilters) {
//    		String filter = null;
//    		try {
//    			filter = filterObj.getString("filter");
//    		} catch (JSONException e) {
//    			e.printStackTrace();
//    			continue;
//    		}
//    		if (filter.indexOf(datagramFilter) != -1) {
//    			skip = false;
//    			break;
//    		}
//    	}
//    	
//    	if (skip)
//    		return;
//
//        if (SSDP.SL_OK.equals(pd.type)) {
//            handleRespMsg(pd);
//        } else if (SSDP.SL_NOTIFY.equals(pd.type)) {
//            handleNotifyMsg(pd);
//        }
    }
    
    public void getLocationData(final String location, final String uuid, final String serviceFilter) {
    	Util.runInBackground(new Runnable() {
			
			@Override
			public void run() {
				Device device = Device.createInstanceFromXML(location, serviceFilter);
				
	            if (device != null) {
	            	if (true) {//device.friendlyName != null) {
	            		device.UUID = uuid;
	            		boolean hasServices = containsServicesWithFilter(device, serviceFilter);
	            		
	            		if (hasServices) {
	            			final ServiceDescription service = discoveredServices.get(uuid);
            			
	            			if (service != null) {
		            			service.setServiceID(serviceIdForFilter(serviceFilter));
		            			service.setServiceFilter(serviceFilter);
		            			service.setFriendlyName(device.friendlyName);
		            			service.setModelName(device.modelName);
		            			service.setModelNumber(device.modelNumber);
		            			service.setModelDescription(device.modelDescription);
		            			service.setManufacturer(device.manufacturer);
		            			service.setApplicationURL(device.applicationURL);
		            			service.setServiceList(device.serviceList);
		            			service.setResponseHeaders(device.headers);
		            			service.setLocationXML(device.locationXML);
		            			
		            			foundServices.put(uuid, service);
		            			
		            			Util.runOnUI(new Runnable() {
									
									@Override
									public void run() {
										for (DiscoveryProviderListener listener : serviceListeners) {
											listener.onServiceAdded(SSDPDiscoveryProvider.this, service);
										}
									}
								});
	            			}
	            		}
	            	}
	            }
	            
	            discoveredServices.remove(uuid);
			}
		}, true);

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
    
    public boolean isSearchingForFilter(String filter) {
    	for (JSONObject serviceFilter : serviceFilters) {
    		try {
    			String ssdpFilter = serviceFilter.getString("filter");
    			
    			if (ssdpFilter.equals(filter))
    				return true;
    		} catch (JSONException e) {
    			e.printStackTrace();
    			continue;
    		}
    	}
    	
    	return false;
    }
    
    public boolean containsServicesWithFilter(Device device, String filter) {
//    	List<String> servicesRequired = new ArrayList<String>();
//    	
//    	for (JSONObject serviceFilter : serviceFilters) {
//    	}
    	
    	//  TODO  Implement this method.  Not sure why needs to happen since there are now required services.
    	
    	return true;
    }
	
	@Override
	public void addListener(DiscoveryProviderListener listener) {
		serviceListeners.add(listener);
	}

	@Override
	public void removeListener(DiscoveryProviderListener listener) {
		serviceListeners.remove(listener);
	}
}
