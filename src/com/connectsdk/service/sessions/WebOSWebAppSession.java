/*
 * WebOSWebAppSession
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Jeffrey Glenn on 07 Mar 2014
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

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.connectsdk.core.Util;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.WebOSTVService;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.command.ServiceCommand;
import com.connectsdk.service.command.URLServiceSubscription;

public class WebOSWebAppSession extends WebAppSession {
	private static final String namespaceKey = "connectsdk.";
	protected WebOSTVService service;
	
	private ServiceSubscription<PlayStateListener> mPlayStateSubscription;
	private ServiceSubscription<MessageListener> mMessageSubscription;
	private ConcurrentHashMap<String, ServiceCommand<?>> mActiveCommands;
	
	private int UID;
	private boolean connected;
	
	public WebOSWebAppSession(LaunchSession launchSession, DeviceService service) {
		super(launchSession, service);
		
		UID = 0;
		mActiveCommands = new ConcurrentHashMap<String, ServiceCommand<?>>(0, 0.75f, 10);
		connected = false;
		
		this.service = (WebOSTVService) service;
	}

	private int getNextId() {
		return ++UID;
	}
	
	public void handleMediaEvent(JSONObject payload) {
		String type = "";
		
		type = payload.optString("type");
		if (type.length() == 0)
			return;
		
		if (type.equals("playState")) {
			if (mPlayStateSubscription == null)
				return;
			
			String playStateString = payload.optString(type);
			if (playStateString.length() == 0)
				return;

			final MediaControl.PlayStateStatus playState = parsePlayState(playStateString);
			
			for (PlayStateListener listener : mPlayStateSubscription.getListeners()) {
				Util.postSuccess(listener, playState);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public void handleMediaCommandResponse(final JSONObject payload) {
		String requetID = payload.optString("requestId");
		if (requetID.length() == 0)
			return;
		
		final ServiceCommand<ResponseListener<Object>> command = (ServiceCommand<ResponseListener<Object>>) mActiveCommands.get(requetID);
		
		if (command == null)
			return;
		
		String mError = payload.optString("error");

		if (mError.length() != 0) {
			Util.postError(command.getResponseListener(), new ServiceCommandError(0, mError, null));
		} else {
			Util.postSuccess(command.getResponseListener(), payload);
		}
		
		mActiveCommands.remove(requetID);
	}
	
	public MessageListener messageHandler = new MessageListener() {
		
		@Override
		public void onMessage(final Object message) {
			JSONObject json = null;

			if (message instanceof JSONObject) {
				json = (JSONObject) message;
			
				String contentTypeFull = null;
				int contentTypeIndex;
				JSONObject payload = null;
				
				contentTypeFull = json.optString("contentType");
				contentTypeIndex = contentTypeFull.indexOf(namespaceKey);

				if (contentTypeIndex != -1) {
					int contentIndex = contentTypeIndex + namespaceKey.length();
					if (contentIndex > contentTypeFull.length())
						return;

					String contentType = contentTypeFull.substring(contentIndex);
					payload = json.optJSONObject(contentType);
		
					if (payload == null)
						return;

					if (contentType.equals("mediaEvent"))
						handleMediaEvent(payload);
					else if (contentType.equals("mediaCommandResponse"))
						handleMediaCommandResponse(payload);
				} else {
					handleMessage(json);
				}
			} else if (message instanceof String) {
				handleMessage(message);
			}
		}
		
		@Override
		public void onError(ServiceCommandError error) {
			// we will not be getting errors through this listener, so we can ignore
		}
		
		@Override
		public void onSuccess(Object object) {
			// we will not be getting success messages through this listener, so we can ignore
		}
	};
	
	public void handleMessage(final Object message) {
		if (mMessageSubscription == null)
			return;
		
		Util.runOnUI(new Runnable() {
			
			@Override
			public void run() {
				getWebAppSessionListener().onReceiveMessage(WebOSWebAppSession.this, message);
			}
		});
		
	}
	
	public PlayStateStatus parsePlayState(String playStateString) {
		if (playStateString.equals("playing"))
			return PlayStateStatus.Playing;
		else if (playStateString.equals("paused"))
			return PlayStateStatus.Paused;
		else if (playStateString.equals("idle"))
			return PlayStateStatus.Idle;
		else if (playStateString.equals("buffering"))
			return PlayStateStatus.Buffering;
		else if (playStateString.equals("finished"))
			return PlayStateStatus.Finished;
		
		return PlayStateStatus.Unknown;
	}
	
	public void connect(final ResponseListener<Object> connectionListener) {
		if (mMessageSubscription == null)
			mMessageSubscription = new URLServiceSubscription<MessageListener>(service, null, null, null);
		
		if (connected) {
			connectionListener.onSuccess(null);
		}
		
		service.connectToWebApp(this, new ResponseListener<Object>() {
			
			@Override
			public void onError(final ServiceCommandError error) {
				Util.postError(connectionListener, error);
			}
			
			@Override
			public void onSuccess(final Object object) {
				connected = true;

				Util.postSuccess(connectionListener, object);
			}
		});
	}
	
	@Override
	public void join(final ResponseListener<Object> connectionListener) {
		service.connectToWebApp(this, true, new ResponseListener<Object>() {
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(connectionListener, error);
			}
			
			@Override
			public void onSuccess(Object object) {
				connected = true;
				Util.postSuccess(connectionListener, WebOSWebAppSession.this);
			}
		});
	}
	
	public void disconnectFromWebApp() {
		service.disconnectFromWebApp(this);
	}
	
	@Override
	public void sendMessage(final String message, final ResponseListener<Object> listener) {
		if (message == null) {
			if (listener != null)
				listener.onError(new ServiceCommandError(0, "Cannot send an Empty Message", null));
			
			return;
		}

		if (connected) {
			service.sendMessage(message, launchSession, listener);
		} else {
			connect(new ResponseListener<Object>() {
				
				@Override public void onSuccess(Object object) {
					service.sendMessage(message, launchSession, listener);
				}
				
				@Override public void onError(ServiceCommandError error) {
					Util.postError(listener, error);
				}
			});
		}
	}
	
	@Override
	public void sendMessage(final JSONObject message, final ResponseListener<Object> listener) {
		if (message == null) {
			Util.postError(listener, new ServiceCommandError(0, "Cannot send an Empty Message", null));
			
			return;
		}

		if (connected) {
			service.sendMessage(message, launchSession, listener);
		} else {
			connect(new ResponseListener<Object>() {
				
				@Override public void onSuccess(Object object) {
					service.sendMessage(message, launchSession, listener);
				}
				
				@Override public void onError(ServiceCommandError error) {
					if (listener != null) {
						listener.onError(error);
					}
				}
			});
		}
	}
	
	@Override
	public void close(ResponseListener<Object> listener) {
		connected = false;
		mActiveCommands.clear();
		
		if (mPlayStateSubscription != null) {
			mPlayStateSubscription.unsubscribe();
			mPlayStateSubscription = null;
		}
			
		if (mMessageSubscription != null) {
			mMessageSubscription.unsubscribe();
			mMessageSubscription = null;
		}
		
		service.getWebAppLauncher().closeWebApp(launchSession, listener);
	}
	
	@Override
	public void seek(final long position, ResponseListener<Object> listener) {
		if (position < 0) {
			if (listener != null)
				listener.onError(new ServiceCommandError(0, "Must pass a valid positive value", null));
			
			return;
		}

		int requestIdNumber = getNextId();
		final String requestId = String.format(Locale.US,  "req%d", requestIdNumber);
		
		JSONObject message = null;
		try {
			message = new JSONObject() {{
				put("contentType", namespaceKey + "mediaCommand");
				put("mediaCommand", new JSONObject() {{
					put("type", "seek");
					put("position", position / 1000);
					put("requestId", requestId);
				}});
			}};
		} catch (JSONException e) {
			if (listener != null)
				listener.onError(new ServiceCommandError(0, "JSON Parse error", null));
		}
		
		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(service, null, null, listener);
		
		mActiveCommands.put(requestId, command);
		
		sendMessage(message, listener);
	}
	
	@Override
	public void getPosition(final PositionListener listener) {
		int requestIdNumber = getNextId();
		final String requestId = String.format(Locale.US,  "req%d", requestIdNumber);
		
		JSONObject message = null;
		try {
			message = new JSONObject() {{
				put("contentType", namespaceKey + "mediaCommand");
				put("mediaCommand", new JSONObject() {{
					put("type", "getPosition");
					put("requestId", requestId);
				}});
			}};
		} catch (JSONException e) {
			Util.postError(listener, new ServiceCommandError(0, "JSON Parse error", null));
		}
		
		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(service, null, null, new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				try {
					long position = ((JSONObject) response).getLong("position");
					
					if (listener != null)
						listener.onSuccess(position * 1000);
				} catch (JSONException e) {
					this.onError(new ServiceCommandError(0, "JSON Parse error", null));
				}
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				if (listener != null)
					listener.onError(error);
			}
		});
		
		mActiveCommands.put(requestId, command);
		
		sendMessage(message, new ResponseListener<Object>() {
			
			@Override public void onSuccess(Object response) { }
			@Override
			public void onError(ServiceCommandError error) {
				if (listener != null)
					listener.onError(error);
			}
		});
	}
	
	@Override
	public void getDuration(final DurationListener listener) {
		int requestIdNumber = getNextId();
		final String requestId = String.format(Locale.US,  "req%d", requestIdNumber);
		
		JSONObject message = null;
		try {
			message = new JSONObject() {{
				put("contentType", namespaceKey + "mediaCommand");
				put("mediaCommand", new JSONObject() {{
					put("type", "getDuration");
					put("requestId", requestId);
				}});
			}};
		} catch (JSONException e) {
			if (listener != null)
				listener.onError(new ServiceCommandError(0, "JSON Parse error", null));
		}
		
		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(service, null, null, new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				try {
					long position = ((JSONObject) response).getLong("duration");
					
					if (listener != null)
						listener.onSuccess(position * 1000);
				} catch (JSONException e) {
					this.onError(new ServiceCommandError(0, "JSON Parse error", null));
				}
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				if (listener != null)
					listener.onError(error);
			}
		});
		
		mActiveCommands.put(requestId, command);
		
		sendMessage(message, new ResponseListener<Object>() {
			
			@Override public void onSuccess(Object response) { }
			@Override
			public void onError(ServiceCommandError error) {
				if (listener != null)
					listener.onError(error);
			}
		});
	}
	
	@Override
	public void getPlayState(final PlayStateListener listener) {
		int requestIdNumber = getNextId();
		final String requestId = String.format(Locale.US,  "req%d", requestIdNumber);
		
		JSONObject message = null;
		try {
			message = new JSONObject() {{
				put("contentType", namespaceKey + "mediaCommand");
				put("mediaCommand", new JSONObject() {{
					put("type", "getPlayState");
					put("requestId", requestId);
				}});
			}};
		} catch (JSONException e) {
			if (listener != null)
				listener.onError(new ServiceCommandError(0, "JSON Parse error", null));
		}
		
		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(service, null, null, new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				try {
					String playStateString = ((JSONObject) response).getString("playState");
					PlayStateStatus playState = parsePlayState(playStateString);
					
					if (listener != null)
						listener.onSuccess(playState);
				} catch (JSONException e) {
					this.onError(new ServiceCommandError(0, "JSON Parse error", null));
				}
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				if (listener != null)
					listener.onError(error);
			}
		});
		
		mActiveCommands.put(requestId, command);
		
		sendMessage(message, new ResponseListener<Object>() {
			
			@Override public void onSuccess(Object response) { }
			@Override
			public void onError(ServiceCommandError error) {
				if (listener != null)
					listener.onError(error);
			}
		});
	}
	
	@Override
	public ServiceSubscription<PlayStateListener> subscribePlayState(final PlayStateListener listener) {
		if (mPlayStateSubscription == null)
			mPlayStateSubscription = new URLServiceSubscription<MediaControl.PlayStateListener>(service, null, null, null);
		
		if (!connected) {
			connect(new ResponseListener<Object>() {
				
				@Override public void onError(ServiceCommandError error) {
					Util.postError(listener, error);
				}
				@Override public void onSuccess(Object response) {
				}
			});
		}
		
		if (!mPlayStateSubscription.getListeners().contains(listener))
			mPlayStateSubscription.addListener(listener);

		return mPlayStateSubscription;
	}
	
	/*****************
	 * Media Control *
	 *****************/
	@Override
	public MediaControl getMediaControl() {
		return this;
	}
	
	@Override
	public CapabilityPriorityLevel getMediaControlCapabilityLevel() {
		return CapabilityPriorityLevel.HIGH;
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
	public void displayImage(final String url, final String mimeType, final String title, final String description, final String iconSrc, final MediaPlayer.LaunchListener listener) {
		int requestIdNumber = getNextId();
		final String requestId = String.format(Locale.US,  "req%d", requestIdNumber);
		
		JSONObject message = null;
		try {
			message = new JSONObject() {{
				putOpt("contentType", namespaceKey + "mediaCommand");
				putOpt("mediaCommand", new JSONObject() {{
					putOpt("type", "displayImage");
					putOpt("mediaURL", url);
					putOpt("iconURL", iconSrc);
					putOpt("title", title);
					putOpt("description", description);
					putOpt("mimeType", mimeType);
					putOpt("requestId", requestId);
				}});
			}};
		} catch (JSONException e) {
			e.printStackTrace();
			// Should never hit this
		}
		
		ResponseListener<Object> response = new ResponseListener<Object>() {
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
			
			@Override
			public void onSuccess(Object object) {
				Util.postSuccess(listener, new MediaLaunchObject(launchSession, getMediaControl()));
			}
		};
		
		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(service, null, null, response);
		
		mActiveCommands.put(requestId, command);
		
		sendMessage(message, new ResponseListener<Object>() {
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
			
			@Override public void onSuccess(Object object) { }
		});
	}
	
	@Override
	public void playMedia(final String url, final String mimeType, final String title, final String description, final String iconSrc, final boolean shouldLoop, final MediaPlayer.LaunchListener listener) {
		int requestIdNumber = getNextId();
		final String requestId = String.format(Locale.US,  "req%d", requestIdNumber);
		
		JSONObject message = null;
		try {
			message = new JSONObject() {{
				putOpt("contentType", namespaceKey + "mediaCommand");
				putOpt("mediaCommand", new JSONObject() {{
					putOpt("type", "playMedia");
					putOpt("mediaURL", url);
					putOpt("iconURL", iconSrc);
					putOpt("title", title);
					putOpt("description", description);
					putOpt("mimeType", mimeType);
					putOpt("shouldLoop", shouldLoop);
					putOpt("requestId", requestId);
				}});
			}};
		} catch (JSONException e) {
			e.printStackTrace();
			// Should never hit this
		}
		
		ResponseListener<Object> response = new ResponseListener<Object>() {
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
			
			@Override
			public void onSuccess(Object object) {
				Util.postSuccess(listener, new MediaLaunchObject(launchSession, getMediaControl()));
			}
		};
		
		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(service, null, null, response);
		
		mActiveCommands.put(requestId, command);
		
		sendMessage(message, new ResponseListener<Object>() {
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
			
			@Override public void onSuccess(Object object) { }
		});
	}
}
