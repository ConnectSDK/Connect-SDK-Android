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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;

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

    private ConcurrentHashMap<String, ServiceDescription> services;
    private CopyOnWriteArrayList<DiscoveryProviderListener> serviceListeners;
    
	public CastDiscoveryProvider(Context context) {
        mMediaRouter = MediaRouter.getInstance(context);
        mMediaRouteSelector = new MediaRouteSelector.Builder()
        	.addControlCategory(CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID))
        	.build();
        
        mMediaRouterCallback = new MediaRouterCallback();
        
		services = new ConcurrentHashMap<String, ServiceDescription>(8, 0.75f, 2);
		serviceListeners = new CopyOnWriteArrayList<DiscoveryProviderListener>();
	}
	
	@Override
	public void start() {
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			
			@Override
			public void run() {
		        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
			}
		});
	}

	@Override
	public void stop() {
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			
			@Override
			public void run() {
				mMediaRouter.removeCallback(mMediaRouterCallback);
			}
		});
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeDeviceFilter(JSONObject parameters) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}
	
    private class MediaRouterCallback extends MediaRouter.Callback {

		@Override
		public void onRouteAdded(MediaRouter router, RouteInfo route) {
            super.onRouteAdded(router, route);

			CastDevice castDevice = CastDevice.getFromBundle(route.getExtras());
			
			String ipAddress = castDevice.getIpAddress().getHostAddress();
            String uuid = castDevice.getDeviceId();
            String friendlyName = castDevice.getFriendlyName();
            String modelName = castDevice.getModelName();
            String modelNumber = castDevice.getDeviceVersion();
            int port = castDevice.getServicePort();
            String modelDescription = route.getDescription();
            
            String serviceFilter = "Chromecast";

            ServiceDescription oldService = services.get(uuid);

            ServiceDescription newService;
        	if ( oldService == null ) {
                newService = new CastServiceDescription(serviceFilter, uuid, ipAddress, castDevice);
                newService.setFriendlyName(friendlyName);
                newService.setModelName(modelName);
                newService.setModelNumber(modelNumber);
                newService.setModelDescription(modelDescription);
                newService.setPort(port);
                newService.setServiceID(CastService.ID);
                
                services.put(uuid, newService);
	        }
        	else {
        		newService = oldService;

        		newService.setIpAddress(ipAddress);
                newService.setFriendlyName(friendlyName);
                newService.setModelName(modelName);
                newService.setModelNumber(modelNumber);
                newService.setModelDescription(modelDescription);
                newService.setPort(port);
                newService.setServiceID(CastService.ID);
                ((CastServiceDescription)newService).setCastDevice(castDevice);
        		
        		services.put(uuid, newService);
        	}
            
        	for ( DiscoveryProviderListener listenter: serviceListeners) {
        		listenter.onServiceAdded(CastDiscoveryProvider.this, newService);
        	}
		}

		@Override
		public void onRouteChanged(MediaRouter router, RouteInfo route) {
			super.onRouteChanged(router, route);
			
			CastDevice castDevice = CastDevice.getFromBundle(route.getExtras());
            
			String uuid = castDevice.getDeviceId();
            
            ServiceDescription myService = services.get(uuid);
			
            if ( myService != null ) {
    			String ipAddress = castDevice.getIpAddress().getHostAddress();
                String friendlyName = castDevice.getFriendlyName();
                String modelName = castDevice.getModelName();
                String modelNumber = castDevice.getDeviceVersion();
                int port = castDevice.getServicePort();
                String modelDescription = route.getDescription();
            
                myService.setIpAddress(ipAddress);
                myService.setFriendlyName(friendlyName);
                myService.setModelName(modelName);
                myService.setModelNumber(modelNumber);
                myService.setModelDescription(modelDescription);
                myService.setPort(port);
                ((CastServiceDescription)myService).setCastDevice(castDevice);

                services.put(uuid, myService);
                
            	for ( DiscoveryProviderListener listenter: serviceListeners) {
            		listenter.onServiceAdded(CastDiscoveryProvider.this, myService);
            	}
            }
		}

		@Override
		public void onRoutePresentationDisplayChanged(MediaRouter router,
				RouteInfo route) {
			System.out.println("[DEBUG] onRoutePresentationDisplayChanged: " + route.getDescription());
			System.out.println("[DEBUG] onRoutePresentationDisplayChanged: " + route.getId());
			System.out.println("[DEBUG] onRoutePresentationDisplayChanged: " + route.getName());
			super.onRoutePresentationDisplayChanged(router, route);
		}

		@Override
		public void onRouteRemoved(MediaRouter router, RouteInfo route) {
			super.onRouteRemoved(router, route);
			
			CastDevice castDevice = CastDevice.getFromBundle(route.getExtras());
			
			String uuid = castDevice.getDeviceId();
			
			if (services.containsKey(uuid)) {
				ServiceDescription serviceDescription = services.get(uuid);
				
				for ( DiscoveryProviderListener listener: serviceListeners) {
            		listener.onServiceRemoved(CastDiscoveryProvider.this, serviceDescription);
            	}
				
				services.remove(uuid);
			}
		}

		@Override
		public void onRouteVolumeChanged(MediaRouter router, RouteInfo route) {
			System.out.println("[DEBUG] onRouteVolumeChanged: " + route.getDescription());
			System.out.println("[DEBUG] onRouteVolumeChanged: " + route.getId());
			System.out.println("[DEBUG] onRouteVolumeChanged: " + route.getName());
			super.onRouteVolumeChanged(router, route);
		}
    	
    }
}
