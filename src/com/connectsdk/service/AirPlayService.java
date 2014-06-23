/*
 * AirPlayService
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

package com.connectsdk.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.connectsdk.core.Util;
import com.connectsdk.etc.helper.DeviceServiceReachability;
import com.connectsdk.etc.helper.HttpMessage;
import com.connectsdk.service.airplay.PListBuilder;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommand;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;
import com.connectsdk.service.sessions.LaunchSession;
import com.connectsdk.service.sessions.LaunchSession.LaunchSessionType;

public class AirPlayService extends DeviceService implements MediaPlayer, MediaControl {

	public static final String ID = "AirPlay";

	interface PlaybackPositionListener {
		void onGetPlaybackPositionSuccess(long duration, long position);
		void onGetPlaybackPositionFailed(ServiceCommandError error);
	}
	
	HttpClient httpClient;

	public AirPlayService(ServiceDescription serviceDescription,
			ServiceConfig serviceConfig) {
		super(serviceDescription, serviceConfig);
		
		httpClient = new DefaultHttpClient();
		ClientConnectionManager mgr = httpClient.getConnectionManager();
		HttpParams params = httpClient.getParams();
		httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(params, mgr.getSchemeRegistry()), params);
	}

	public static JSONObject discoveryParameters() {
		JSONObject params = new JSONObject();
		
		try {
			params.put("serviceId", ID);
			params.put("filter",  "_airplay._tcp.local.");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return params;
	}
	
	@Override
	public MediaControl getMediaControl() {
		return this;
	}

	@Override
	public CapabilityPriorityLevel getMediaControlCapabilityLevel() {
		return CapabilityPriorityLevel.NORMAL;
	}

	@Override
	public void play(ResponseListener<Object> listener) {
		Map <String,String> params = new HashMap<String,String>();
		params.put("value", "1.000000");
		
		String uri = getRequestURL("rate", params);
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, listener);
		request.send();
	}

	@Override
	public void pause(ResponseListener<Object> listener) {
		Map <String,String> params = new HashMap<String,String>();
		params.put("value", "0.000000");
		
		String uri = getRequestURL("rate", params);
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, listener);
		request.send();
	}

	@Override
	public void stop(ResponseListener<Object> listener) {
		String uri = getRequestURL("stop");
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, listener);
		// TODO This is temp fix for issue https://github.com/ConnectSDK/Connect-SDK-Android/issues/66
		request.send();
		request.send();
	}

	@Override
	public void rewind(ResponseListener<Object> listener) {
		Map <String,String> params = new HashMap<String,String>();
		params.put("value", "-2.000000");
		
		String uri = getRequestURL("rate", params);
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, listener);
		request.send();
	}

	@Override
	public void fastForward(ResponseListener<Object> listener) {
		Map <String,String> params = new HashMap<String,String>();
		params.put("value", "2.000000");
		
		String uri = getRequestURL("rate", params);
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, listener);
		request.send();
	}

	@Override
	public void seek(final long position, ResponseListener<Object> listener) {
		float pos = ((float) position / 1000); 
		
		Map <String,String> params = new HashMap<String,String>();
		params.put("position", String.valueOf(pos));
		
		String uri = getRequestURL("scrub", params);
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, listener);
		request.send();
	}
	
	@Override
	public void getPosition(final PositionListener listener) {
		getPlaybackPosition(new PlaybackPositionListener() {
			
			@Override
			public void onGetPlaybackPositionSuccess(long duration, long position) {
				Util.postSuccess(listener, position);
			}
			
			@Override
			public void onGetPlaybackPositionFailed(ServiceCommandError error) {
				Util.postError(listener, new ServiceCommandError(0, "Unable to get position", null));
			}
		});		
	}

	@Override
	public void getPlayState(final PlayStateListener listener) {
		getPlaybackInfo(new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object object) {
				// TODO need to handle play state
//				Util.postSuccess(listener, object);
			}

			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		});
	}
	
	@Override
	public void getDuration(final DurationListener listener) {
		getPlaybackPosition(new PlaybackPositionListener() {
			
			@Override
			public void onGetPlaybackPositionSuccess(long duration, long position) {
				Util.postSuccess(listener, duration);
			}
			
			@Override
			public void onGetPlaybackPositionFailed(ServiceCommandError error) {
				Util.postError(listener, new ServiceCommandError(0, "Unable to get duration", null));
			}
		});
	}
	
	private void getPlaybackPosition(final PlaybackPositionListener listener) {
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				String strResponse = (String) response;
				
	            long duration = 0;
	            long position = 0;
	  
	            StringTokenizer st = new StringTokenizer(strResponse);
	            while (st.hasMoreTokens()) {
	            	String str = st.nextToken();
	            	String value;
	            	if (str.contains("duration")) {
	            		value = st.nextToken();
			            float f = Float.valueOf(value);
	            		duration = (long) f * 1000;
	            	}
	            	else if (str.contains("position")) {
	            		value = st.nextToken();
			            float f = Float.valueOf(value);
	            		position = (long) f * 1000;
	            	}
	            }

	            if (listener != null) {
	            	listener.onGetPlaybackPositionSuccess(duration, position);
	            }
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				if (listener != null)
					listener.onGetPlaybackPositionFailed(error);
			}
		};
		
		String uri = getRequestURL("scrub");
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, responseListener);
		request.setHttpMethod(ServiceCommand.TYPE_GET);
		request.send();
	}
	
	private void getPlaybackInfo(ResponseListener<Object> listener) {
		String uri = getRequestURL("playback-info");
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, listener);
		request.setHttpMethod(ServiceCommand.TYPE_GET);
		request.send();
	}

	@Override
	public ServiceSubscription<PlayStateListener> subscribePlayState(
			PlayStateListener listener) {
		Util.postError(listener, ServiceCommandError.notSupported());
		
		return null;
	}

	@Override
	public MediaPlayer getMediaPlayer() {
		return this;
	}

	@Override
	public CapabilityPriorityLevel getMediaPlayerCapabilityLevel() {
		return CapabilityPriorityLevel.NORMAL;
	}

	@Override
	public void displayImage(final String url, String mimeType, String title,
			String description, String iconSrc, final LaunchListener listener) {
		Util.runInBackground(new Runnable() {
			
			@Override
			public void run() {
				ResponseListener<Object> responseListener = new ResponseListener<Object>() {
					
					@Override
					public void onSuccess(Object response) {
						LaunchSession launchSession = new LaunchSession();
						launchSession.setService(AirPlayService.this);
						launchSession.setSessionType(LaunchSessionType.Media);

						Util.postSuccess(listener, new MediaLaunchObject(launchSession, AirPlayService.this));
					}
					
					@Override
					public void onError(ServiceCommandError error) {
						Util.postError(listener, error);
					}
				};
				
				String uri = getRequestURL("photo");
				HttpEntity entity = null;
				
				try {
				    URL imagePath = new URL(url);
		            HttpURLConnection connection = (HttpURLConnection) imagePath.openConnection();
		            connection.setDoInput(true);
		            connection.connect();
		            InputStream input = connection.getInputStream();
		            Bitmap  myBitmap = BitmapFactory.decodeStream(input);

		            ByteArrayOutputStream stream = new ByteArrayOutputStream();
		            myBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);

		            entity = new ByteArrayEntity(stream.toByteArray());
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(AirPlayService.this, uri, entity, responseListener);
				request.send();
			}
		});
	}

	public void playVideo(final String url, String mimeType, String title,
			String description, String iconSrc, boolean shouldLoop,
			final LaunchListener listener) {

		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				LaunchSession launchSession = new LaunchSession();
				launchSession.setService(AirPlayService.this);
				launchSession.setSessionType(LaunchSessionType.Media);
				
				Util.postSuccess(listener, new MediaLaunchObject(launchSession, AirPlayService.this));
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		};
		
		String uri = getRequestURL("play");
		HttpEntity entity = null;
		
		PListBuilder builder = new PListBuilder();
		builder.putString("Content-Location", url);
		builder.putReal("Start-Position", 0);
		
		try {
			entity = new StringEntity(builder.toString());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, entity, responseListener);
		request.send();	
	}
	
	@Override
	public void playMedia(String url, String mimeType, String title,
			String description, String iconSrc, boolean shouldLoop,
			LaunchListener listener) {

		if ( mimeType.contains("image") ) {
			displayImage(url, mimeType, title, description, iconSrc, listener);
		}
		else {
			playVideo(url, mimeType, title, description, iconSrc, shouldLoop, listener);
		}
	}

	@Override
	public void closeMedia(LaunchSession launchSession,
			ResponseListener<Object> listener) {

		stop(listener);
	}
	
	@Override
	public void sendCommand(final ServiceCommand<?> mCommand) {
		Util.runInBackground(new Runnable() {
			
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				final ServiceCommand<ResponseListener<Object>> command = (ServiceCommand<ResponseListener<Object>>) mCommand;

				Object payload = command.getPayload();
				
				HttpRequestBase request = command.getRequest();
				request.addHeader(HttpMessage.CONTENT_TYPE_HEADER, HttpMessage.CONTENT_TYPE_APPLICATION_PLIST);
				HttpResponse response = null;
				
				if (payload != null && command.getHttpMethod().equalsIgnoreCase(ServiceCommand.TYPE_POST)) {
					HttpEntity entity = null;
					
					try {
						if (payload instanceof String) {
							entity = new StringEntity((String) payload);
						} else if (payload instanceof JSONObject) {
							entity = new StringEntity(((JSONObject) payload).toString());
						} else if (payload instanceof HttpEntity) {
							entity = (HttpEntity)payload;
						}
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					
					((HttpPost) request).setEntity(entity);
				}
				
				try {
					response = httpClient.execute(request);
					
					int code = response.getStatusLine().getStatusCode();
					
					if (code == 200) { 
			            HttpEntity entity = response.getEntity();
			            String message = EntityUtils.toString(entity, "UTF-8");
			            
			            Util.postSuccess(command.getResponseListener(), message);
					}
					else {
			        	Util.postError(command.getResponseListener(), ServiceCommandError.getError(code));
					}
		
					response.getEntity().consumeContent();
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	@Override
	protected void updateCapabilities() {
		List<String> capabilities = new ArrayList<String>();
	
		for (String capability : MediaPlayer.Capabilities) { capabilities.add(capability); }
		
		capabilities.add(Play);
		capabilities.add(Pause);
		capabilities.add(Stop);
		capabilities.add(Position);
		capabilities.add(Duration);
		capabilities.add(PlayState);
		capabilities.add(Seek);
		capabilities.add(Rewind);
		capabilities.add(FastForward);

		setCapabilities(capabilities);
	}

	private String getRequestURL(String command) {
		return getRequestURL(command, null);
	}

	private String getRequestURL(String command, Map<String, String> params) {
		StringBuilder sb = new StringBuilder();
		sb.append("http://").append(serviceDescription.getIpAddress());
		sb.append(":").append(serviceDescription.getPort());
		sb.append("/").append(command);
		
		if (params != null) {
			for (Map.Entry<String, String> entry : params.entrySet()) {
			    String param = String.format("?%s=%s", entry.getKey(), entry.getValue());
			    sb.append(param); 
			}
		}
		
		return sb.toString();
	}
	
	@Override
	public boolean isConnectable() {
		return true;
	}
	
	@Override
	public boolean isConnected() {
		return connected;
	}
	
	@Override
	public void connect() {
		connected = true;
		
		reportConnected(true);
	}
	
	@Override
	public void disconnect() {
		connected = false;
		
		if (mServiceReachability != null)
			mServiceReachability.stop();
		
		Util.runOnUI(new Runnable() {
			
			@Override
			public void run() {
				if (listener != null)
					listener.onDisconnect(AirPlayService.this, null);
			}
		});
	}
	
	@Override
	public void onLoseReachability(DeviceServiceReachability reachability) {
		if (connected) {
			disconnect();
		} else {
			mServiceReachability.stop();
		}
	}
	
}
