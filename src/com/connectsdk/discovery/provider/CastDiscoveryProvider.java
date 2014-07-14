/*
 * CastDiscoveryProvider
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 20 Feb 2014
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;

import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.discovery.DiscoveryProviderListener;
import com.connectsdk.service.CastService;
import com.connectsdk.service.config.CastServiceDescription;
import com.connectsdk.service.config.ServiceDescription;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;

public class CastDiscoveryProvider implements DiscoveryProvider {
    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private MediaRouter.Callback mMediaRouterCallback;

    private ConcurrentHashMap<String, ServiceDescription> foundServices;
    private CopyOnWriteArrayList<DiscoveryProviderListener> serviceListeners;
    
	private final static int RESCAN_INTERVAL = 10000;
	private final static int RESCAN_ATTEMPTS = 3;
	private final static int SSDP_TIMEOUT = RESCAN_INTERVAL * RESCAN_ATTEMPTS;
	
	private Timer addCallbackTimer;
	private Timer removeCallbackTimer;

	public CastDiscoveryProvider(Context context) {
        mMediaRouter = MediaRouter.getInstance(context);
        mMediaRouteSelector = new MediaRouteSelector.Builder()
        	.addControlCategory(CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID))
        	.build();
        
        mMediaRouterCallback = new MediaRouterCallback();
        
        foundServices = new ConcurrentHashMap<String, ServiceDescription>(8, 0.75f, 2);
		serviceListeners = new CopyOnWriteArrayList<DiscoveryProviderListener>();
	}
	
	@Override
	public void start() {
		stop();
		
		addCallbackTimer = new Timer();
		addCallbackTimer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				sendSearch();
			}
		}, 100, RESCAN_INTERVAL);
		
		removeCallbackTimer = new Timer();
		removeCallbackTimer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					
					@Override
					public void run() {
						mMediaRouter.removeCallback(mMediaRouterCallback);
					}
				});
			}
		}, 9100, RESCAN_INTERVAL);
	}
	
	private void sendSearch() {
		List<String> killKeys = new ArrayList<String>();
		
		long killPoint = new Date().getTime() - SSDP_TIMEOUT;
		
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
							listener.onServiceRemoved(CastDiscoveryProvider.this, service);
						}
					}
				});
			}
			
			if (foundServices.containsKey(key))
				foundServices.remove(key);
		}
		
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			
			@Override
			public void run() {
		        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
			}
		});
	}

	@Override
	public void stop() {
		if (addCallbackTimer != null) {
			addCallbackTimer.cancel();
		}
		
		if (removeCallbackTimer != null) {
			removeCallbackTimer.cancel();
		}
		
		if (mMediaRouter != null) {
			new Handler(Looper.getMainLooper()).post(new Runnable() {
				
				@Override
				public void run() {
					mMediaRouter.removeCallback(mMediaRouterCallback);
				}
			});
		}
	}

	@Override
	public void reset() {
		stop();
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
	public void addDeviceFilter(JSONObject parameters) {}

	@Override
	public void removeDeviceFilter(JSONObject parameters) {}

	@Override
	public boolean isEmpty() {
		return false;
	}
	
    private class MediaRouterCallback extends MediaRouter.Callback {

		@Override
		public void onRouteAdded(MediaRouter router, RouteInfo route) {
            super.onRouteAdded(router, route);

			CastDevice castDevice = CastDevice.getFromBundle(route.getExtras());
            String uuid = castDevice.getDeviceId();
            
            ServiceDescription foundService = foundServices.get(uuid);

        	boolean isNew = foundService == null;
        	boolean listUpdateFlag = false;
            
        	if (isNew) {
        		foundService = new CastServiceDescription(CastService.ID, uuid, castDevice.getIpAddress().getHostAddress(), castDevice);
        		foundService.setFriendlyName(castDevice.getFriendlyName());
        		foundService.setModelName(castDevice.getModelName());
        		foundService.setModelNumber(castDevice.getDeviceVersion());
        		foundService.setModelDescription(route.getDescription());
        		foundService.setPort(castDevice.getServicePort());
        		foundService.setServiceID(CastService.ID);
                
        		listUpdateFlag = true;
	        }
        	else {
        		if (!foundService.getFriendlyName().equals(castDevice.getFriendlyName())) {
        			foundService.setFriendlyName(castDevice.getFriendlyName());
        			listUpdateFlag = true;
        		}
        		
                ((CastServiceDescription)foundService).setCastDevice(castDevice);
        	}

        	if (foundService != null)
        		foundService.setLastDetection(new Date().getTime());
        	
        	foundServices.put(uuid, foundService);
            
        	if (listUpdateFlag) {
        		for (DiscoveryProviderListener listenter: serviceListeners) {
        			listenter.onServiceAdded(CastDiscoveryProvider.this, foundService);
        		}
        	}
		}

		@Override
		public void onRouteChanged(MediaRouter router, RouteInfo route) {
			super.onRouteChanged(router, route);
			
			CastDevice castDevice = CastDevice.getFromBundle(route.getExtras());
			String uuid = castDevice.getDeviceId();
            
            ServiceDescription foundService = foundServices.get(uuid);
			
        	boolean isNew = foundService == null;
        	boolean listUpdateFlag = false;

            if (!isNew) {
                foundService.setIpAddress(castDevice.getIpAddress().getHostAddress());
                foundService.setModelName(castDevice.getModelName());
                foundService.setModelNumber(castDevice.getDeviceVersion());
                foundService.setModelDescription(route.getDescription());
                foundService.setPort(castDevice.getServicePort());
                ((CastServiceDescription)foundService).setCastDevice(castDevice);

        		if (!foundService.getFriendlyName().equals(castDevice.getFriendlyName())) {
        			foundService.setFriendlyName(castDevice.getFriendlyName());
                	listUpdateFlag = true;
        		}
        		
           		foundService.setLastDetection(new Date().getTime());
                
                foundServices.put(uuid, foundService);
                
                if (listUpdateFlag) {
                	for (DiscoveryProviderListener listenter: serviceListeners) {
                		listenter.onServiceAdded(CastDiscoveryProvider.this, foundService);
                	}
                }
            }
		}

		@Override
		public void onRoutePresentationDisplayChanged(MediaRouter router,
				RouteInfo route) {
			Log.d(Util.T, "onRoutePresentationDisplayChanged: [" + route.getName() + "] [" + route.getDescription() + "]");
			super.onRoutePresentationDisplayChanged(router, route);
		}

		@Override
		public void onRouteRemoved(MediaRouter router, RouteInfo route) {
			super.onRouteRemoved(router, route);
			
//			CastDevice castDevice = CastDevice.getFromBundle(route.getExtras());
//			String uuid = castDevice.getDeviceId();
//			
//        	final ServiceDescription service = foundServices.get(uuid);
//			
//			if (service != null) {
//        		Util.runOnUI(new Runnable() {
//					
//					@Override
//					public void run() {
//						for (DiscoveryProviderListener listener : serviceListeners) {
//							listener.onServiceRemoved(CastDiscoveryProvider.this, service);
//						}
//					}
//				});
//				
//				foundServices.remove(uuid);
//			}
		}

		@Override
		public void onRouteVolumeChanged(MediaRouter router, RouteInfo route) {
			Log.d(Util.T, "onRouteVolumeChanged: [" + route.getName() + "] [" + route.getDescription() + "]");
			super.onRouteVolumeChanged(router, route);
		}
    	
    }
}
