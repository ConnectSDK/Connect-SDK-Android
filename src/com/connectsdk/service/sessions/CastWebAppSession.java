/*
 * CastWebAppSession
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 07 Mar 2014
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

package com.connectsdk.service.sessions;

import java.io.IOException;

import org.json.JSONObject;

import android.util.Log;

import com.connectsdk.core.Util;
import com.connectsdk.service.CastService;
import com.connectsdk.service.CastServiceChannel;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.URLServiceSubscription;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

public class CastWebAppSession extends WebAppSession {
	private CastService service;
	private CastServiceChannel castServiceChannel;
    private ApplicationMetadata metadata;
	
	public CastWebAppSession(LaunchSession launchSession, DeviceService service) {
		super(launchSession, service);
		
		this.service = (CastService) service;
	}
	
	@Override
	public void connect(final ResponseListener<Object> listener) {
		if (castServiceChannel != null) {
			disconnectFromWebApp(launchSession);
		}
		
		castServiceChannel = new CastServiceChannel(launchSession.getAppId(), this);
		
		try {
			Cast.CastApi.removeMessageReceivedCallbacks(service.getApiClient(), castServiceChannel.getNamespace());
			
			Cast.CastApi.setMessageReceivedCallbacks(service.getApiClient(),
					castServiceChannel.getNamespace(),
					castServiceChannel);
			
			Util.postSuccess(listener, null);
		} catch (IOException e) {
			castServiceChannel = null;

			Util.postError(listener, new ServiceCommandError(0, "Failed to create channel", null));
		}
	}
	
	@Override
	public void join(ResponseListener<Object> connectionListener) {
		connect(connectionListener);
	}
	
	public void disconnectFromWebApp(LaunchSession launchSession) {
		if (castServiceChannel == null) 
			return;

		try {
			Cast.CastApi.removeMessageReceivedCallbacks(service.getApiClient(), castServiceChannel.getNamespace());
			castServiceChannel = null;
		} catch (IOException e) {
	        Log.e("Connect SDK", "Exception while removing application", e);
		}
	}
	
	public void handleAppClose() {
      	for (URLServiceSubscription<?> subscription: service.getSubscriptions()) {
    		if (subscription.getTarget().equalsIgnoreCase("PlayState")) {
				for (int i = 0; i < subscription.getListeners().size(); i++) {
					@SuppressWarnings("unchecked")
					ResponseListener<Object> listener = (ResponseListener<Object>) subscription.getListeners().get(i);
					Util.postSuccess(listener, PlayStateStatus.Idle);
				}
    		}
    	}

		if (getWebAppSessionListener() != null) { 
			getWebAppSessionListener().onWebAppSessionDisconnect(this);
		}
	}
	
	@Override
	public void sendMessage(String message, final ResponseListener<Object> listener) {
		if (message == null) {
			Util.postError(listener, new ServiceCommandError(0, "Cannot send null message", null));
	        return;
	    }

	    if (castServiceChannel == null) {
	    	Util.postError(listener, new ServiceCommandError(0, "Cannot send a message to the web app without first connecting", null));
	        return;
	    }
		
	    Cast.CastApi.sendMessage(service.getApiClient(), castServiceChannel.getNamespace(), message).setResultCallback(new ResultCallback<Status>() {

	    	@Override
	    	public void onResult(Status result) {
	    		if (result.isSuccess()) {
	    			Util.postSuccess(listener, null);
	    		}
	    		else {
	    			Util.postError(listener, new ServiceCommandError(result.getStatusCode(), result.toString(), result));
	    		}
	    	}
	    });
	}
	
	@Override
	public void sendMessage(JSONObject message, ResponseListener<Object> listener) {
		sendMessage(message.toString(), listener);
	}
	
	@Override
	public void close(ResponseListener<Object> listener) {
		launchSession.close(listener);
	}
	
	/****************
	 * Media Player *
	 ****************/
	@Override
	public MediaPlayer getMediaPlayer() {
		return this;
	}
	
	@Override
	public CapabilityPriorityLevel getMediaPlayerCapabilityLevel() {
		return CapabilityPriorityLevel.HIGH;
	}
	
	@Override
	public void playMedia(String url, String mimeType, String title, String description, String iconSrc, boolean shouldLoop, MediaPlayer.LaunchListener listener) {
		service.playMedia(url, mimeType, title, description, iconSrc, shouldLoop, listener);
	}
	
	@Override
	public void closeMedia(LaunchSession launchSession, ResponseListener<Object> listener) {
		close(listener);
	}

	public ApplicationMetadata getMetadata() {
		return metadata;
	}

	public void setMetadata(ApplicationMetadata metadata) {
		this.metadata = metadata;
	}
}
