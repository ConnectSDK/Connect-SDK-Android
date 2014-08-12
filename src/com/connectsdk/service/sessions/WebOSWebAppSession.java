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

import java.util.Enumeration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.connectsdk.core.MediaInfo;
import com.connectsdk.core.Util;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.WebOSTVService;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommand;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.command.URLServiceSubscription;
import com.connectsdk.service.sessions.LaunchSession.LaunchSessionType;
import com.connectsdk.service.webos.WebOSTVServiceSocketClient;
import com.connectsdk.service.webos.WebOSTVServiceSocketClient.WebOSTVServiceSocketClientListener;

public class WebOSWebAppSession extends WebAppSession {
	private static final String namespaceKey = "connectsdk.";
	protected WebOSTVService service;

	ResponseListener<ServiceCommand<ResponseListener<Object>>> mConnectionListener;

	public WebOSTVServiceSocketClient socket;
	public URLServiceSubscription<ResponseListener<Object>> appToAppSubscription;

	private ServiceSubscription<PlayStateListener> mPlayStateSubscription;
	private ServiceSubscription<MessageListener> mMessageSubscription;
	private ConcurrentHashMap<String, ServiceCommand<?>> mActiveCommands;

	String mFullAppId;

	private int UID;
	private boolean connected;

	public WebOSWebAppSession(LaunchSession launchSession, DeviceService service) {
		super(launchSession, service);

		UID = 0;
		mActiveCommands = new ConcurrentHashMap<String, ServiceCommand<?>>(0,
				0.75f, 10);
		connected = false;

		this.service = (WebOSTVService) service;
	}

	private int getNextId() {
		return ++UID;
	}

	public Boolean isConnected() {
		return connected;
	}

