package com.connectsdk.service.sessions;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.connectsdk.core.Util;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.MultiScreenService;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommand;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.command.URLServiceSubscription;
import com.samsung.multiscreen.application.Application;
import com.samsung.multiscreen.application.Application.Status;
import com.samsung.multiscreen.application.ApplicationAsyncResult;
import com.samsung.multiscreen.application.ApplicationError;
import com.samsung.multiscreen.channel.Channel;
import com.samsung.multiscreen.channel.ChannelAsyncResult;
import com.samsung.multiscreen.channel.ChannelClient;
import com.samsung.multiscreen.channel.ChannelError;
import com.samsung.multiscreen.channel.IChannelListener;
import com.samsung.multiscreen.device.DeviceAsyncResult;
import com.samsung.multiscreen.device.DeviceError;

public class MultiScreenWebAppSession extends WebAppSession {
	protected MultiScreenService service;
	protected Application application;

	private final String channelId = "com.connectsdk.MainChannel";

    ServiceSubscription<PlayStateListener> playStateSubscription;
    Map<String, ServiceCommand<ResponseListener<? extends Object>>> activeCommands;

	private int UID;

	private Channel mChannel;
	private IChannelListener channelListener = new IChannelListener() {
		
		@Override
		public void onDisconnect() {
			mChannel = null;
		}
		
		@Override
		public void onConnect() {
			// TODO Auto-generated method stub

		}
		
		@Override
		public void onClientMessage(ChannelClient client, String message) {
			try {
				JSONObject messageJSON = new JSONObject(message);
				
				String contentType = messageJSON.optString("contentType");
				String str = new String("connectsdk.");
				
				if (contentType != null && contentType.contains(str)) {
					String payloadKey = contentType.substring(str.length());
					
					if (payloadKey == null || payloadKey.length() == 0) 
						return;
					
					JSONObject messagePayload = messageJSON.optJSONObject(payloadKey);
					
					if (messagePayload == null) 
						return;
					
					if (payloadKey.equals("mediaEvent")) {
						handleMediaEvent(messagePayload);
					}
					else if (payloadKey.equals("mediaCommandResponse")) {
						handleMediaCommandResponse(messagePayload);
					}
					
				}
				else {
					handleMessage(messageJSON);
				}
			} catch (JSONException e) {
				handleMessage(message);
			}
		}
		
		@Override
		public void onClientDisconnected(ChannelClient client) {
			// TODO Auto-generated method stub

		}
		
		@Override
		public void onClientConnected(ChannelClient client) {
			// TODO Auto-generated method stub
			
		}
	};
	
	public void handleMediaEvent(JSONObject payload) {
		String type = payload.optString("type", null);
		
		if (type.equals("playState")) {
			if (playStateSubscription == null) 
				return;
			
			String playStateString = payload.optString("playState");
			PlayStateStatus playState = parsePlayState(playStateString);
			
			for (PlayStateListener listener: playStateSubscription.getListeners()) {
				Util.postSuccess(listener, playState);
			}
		}
	}
	
	public void handleMediaCommandResponse(JSONObject payload) {
		String requestId = payload.optString("requestId");
		
		ServiceCommand<?> command = activeCommands.get(requestId);
	    
		if (command == null)
	        return;

		String error = payload.optString("error", null);
		
		if (error != null) {
			if (command.getResponseListener() != null) {
				command.getResponseListener().onError(new ServiceCommandError(0, error, null));
			}
		}
		else {
			if (command.getResponseListener() != null) {
				command.getResponseListener().onSuccess(payload);
			}
		}
		
		activeCommands.remove(requestId);
	}
	
	public void handleMessage(Object message) {
		if (getWebAppSessionListener() != null) {
			getWebAppSessionListener().onReceiveMessage(this, message);
		}
	}

	public MultiScreenWebAppSession(LaunchSession launchSession,
			DeviceService service) {
		super(launchSession, service);
		
		this.service = (MultiScreenService) service;
	}