	public void setConnected(Boolean connected) {
		this.connected = connected;
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

			for (PlayStateListener listener : mPlayStateSubscription
					.getListeners()) {
				Util.postSuccess(listener, playState);
			}
		}
	}

	public String getFullAppId() {
		if (mFullAppId == null) {
			if (launchSession.getSessionType() != LaunchSessionType.WebApp)
				mFullAppId = launchSession.getAppId();
			else {
				Enumeration<String> enumeration = service.getWebAppIdMappings()
						.keys();

				while (enumeration.hasMoreElements()) {
					String mappedFullAppId = enumeration.nextElement();
					String mappedAppId = service.getWebAppIdMappings().get(
							mappedFullAppId);

					if (mappedAppId.equalsIgnoreCase(launchSession.getAppId())) {
						mFullAppId = mappedAppId;
						break;
					}
				}
			}
		}

		if (mFullAppId == null)
			return launchSession.getAppId();
		else
			return mFullAppId;
	}

	public void setFullAppId(String fullAppId) {
		mFullAppId = fullAppId;
	}

	private WebOSTVServiceSocketClientListener mSocketListener = new WebOSTVServiceSocketClientListener() {

		@Override
		public void onRegistrationFailed(ServiceCommandError error) {
		}

		@Override
		public Boolean onReceiveMessage(JSONObject payload) {
			String type = payload.optString("type");

			if ("p2p".equals(type)) {
				String fromAppId = null;

				fromAppId = payload.optString("from");

				if (!fromAppId.equalsIgnoreCase(getFullAppId()))
					return false;

				Object message = payload.opt("payload");

				if (message instanceof JSONObject) {
					JSONObject messageJSON = (JSONObject) message;

					String contentType = messageJSON.optString("contentType");
					Integer contentTypeIndex = contentType
							.indexOf("connectsdk.");

					if (contentType != null && contentTypeIndex >= 0) {
						String payloadKey = contentType.split("connectsdk.")[1];

						if (payloadKey == null || payloadKey.length() == 0)
							return false;

						JSONObject messagePayload = messageJSON
								.optJSONObject(payloadKey);

						if (messagePayload == null)
							return false;

						if (payloadKey.equalsIgnoreCase("mediaEvent"))
							handleMediaEvent(messagePayload);
						else if (payloadKey
								.equalsIgnoreCase("mediaCommandResponse"))
							handleMediaCommandResponse(messagePayload);
					} else {
						handleMessage(messageJSON);
					}
				} else if (message instanceof String) {
					handleMessage(message);
				}

				return false;
			}

			return true;
		}

		@Override
		public void onFailWithError(ServiceCommandError error) {
			connected = false;
			appToAppSubscription = null;

			if (mConnectionListener != null) {
				if (error == null)
					error = new ServiceCommandError(0,
							"Unknown error connecting to web socket", null);

				mConnectionListener.onError(error);
			}

			mConnectionListener = null;
		}

		@Override
		public void onConnect() {
			if (mConnectionListener != null)
				mConnectionListener.onSuccess(null);

			mConnectionListener = null;
		}

		@Override
		public void onCloseWithError(ServiceCommandError error) {
			connected = false;
			appToAppSubscription = null;

			if (mConnectionListener != null) {
				if (error != null)
					mConnectionListener.onError(error);
				else {
					if (getWebAppSessionListener() != null)
						getWebAppSessionListener().onWebAppSessionDisconnect(
								WebOSWebAppSession.this);
				}
			}

			mConnectionListener = null;
		}

		@Override
		public void onBeforeRegister() {
		}
	};

	@SuppressWarnings("unchecked")
	public void handleMediaCommandResponse(final JSONObject payload) {
		String requetID = payload.optString("requestId");
		if (requetID.length() == 0)
			return;

		final ServiceCommand<ResponseListener<Object>> command = (ServiceCommand<ResponseListener<Object>>) mActiveCommands
				.get(requetID);

		if (command == null)
			return;

		String mError = payload.optString("error");

		if (mError.length() != 0) {
			Util.postError(command.getResponseListener(),
					new ServiceCommandError(0, mError, null));
		} else {
			Util.postSuccess(command.getResponseListener(), payload);
		}

		mActiveCommands.remove(requetID);
	}

	public void handleMessage(final Object message) {
		Util.runOnUI(new Runnable() {

			@Override
			public void run() {
				if (getWebAppSessionListener() != null)
					getWebAppSessionListener().onReceiveMessage(
							WebOSWebAppSession.this, message);
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

	public void connect(ResponseListener<Object> connectionListener) {
		connect(false, connectionListener);
	}

	@Override
	public void join(ResponseListener<Object> connectionListener) {
		connect(true, connectionListener);
	}

	private void connect(final Boolean joinOnly,
			final ResponseListener<Object> connectionListener) {
		if (socket != null
				&& socket.getState() == WebOSTVServiceSocketClient.State.CONNECTING) {
			if (connectionListener != null)
				;
			connectionListener
					.onError(new ServiceCommandError(
							0,
							"You have a connection request pending,  please wait until it has finished",
							null));

			return;
		}

		if (isConnected()) {
			if (connectionListener != null)
				connectionListener.onSuccess(null);

			return;
		}

		mConnectionListener = new ResponseListener<ServiceCommand<ResponseListener<Object>>>() {

			@Override
			public void onError(ServiceCommandError error) {
				if (socket != null)
					disconnectFromWebApp();

				if (connectionListener != null) {
					if (error == null)
						error = new ServiceCommandError(0,
								"Unknown error connecting to web app", null);

					connectionListener.onError(error);
				}
			}

			@Override
			public void onSuccess(
					ServiceCommand<ResponseListener<Object>> object) {
				ResponseListener<Object> finalConnectionListener = new ResponseListener<Object>() {

					@Override
					public void onError(ServiceCommandError error) {
						disconnectFromWebApp();

						if (connectionListener != null)
							connectionListener.onError(error);
					}

					@Override
					public void onSuccess(Object object) {
						connected = true;

						if (connectionListener != null)
							connectionListener.onSuccess(object);
					}
				};

				service.connectToWebApp(WebOSWebAppSession.this, joinOnly,
						finalConnectionListener);
			}
		};

		if (socket != null) {
			if (socket.isConnected())
				mConnectionListener.onSuccess(null);
			else
				socket.connect();
		} else {
			socket = new WebOSTVServiceSocketClient(service,
					WebOSTVServiceSocketClient.getURI(service));
			socket.setListener(mSocketListener);
			socket.connect();
		}
	}

	public void disconnectFromWebApp() {
		connected = false;
		mConnectionListener = null;

		if (appToAppSubscription != null) {
			appToAppSubscription.removeListeners();
			appToAppSubscription = null;
		}

		if (socket != null) {
			socket.setListener(null);
			socket.disconnect();
			socket = null;
		}
	}

	@Override
	public void sendMessage(final String message,
			final ResponseListener<Object> listener) {
		if (message == null || message.length() == 0) {
			if (listener != null)
				listener.onError(new ServiceCommandError(0,
						"Cannot send an Empty Message", null));

			return;
		}

		sendP2PMessage(message, listener);
	}

	@Override
	public void sendMessage(final JSONObject message,
			final ResponseListener<Object> listener) {
		if (message == null || message.length() == 0) {
			Util.postError(listener, new ServiceCommandError(0,
					"Cannot send an Empty Message", null));

			return;
		}

		sendP2PMessage(message, listener);
	}

	private void sendP2PMessage(final Object message,
			final ResponseListener<Object> listener) {
		JSONObject _payload = new JSONObject();

		try {
			_payload.put("type", "p2p");
			_payload.put("to", getFullAppId());
			_payload.put("payload", message);
		} catch (JSONException ex) {
			// do nothing
		}

		final JSONObject payload = _payload;

		if (isConnected()) {
			socket.sendMessage(payload, null);

			if (listener != null)
				listener.onSuccess(null);
		} else {
			ResponseListener<Object> connectListener = new ResponseListener<Object>() {

				@Override
				public void onError(ServiceCommandError error) {
					if (listener != null)
						listener.onError(error);
				}

				@Override
				public void onSuccess(Object object) {
					sendP2PMessage(message, listener);
				}
			};

			connect(connectListener);
		}
	}

	@Override
	public void close(ResponseListener<Object> listener) {
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
				listener.onError(new ServiceCommandError(0,
						"Must pass a valid positive value", null));

			return;
		}

		int requestIdNumber = getNextId();
		final String requestId = String.format(Locale.US, "req%d",
				requestIdNumber);

		JSONObject message = null;
		try {
			message = new JSONObject() {
				{
					put("contentType", namespaceKey + "mediaCommand");
					put("mediaCommand", new JSONObject() {
						{
							put("type", "seek");
							put("position", position / 1000);
							put("requestId", requestId);
						}
					});
				}
			};
		} catch (JSONException e) {
			if (listener != null)
				listener.onError(new ServiceCommandError(0, "JSON Parse error",
						null));
		}

		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(
				null, null, null, listener);

		mActiveCommands.put(requestId, command);

		sendMessage(message, listener);
	}

	@Override
	public void getPosition(final PositionListener listener) {
		int requestIdNumber = getNextId();
		final String requestId = String.format(Locale.US, "req%d",
				requestIdNumber);

		JSONObject message = null;
		try {
			message = new JSONObject() {
				{
					put("contentType", namespaceKey + "mediaCommand");
					put("mediaCommand", new JSONObject() {
						{
							put("type", "getPosition");
							put("requestId", requestId);
						}
					});
				}
			};
		} catch (JSONException e) {
			Util.postError(listener, new ServiceCommandError(0,
					"JSON Parse error", null));
		}

		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(
				null, null, null, new ResponseListener<Object>() {

					@Override
					public void onSuccess(Object response) {
						try {
							long position = ((JSONObject) response)
									.getLong("position");

							if (listener != null)
								listener.onSuccess(position * 1000);
						} catch (JSONException e) {
							this.onError(new ServiceCommandError(0,
									"JSON Parse error", null));
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

			@Override
			public void onSuccess(Object response) {
			}

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
		final String requestId = String.format(Locale.US, "req%d",
				requestIdNumber);

		JSONObject message = null;
		try {
			message = new JSONObject() {
				{
					put("contentType", namespaceKey + "mediaCommand");
					put("mediaCommand", new JSONObject() {
						{
							put("type", "getDuration");
							put("requestId", requestId);
						}
					});
				}
			};
		} catch (JSONException e) {
			if (listener != null)
				listener.onError(new ServiceCommandError(0, "JSON Parse error",
						null));
		}

		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(
				null, null, null, new ResponseListener<Object>() {

					@Override
					public void onSuccess(Object response) {
						try {
							long position = ((JSONObject) response)
									.getLong("duration");

							if (listener != null)
								listener.onSuccess(position * 1000);
						} catch (JSONException e) {
							this.onError(new ServiceCommandError(0,
									"JSON Parse error", null));
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

			@Override
			public void onSuccess(Object response) {
			}

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
		final String requestId = String.format(Locale.US, "req%d",
				requestIdNumber);

		JSONObject message = null;
		try {
			message = new JSONObject() {
				{
					put("contentType", namespaceKey + "mediaCommand");
					put("mediaCommand", new JSONObject() {
						{
							put("type", "getPlayState");
							put("requestId", requestId);
						}
					});
				}
			};
		} catch (JSONException e) {
			if (listener != null)
				listener.onError(new ServiceCommandError(0, "JSON Parse error",
						null));
		}

		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(
				null, null, null, new ResponseListener<Object>() {

					@Override
					public void onSuccess(Object response) {
						try {
							String playStateString = ((JSONObject) response)
									.getString("playState");
							PlayStateStatus playState = parsePlayState(playStateString);

							if (listener != null)
								listener.onSuccess(playState);
						} catch (JSONException e) {
							this.onError(new ServiceCommandError(0,
									"JSON Parse error", null));
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

			@Override
			public void onSuccess(Object response) {
			}

			@Override
			public void onError(ServiceCommandError error) {
				if (listener != null)
					listener.onError(error);
			}
		});
	}

	@Override
	public ServiceSubscription<PlayStateListener> subscribePlayState(
			final PlayStateListener listener) {
		if (mPlayStateSubscription == null)
			mPlayStateSubscription = new URLServiceSubscription<MediaControl.PlayStateListener>(
					null, null, null, null);

		if (!connected) {
			connect(new ResponseListener<Object>() {

				@Override
				public void onError(ServiceCommandError error) {
					Util.postError(listener, error);
				}

				@Override
				public void onSuccess(Object response) {
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
	public void displayImage(final String url, final String mimeType,
			final String title, final String description, final String iconSrc,
			final MediaPlayer.LaunchListener listener) {
		int requestIdNumber = getNextId();
		final String requestId = String.format(Locale.US, "req%d",
				requestIdNumber);

		JSONObject message = null;
		try {
			message = new JSONObject() {
				{
					putOpt("contentType", namespaceKey + "mediaCommand");
					putOpt("mediaCommand", new JSONObject() {
						{
							putOpt("type", "displayImage");
							putOpt("mediaURL", url);
							putOpt("iconURL", iconSrc);
							putOpt("title", title);
							putOpt("description", description);
							putOpt("mimeType", mimeType);
							putOpt("requestId", requestId);
						}
					});
				}
			};
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
				Util.postSuccess(listener, new MediaLaunchObject(launchSession,
						getMediaControl()));
			}
		};

		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(
				socket, null, null, response);

		mActiveCommands.put(requestId, command);

		sendP2PMessage(message, new ResponseListener<Object>() {

			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}

			@Override
			public void onSuccess(Object object) {
			}
		});
	}

	@Override
	public void displayImage(final MediaInfo mediaInfo,
			final MediaPlayer.LaunchListener listener) {
		int requestIdNumber = getNextId();
		final String requestId = String.format(Locale.US, "req%d",
				requestIdNumber);

		JSONObject message = null;
		try {
			message = new JSONObject() {
				{
					putOpt("contentType", namespaceKey + "mediaCommand");
					putOpt("mediaCommand", new JSONObject() {
						{
							putOpt("type", "displayImage");
							putOpt("mediaURL", mediaInfo.getUrl());
							putOpt("iconURL", mediaInfo.getImages().get(0)
									.getUrl());
							putOpt("title", mediaInfo.getTitle());
							putOpt("description", mediaInfo.getDescription());
							putOpt("mimeType", mediaInfo.getMimeType());
							putOpt("requestId", requestId);
						}
					});
				}
			};
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
				Util.postSuccess(listener, new MediaLaunchObject(launchSession,
						getMediaControl()));
			}
		};

		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(
				socket, null, null, response);

		mActiveCommands.put(requestId, command);

		sendP2PMessage(message, new ResponseListener<Object>() {

			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}

			@Override
			public void onSuccess(Object object) {
			}
		});
	}

	@Override
	public void playMedia(final String url, final String mimeType,
			final String title, final String description, final String iconSrc,
			final boolean shouldLoop, final MediaPlayer.LaunchListener listener) {
		int requestIdNumber = getNextId();
		final String requestId = String.format(Locale.US, "req%d",
				requestIdNumber);

		JSONObject message = null;
		try {
			message = new JSONObject() {
				{
					putOpt("contentType", namespaceKey + "mediaCommand");
					putOpt("mediaCommand", new JSONObject() {
						{
							putOpt("type", "playMedia");
							putOpt("mediaURL", url);
							putOpt("iconURL", iconSrc);
							putOpt("title", title);
							putOpt("description", description);
							putOpt("mimeType", mimeType);
							putOpt("shouldLoop", shouldLoop);
							putOpt("requestId", requestId);
						}
					});
				}
			};
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
				Util.postSuccess(listener, new MediaLaunchObject(launchSession,
						getMediaControl()));
			}
		};

		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(
				null, null, null, response);

		mActiveCommands.put(requestId, command);

		sendMessage(message, new ResponseListener<Object>() {

			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}

			@Override
			public void onSuccess(Object object) {
			}
		});
	}

	@Override
	public void playMedia(final MediaInfo mediaInfo,
			final boolean shouldLoop, final MediaPlayer.LaunchListener listener) {
		int requestIdNumber = getNextId();
		final String requestId = String.format(Locale.US, "req%d",
				requestIdNumber);

		JSONObject message = null;
		try {
			message = new JSONObject() {
				{
					putOpt("contentType", namespaceKey + "mediaCommand");
					putOpt("mediaCommand", new JSONObject() {
						{
							putOpt("type", "playMedia");
							putOpt("mediaURL", mediaInfo.getUrl());
							putOpt("iconURL", mediaInfo.getImages().get(0)
									.getUrl()  == null ? NULL : mediaInfo.getImages().get(0)
											.getUrl()) ;
							putOpt("poster", mediaInfo.getImages().get(1).getUrl() == null ? NULL : mediaInfo.getImages().get(1).getUrl());
							putOpt("title", mediaInfo.getTitle());
							putOpt("description", mediaInfo.getDescription());
							putOpt("mimeType", mediaInfo.getMimeType());
							putOpt("shouldLoop", shouldLoop);
							putOpt("requestId", requestId);
						}
					});
				}
			};
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
				Util.postSuccess(listener, new MediaLaunchObject(launchSession,
						getMediaControl()));
			}
		};

		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(
				null, null, null, response);

		mActiveCommands.put(requestId, command);

		sendMessage(message, new ResponseListener<Object>() {

			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}

			@Override
			public void onSuccess(Object object) {
			}
		});
	}
}