	public PlayStateStatus parsePlayState(String playStateString) {
		PlayStateStatus playState = PlayStateStatus.Unknown;

	    if (playStateString.equals("playing"))
	        playState = PlayStateStatus.Playing;
	    else if (playStateString.equals("paused"))
	        playState = PlayStateStatus.Paused;
	    else if (playStateString.equals("idle"))
	        playState = PlayStateStatus.Idle;
	    else if (playStateString.equals("buffering"))
	        playState = PlayStateStatus.Buffering;
	    else if (playStateString.equals("finished"))
	        playState = PlayStateStatus.Finished;

	    return playState;
		
	}
	
	public int getNextId() {
		return UID++;
	}
	
	@Override
	public void connect(final ResponseListener<Object> listener) {
	    if (service == null || service.getDevice() == null) {
	        if (listener != null) {
	        	Util.postError(listener, new ServiceCommandError(0, "You can only connect to a valid WebAppSession object.", null));
	        }

	        return;
	    }
	    
	    activeCommands = new HashMap<String, ServiceCommand<ResponseListener<? extends Object>>>();
	    UID = 0;

	    service.getDevice().connectToChannel(channelId, new DeviceAsyncResult<Channel>() {
			
			@Override
			public void onResult(Channel channel) {
				mChannel= channel;
				mChannel.setListener(channelListener);

				if (listener != null) {
	            	Util.postSuccess(listener, null);
	            }
			}
			
			@Override
			public void onError(DeviceError error) {
				if (listener != null) {
					Util.postError(listener, new ServiceCommandError((int)error.getCode(), error.getMessage(), error));
				}
			}
		});
	}
	
	@Override
	public void join(final ResponseListener<Object> listener) {
		application.updateStatus(new ApplicationAsyncResult<Application.Status>() {
			
			@Override
			public void onResult(Status status) {
				if (status == Status.RUNNING) {
					connect(listener);
				}
				else {
					if (listener != null) {
						listener.onError(new ServiceCommandError(0, "Cannot join a web app that is not running", null));
					}
				}
			}
			
			@Override
			public void onError(ApplicationError error) {
				if (listener != null) {
					Util.postError(listener, new ServiceCommandError((int)error.getCode(), error.getMessage(), error));
				}
			}
		});
	}
	
	@Override
	public void sendMessage(String message, ResponseListener<Object> listener) {
	    if (message == null || message.length() == 0) {
	        if (listener != null) {
	        	Util.postError(listener, new ServiceCommandError(0, "Cannot send an empty message", null));
	        }

	        return;
	    }

	    if (mChannel != null && mChannel.isConnected()) {
	    	mChannel.sendToHost(message);

	    	if (listener != null) {
	    		Util.postSuccess(listener, null);
	    	}
	    }
	    else {
	    	if (listener != null) {
	    		Util.postError(listener, new ServiceCommandError(0, "Connection has not been established or has been lost", null));
	    	}
	    }
	}

	@Override
	public void sendMessage(JSONObject message, ResponseListener<Object> listener) {
		if (message == null || message.length() == 0) {
			if (listener != null) {
				Util.postError(listener, new ServiceCommandError(0, "Cannot send an empty message", null));
			}
			
			return;
		}
		
		sendMessage(message.toString(), listener);
	}
	
	@Override
	public void disconnectFromWebApp() {
		if (mChannel == null) {
			return;
		}
		
		mChannel.disconnect(new ChannelAsyncResult<Boolean>() {
			
			@Override
			public void onResult(Boolean result) {
				mChannel.setListener(null);
				
				if (getWebAppSessionListener() != null) {
					getWebAppSessionListener().onWebAppSessionDisconnect(MultiScreenWebAppSession.this);
				}
			}
			
			@Override
			public void onError(ChannelError error) {}
		});
	}

	@Override
	public void close(final ResponseListener<Object> listener) {
		if (mChannel != null && mChannel.isConnected()) {
//	        // This is a hack to enable closing of bridged web apps that we didn't open
			JSONObject closeCommand = new JSONObject();
			JSONObject type = new JSONObject();
			
			try {
				type.put("type", "close");
				
				closeCommand.put("contentType", "connectsdk.serviceCommand");
				closeCommand.put("serviceCommand", type);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			sendMessage(closeCommand, new ResponseListener<Object>() {
				
				@Override
				public void onSuccess(Object object) {
					disconnectFromWebApp();
					
					if (listener != null) {
						Util.postSuccess(listener, null);
					}
				}
				
				@Override
				public void onError(ServiceCommandError error) {
					disconnectFromWebApp();
					
					if (listener != null) {
						Util.postError(listener, error);
					}
				}
			});
		}
		else {
			service.getWebAppLauncher().closeWebApp(launchSession, listener);
		}
	}

	@Override
	public MediaPlayer getMediaPlayer() {
		return this;
	}

	@Override
	public CapabilityPriorityLevel getMediaPlayerCapabilityLevel() {
		return CapabilityPriorityLevel.HIGH;
	}

	@Override
	public void displayImage(String url, String mimeType, String title,
			String description, String iconSrc, final MediaPlayer.LaunchListener listener) {
		int requestIdNumber = getNextId();
		String requestId = String.format("req%d", requestIdNumber);
		
		JSONObject message = new JSONObject();
		JSONObject mediaCommand = new JSONObject();
		
		try {
			mediaCommand.put("type", "displayImage");
			mediaCommand.put("mediaURL", url);
			mediaCommand.put("iconURL", iconSrc);
			mediaCommand.put("title", title);
			mediaCommand.put("description", description);
			mediaCommand.put("mimeType", mimeType);
			mediaCommand.put("requestId", requestId);

			message.put("contentType", "connectsdk.mediaCommand");
			message.put("mediaCommand", mediaCommand);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onError(ServiceCommandError error) {
				if (listener != null) {
					Util.postError(listener, error);
				}
			}
			
			@Override
			public void onSuccess(Object object) {
				if (listener != null) {
					Util.postSuccess(listener, new MediaLaunchObject(launchSession, getMediaControl()));
				}
			}
		};
		ServiceCommand<ResponseListener<? extends Object>> command = new ServiceCommand<ResponseListener<? extends Object>>(null, null, null, responseListener);
		activeCommands.put(requestId, command);

		sendMessage(message.toString(), new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object object) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onError(ServiceCommandError error) {
				// TODO Auto-generated method stub
				
			}
		});
	}

	@Override
	public void playMedia(String url, String mimeType, String title,
			String description, String iconSrc, boolean shouldLoop,
			final MediaPlayer.LaunchListener listener) {
		int requestIdNumber = getNextId();
		String requestId = String.format("req%d", requestIdNumber);
		
		JSONObject message = new JSONObject();
		JSONObject mediaCommand = new JSONObject();
		
		try {
			mediaCommand.put("type", "playMedia");
			mediaCommand.put("mediaURL", url);
			mediaCommand.put("iconURL", iconSrc);
			mediaCommand.put("title", title);
			mediaCommand.put("description", description);
			mediaCommand.put("mimeType", mimeType);
			mediaCommand.put("shouldLoop", shouldLoop);
			mediaCommand.put("requestId", requestId);

			message.put("contentType", "connectsdk.mediaCommand");
			message.put("mediaCommand", mediaCommand);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onError(ServiceCommandError error) {
				if (listener != null) {
					Util.postError(listener, error);
				}
			}
			
			@Override
			public void onSuccess(Object object) {
				if (listener != null) {
					Util.postSuccess(listener, new MediaLaunchObject(launchSession, getMediaControl()));
				}
			}
		};
		ServiceCommand<ResponseListener<? extends Object>> command = new ServiceCommand<ResponseListener<? extends Object>>(null, null, null, responseListener);
		activeCommands.put(requestId, command);

		sendMessage(message.toString(), new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object object) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onError(ServiceCommandError error) {
				// TODO Auto-generated method stub
				
			}
		});
	}

	@Override
	public void closeMedia(LaunchSession launchSession, ResponseListener<Object> listener) {
		close(listener);
	}

	@Override
	public MediaControl getMediaControl() {
		return this;
	}

	@Override
	public CapabilityPriorityLevel getMediaControlCapabilityLevel() {
		return CapabilityPriorityLevel.HIGH;
	}
	
	@Override
	public void play(final ResponseListener<Object> listener) {
		int requestIdNumber = getNextId();
		String requestId = String.format("req%d", requestIdNumber);

		JSONObject message = new JSONObject();
		JSONObject mediaCommand = new JSONObject();
		
		try {
			mediaCommand.put("type", "play");
			mediaCommand.put("requestId", requestId);
			
			message.put("contentType", "connectsdk.mediaCommand");
			message.put("mediaCommand", mediaCommand);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onError(ServiceCommandError error) {
				if (listener != null) {
					Util.postError(listener, error);
				}
			}
			
			@Override
			public void onSuccess(Object object) {
				if (listener != null) {
					Util.postSuccess(listener, null);
				}
			}
		};
		ServiceCommand<ResponseListener<? extends Object>> command = new ServiceCommand<ResponseListener<? extends Object>>(null, null, null, responseListener);
		activeCommands.put(requestId, command);
		
		sendMessage(message.toString(), new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object object) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onError(ServiceCommandError error) {
				// TODO Auto-generated method stub
				
			}
		});
	}

	@Override
	public void pause(final ResponseListener<Object> listener) {
		int requestIdNumber = getNextId();
		String requestId = String.format("req%d", requestIdNumber);

		JSONObject message = new JSONObject();
		JSONObject mediaCommand = new JSONObject();
		
		try {
			mediaCommand.put("type", "pause");
			mediaCommand.put("requestId", requestId);
			
			message.put("contentType", "connectsdk.mediaCommand");
			message.put("mediaCommand", mediaCommand);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onError(ServiceCommandError error) {
				if (listener != null) {
					Util.postError(listener, error);
				}
			}
			
			@Override
			public void onSuccess(Object object) {
				if (listener != null) {
					Util.postSuccess(listener, null);
				}
			}
		};
		ServiceCommand<ResponseListener<? extends Object>> command = new ServiceCommand<ResponseListener<? extends Object>>(null, null, null, responseListener);
		activeCommands.put(requestId, command);

		sendMessage(message.toString(), new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object object) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onError(ServiceCommandError error) {
				// TODO Auto-generated method stub
				
			}
		});
	}

	@Override
	public void seek(long position, final ResponseListener<Object> listener) {
		int requestIdNumber = getNextId();
		String requestId = String.format("req%d", requestIdNumber);

		JSONObject message = new JSONObject();
		JSONObject mediaCommand = new JSONObject();
		
		try {
			mediaCommand.put("type", "seek");
			mediaCommand.put("position", position);
			mediaCommand.put("requestId", requestId);
			
			message.put("contentType", "connectsdk.mediaCommand");
			message.put("mediaCommand", mediaCommand);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onError(ServiceCommandError error) {
				if (listener != null) {
					Util.postError(listener, error);
				}
			}
			
			@Override
			public void onSuccess(Object object) {
				if (listener != null) {
					Util.postSuccess(listener, null);
				}
			}
		};
		ServiceCommand<ResponseListener<? extends Object>> command = new ServiceCommand<ResponseListener<? extends Object>>(null, null, null, responseListener);
		activeCommands.put(requestId, command);

		sendMessage(message.toString(), new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object object) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onError(ServiceCommandError error) {
				// TODO Auto-generated method stub
				
			}
		});
	}

	@Override
	public void getPosition(final PositionListener listener) {
		int requestIdNumber = getNextId();
		String requestId = String.format("req%d", requestIdNumber);

		JSONObject message = new JSONObject();
		JSONObject mediaCommand = new JSONObject();
		
		try {
			mediaCommand.put("type", "getPosition");
			mediaCommand.put("requestId", requestId);
			
			message.put("contentType", "connectsdk.mediaCommand");
			message.put("mediaCommand", mediaCommand);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onError(ServiceCommandError error) {
				if (listener != null) {
					Util.postError(listener, error);
				}
			}
			
			@Override
			public void onSuccess(Object object) {
				JSONObject responseObject = (JSONObject)object;
				String positionString = responseObject.optString("position", null);
				float position = 0;
				
				if (positionString != null)
					position = Float.parseFloat(positionString) * 1000;
				
				if (listener != null) {
					Util.postSuccess(listener, (long)position);
				}
			}
		};
		ServiceCommand<ResponseListener<? extends Object>> command = new ServiceCommand<ResponseListener<? extends Object>>(null, null, null, responseListener);
		activeCommands.put(requestId, command);
		
		sendMessage(message.toString(), new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object object) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onError(ServiceCommandError error) {
				// TODO Auto-generated method stub
				
			}
		});
	}

	@Override
	public void getDuration(final DurationListener listener) {
		int requestIdNumber = getNextId();
		String requestId = String.format("req%d", requestIdNumber);

		JSONObject message = new JSONObject();
		JSONObject mediaCommand = new JSONObject();
		
		try {
			mediaCommand.put("type", "getDuration");
			mediaCommand.put("requestId", requestId);
			
			message.put("contentType", "connectsdk.mediaCommand");
			message.put("mediaCommand", mediaCommand);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onError(ServiceCommandError error) {
				if (listener != null) {
					Util.postError(listener, error);
				}
			}
			
			@Override
			public void onSuccess(Object object) {
				JSONObject responseObject = (JSONObject)object;
				String durationString = responseObject.optString("duration", null);
				float duration = 0;
				
				if (durationString != null)
					duration = Float.parseFloat(durationString) * 1000;
				
				if (listener != null) {
					Util.postSuccess(listener, (long)duration);
				}
			}
		};
		ServiceCommand<ResponseListener<? extends Object>> command = new ServiceCommand<ResponseListener<? extends Object>>(null, null, null, responseListener);
		activeCommands.put(requestId, command);
		
		sendMessage(message.toString(), new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object object) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onError(ServiceCommandError error) {
				// TODO Auto-generated method stub
				
			}
		});
	}

	@Override
	public void getPlayState(final PlayStateListener listener) {
		int requestIdNumber = getNextId();
		String requestId = String.format("req%d", requestIdNumber);

		JSONObject message = new JSONObject();
		JSONObject mediaCommand = new JSONObject();
		
		try {
			mediaCommand.put("type", "getPlayState");
			mediaCommand.put("requestId", requestId);
			
			message.put("contentType", "connectsdk.mediaCommand");
			message.put("mediaCommand", mediaCommand);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onError(ServiceCommandError error) {
				if (listener != null) {
					Util.postError(listener, error);
				}
			}
			
			@Override
			public void onSuccess(Object object) {
				JSONObject responseObject = (JSONObject)object;
				String playStateString = responseObject.optString("playState");
				PlayStateStatus playState = parsePlayState(playStateString);
				
				if (listener != null) {
					Util.postSuccess(listener, playState);
				}
			}
		};
		ServiceCommand<ResponseListener<? extends Object>> command = new ServiceCommand<ResponseListener<? extends Object>>(null, null, null, responseListener);
		activeCommands.put(requestId, command);
		
		sendMessage(message.toString(), new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object object) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onError(ServiceCommandError error) {
				// TODO Auto-generated method stub
				
			}
		});
	}

	@Override
	public ServiceSubscription<PlayStateListener> subscribePlayState(PlayStateListener listener) {
		if (playStateSubscription == null) {
			playStateSubscription = new URLServiceSubscription<MediaControl.PlayStateListener>(null, null, null, null);
		}
		
		if (mChannel == null || !mChannel.isConnected()) {
			connect(null);
		}
		
		playStateSubscription.addListener(listener);
		
		return playStateSubscription;
	}

	public Application getApplication() {
		return application;
	}

	public void setApplication(Application application) {
		this.application = application;
	}
}
