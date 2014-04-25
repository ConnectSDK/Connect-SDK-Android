/*
 * WebOSTVService
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

package com.connectsdk.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.java_websocket.WebSocket;
import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.util.Log;

import com.connectsdk.core.AppInfo;
import com.connectsdk.core.ChannelInfo;
import com.connectsdk.core.ExternalInputInfo;
import com.connectsdk.core.ProgramList;
import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryManager.PairingLevel;
import com.connectsdk.service.capability.ExternalInputControl;
import com.connectsdk.service.capability.KeyControl;
import com.connectsdk.service.capability.Launcher;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.MouseControl;
import com.connectsdk.service.capability.PowerControl;
import com.connectsdk.service.capability.TVControl;
import com.connectsdk.service.capability.TextInputControl;
import com.connectsdk.service.capability.ToastControl;
import com.connectsdk.service.capability.VolumeControl;
import com.connectsdk.service.capability.WebAppLauncher;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.NotSupportedServiceSubscription;
import com.connectsdk.service.command.ServiceCommand;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.command.URLServiceSubscription;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;
import com.connectsdk.service.config.WebOSTVServiceConfig;
import com.connectsdk.service.sessions.LaunchSession;
import com.connectsdk.service.sessions.LaunchSession.LaunchSessionType;
import com.connectsdk.service.sessions.WebAppSession;
import com.connectsdk.service.sessions.WebAppSession.MessageListener;
import com.connectsdk.service.sessions.WebOSWebAppSession;
import com.connectsdk.service.webos.WebOSTVKeyboardInput;
import com.connectsdk.service.webos.WebOSTVMouseSocketConnection;

public class WebOSTVService extends DeviceService implements Launcher, MediaControl, MediaPlayer, VolumeControl, TVControl, ToastControl, ExternalInputControl, MouseControl, TextInputControl, PowerControl, KeyControl, WebAppLauncher {
	
	public static final String ID = "webOS TV";
	
	enum State {
    	NONE,
    	INITIAL,
    	CONNECTING,
    	REGISTERING,
    	REGISTERED,
    	DISCONNECTING
    };

	public interface WebOSTVServicePermission {
		public enum Open implements WebOSTVServicePermission {
		    LAUNCH,
		    LAUNCH_WEB,
		    APP_TO_APP,
		    CONTROL_AUDIO,
		    CONTROL_INPUT_MEDIA_PLAYBACK;
		}
		
		public static final WebOSTVServicePermission[] OPEN = {
			Open.LAUNCH,
			Open.LAUNCH_WEB,
			Open.APP_TO_APP,
			Open.CONTROL_AUDIO,
			Open.CONTROL_INPUT_MEDIA_PLAYBACK
		};
		
		public enum Protected implements WebOSTVServicePermission {
			CONTROL_POWER,
		    READ_INSTALLED_APPS,
		    CONTROL_DISPLAY,
		    CONTROL_INPUT_JOYSTICK,
		    CONTROL_INPUT_MEDIA_RECORDING,
		    CONTROL_INPUT_TV,
		    READ_INPUT_DEVICE_LIST,
		    READ_NETWORK_STATE,
		    READ_TV_CHANNEL_LIST,
		    WRITE_NOTIFICATION_TOAST
		}
		
		public static final WebOSTVServicePermission[] PROTECTED = {
			Protected.CONTROL_POWER,
			Protected.READ_INSTALLED_APPS,
		    Protected.CONTROL_DISPLAY,
		    Protected.CONTROL_INPUT_JOYSTICK,
		    Protected.CONTROL_INPUT_MEDIA_RECORDING,
		    Protected.CONTROL_INPUT_TV,
		    Protected.READ_INPUT_DEVICE_LIST,
		    Protected.READ_NETWORK_STATE,
		    Protected.READ_TV_CHANNEL_LIST,
		    Protected.WRITE_NOTIFICATION_TOAST
		};
		
		public enum PersonalActivity implements WebOSTVServicePermission {
		    CONTROL_INPUT_TEXT,
		    CONTROL_MOUSE_AND_KEYBOARD,
		    READ_CURRENT_CHANNEL,
		    READ_RUNNING_APPS;
		}
		
		public static final WebOSTVServicePermission[] PERSONAL_ACTIVITY = {
			PersonalActivity.CONTROL_INPUT_TEXT,
			PersonalActivity.CONTROL_MOUSE_AND_KEYBOARD,
			PersonalActivity.READ_CURRENT_CHANNEL,
			PersonalActivity.READ_RUNNING_APPS
		};
	}
	
	public final static String[] kWebOSTVServiceOpenPermissions = {
		"LAUNCH",
		"LAUNCH_WEBAPP",
		"APP_TO_APP",
		"CONTROL_AUDIO",
		"CONTROL_INPUT_MEDIA_PLAYBACK"
	};
	
	public final static String[] kWebOSTVServiceProtectedPermissions = {
		"CONTROL_POWER",
		"READ_INSTALLED_APPS",
		"CONTROL_DISPLAY",
		"CONTROL_INPUT_JOYSTICK",
		"CONTROL_INPUT_MEDIA_RECORDING",
		"CONTROL_INPUT_TV",
		"READ_INPUT_DEVICE_LIST",
		"READ_NETWORK_STATE",
		"READ_TV_CHANNEL_LIST",
		"WRITE_NOTIFICATION_TOAST"
	};
	
	public final static String[] kWebOSTVServicePersonalActivityPermissions = {
		"CONTROL_INPUT_TEXT",
		"CONTROL_MOUSE_AND_KEYBOARD",
		"READ_CURRENT_CHANNEL",
		"READ_RUNNING_APPS"
	};

	
	public interface SecureAccessTestListener extends ResponseListener<Boolean> { }

	public interface ACRAuthTokenListener extends ResponseListener<String> { }
	
	public interface LaunchPointsListener extends ResponseListener<JSONArray> { }

	static String FOREGROUND_APP = "ssap://com.webos.applicationManager/getForegroundAppInfo";
	static String APP_STATUS = "ssap://com.webos.service.appstatus/getAppStatus";
	static String APP_STATE = "ssap://system.launcher/getAppState";
	static String VOLUME = "ssap://audio/getVolume";
	static String MUTE = "ssap://audio/getMute";
	static String VOLUME_STATUS = "ssap://audio/getStatus";
	static String CHANNEL_LIST = "ssap://tv/getChannelList";
	static String CHANNEL = "ssap://tv/getCurrentChannel";
	static String PROGRAM = "ssap://tv/getChannelProgramInfo";
	
	static final String CLOSE_APP_URI = "ssap://system.launcher/close";
	static final String CLOSE_MEDIA_URI = "ssap://media.viewer/close";
	static final String CLOSE_WEBAPP_URI = "ssap://webapp/closeWebApp";
	
	WebOSTVMouseSocketConnection mouseSocket;
	
	WebSocketClient mouseWebSocket;
	WebOSTVKeyboardInput keyboardInput;

	HashMap<String, URLServiceSubscription<ResponseListener<Object>>> mAppToAppSubscriptions;
	HashMap<String, MessageListener> mAppToAppMessageListeners;
    
	int nextRequestId = 1;
	URI uri;
    
	WebOSWebSocketClient socket;
	PairingType pairingType;

	TrustManager customTrustManager;
	
    JSONObject manifest;
    List<String> permissions;
    
    State state = State.INITIAL;
    static final int PORT = 3001;
    
	// Queue of commands that should be sent once register is complete
    LinkedHashSet<ServiceCommand<ResponseListener<Object>>> commandQueue = new LinkedHashSet<ServiceCommand<ResponseListener<Object>>>();
    
	public WebOSTVService(ServiceDescription serviceDescription, ServiceConfig serviceConfig) {
		super(serviceDescription, serviceConfig);
		
		setServiceDescription(serviceDescription);

		state = State.INITIAL;
		pairingType = PairingType.FIRST_SCREEN;
		
		mAppToAppSubscriptions = new HashMap<String, URLServiceSubscription<ResponseListener<Object>>>();
		mAppToAppMessageListeners = new HashMap<String, MessageListener>();
		
		setCapabilities();
		setDefaultManifest();
	}
	
	@Override
	public void setServiceDescription(ServiceDescription serviceDescription) {
		this.serviceDescription = serviceDescription;
		
		//  TODO: Fix this when coming back from the device store.
//		String serverInfo = serviceDescription.getResponseHeaders().get(Device.HEADER_SERVER).get(0);
//		String systemOS = serverInfo.split(" ")[0];
//		String[] versionComponents = systemOS.split("/");
//		String systemVersion = versionComponents[versionComponents.length - 1];
//		
//		this.serviceDescription.setVersion(systemVersion);
		this.serviceDescription.setVersion("");
	}
	
	public static JSONObject discoveryParameters() {
		JSONObject params = new JSONObject();
		
		try {
			params.put("serviceId", ID);
			params.put("filter", "urn:lge-com:service:webos-second-screen:1");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return params;
	}
	
	private void setDefaultManifest() {
		manifest = new JSONObject();
		permissions = getPermissions();
		
		try {
			manifest.put("manifestVersion", 1);
//			manifest.put("appId", 1);
//			manifest.put("vendorId", 1);
//			manifest.put("localizedAppNames", 1);
			manifest.put("permissions",  convertStringListToJSONArray(permissions));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private JSONArray convertStringListToJSONArray(List<String> list) {
		JSONArray jsonArray = new JSONArray();

		for(String str: list) {
			jsonArray.put(str);
		}
		
		return jsonArray;
	}
	
	@Override
	public void connect() {
		synchronized (this) {
			if (state != State.INITIAL) {
				Log.w("Connect SDK", "already connecting; not trying to connect again: " + state);
				return; // don't try to connect again while connected
			}
			
			state = State.CONNECTING;
		}
		
		String uriString = "wss://" + serviceDescription.getIpAddress() + ":" + PORT;
		
		try {
			uri = new URI(uriString);
			socket = new WebOSWebSocketClient(this, uri);
			
			setupSSL();
			
			Log.d("Connect SDK", "attempting to connect to " + serviceDescription.getIpAddress());
            
			socket.connect();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void disconnect() {
		Log.d("Connect SDK", "attempting to disconnect to " + serviceDescription.getIpAddress());

		Util.runOnUI(new Runnable() {
			
			@Override
			public void run() {
				for (ConnectableDeviceListenerPair pair: deviceListeners)
					pair.listener.onDeviceDisconnected(pair.device);

				deviceListeners.clear();
			}
		});

		if ( socket != null)
			socket.close();
		
		state = State.INITIAL;
		
		mAppToAppSubscriptions.clear();
		mAppToAppMessageListeners.clear();
	}
	
	protected void handleMessage(String data) {
		try {
			JSONObject obj = new JSONObject(data);
			
			handleMessage(obj);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	protected void handleMessage(JSONObject message) {

		String type = message.optString("type");
		Object payload = message.opt("payload");

		if (type.length() == 0)
			return;
		
		if ("p2p".equals(type))
		{
			String webAppId = null;
			
			webAppId = message.optString("from");
			
			if (webAppId.length() == 0)
				return;
			
			String subscriptionKey = null;
			
			for (String key : mAppToAppMessageListeners.keySet())
			{
				if (webAppId.contains(key))
				{
					subscriptionKey = key;
					break;
				}
			}
			
			if (subscriptionKey == null)
				return;
			
			MessageListener messageListener = mAppToAppMessageListeners.get(subscriptionKey);
			
			if (messageListener != null)
				messageListener.onMessage(payload);

		} else if ("response".equals(type)) {
		    String strId = message.optString("id");
		    
		    if ( isInteger(strId) ) {
			    Integer id = Integer.valueOf(strId);
			    
			    ServiceCommand<ResponseListener<Object>> request = (ServiceCommand<ResponseListener<Object>>) requests.get(id);
			    
			    if (request != null) {
//		        	Log.d("Connect SDK", "Found requests need to handle response");
				    if (payload != null) {
				    	Util.postSuccess(request.getResponseListener(), payload);
			        } 
			        else {
			           	Util.postError(request.getResponseListener(), new ServiceCommandError(-1, "JSON parse error", null));
			        }
		        	
			        if (!(request instanceof URLServiceSubscription)) {
			        	requests.remove(id);
			        }
			    } 
			    else {
			        System.err.println("no matching request id: " + strId + ", payload: " + payload.toString());
			    }
		    }
		} else if ("registered".equals(type)) {
			if ( !(serviceConfig instanceof WebOSTVServiceConfig) ) {
				serviceConfig = new WebOSTVServiceConfig(serviceConfig.getServiceUUID());
			}
			
			if (payload instanceof JSONObject) {
				String clientKey = ((JSONObject) payload).optString("client-key");
				((WebOSTVServiceConfig) serviceConfig).setClientKey(clientKey);
				
				// Track SSL certificate
				// Not the prettiest way to get it, but we don't have direct access to the SSLEngine
				((WebOSTVServiceConfig) serviceConfig).setServerCertificate(customTrustManager.getLastCheckedCertificate());
				
				handleRegistered();
			}
		} else if ("error".equals(type) && message instanceof JSONObject) {
		    String error = ((JSONObject) message).optString("error");
		    if (error.length() == 0)
		    	return;
		    
		    int errorCode = -1;
		    String errorDesc = null;
		    
		    try {
			    String [] parts = error.split(" ", 2);
			    errorCode = Integer.parseInt(parts[0]);
			    errorDesc = parts[1];
		    } catch (Exception e) {
		    	e.printStackTrace();
		    }
		    
	    	if (payload != null) {
	    		Log.d("Connect SDK", "Error Payload: " + payload.toString());
	    	}
		    
			if ( message.has("id") ) {
				String strId = message.optString("id");
				if (strId.length() == 0)
					return;
				
				Integer id = Integer.valueOf(strId);
			    ServiceCommand<ResponseListener<Object>> request = (ServiceCommand<ResponseListener<Object>>) requests.get(id);

		    	Log.d("Connect SDK", "Error Desc: " + errorDesc);
		    	
		    	if (request != null) {
		    		Util.postError(request.getResponseListener(), new ServiceCommandError(errorCode, errorDesc, payload));
				        
		    		if (!(request instanceof URLServiceSubscription)) 
		    			requests.remove(id);
		    		
	    			if ( errorCode == 403 ) {	// 403 User Denied Access 
			    		disconnect();
			    		return;
			    	}
		    	}
			}
		}
	}
	
	@Override
	public Launcher getLauncher() {
		return this;
	};
	
	@Override
	public CapabilityPriorityLevel getLauncherCapabilityLevel() {
		return CapabilityPriorityLevel.HIGH;
	}
	
	@Override
	public void launchApp(String appId, AppLaunchListener listener) {
		AppInfo appInfo = new AppInfo();
		appInfo.setId(appId);
		
		launchAppWithInfo(appInfo, listener);
	}

	@Override
	public void launchAppWithInfo(AppInfo appInfo, Launcher.AppLaunchListener listener) {
		launchAppWithInfo(appInfo, null, listener);
	}

	@Override
	public void launchAppWithInfo(final AppInfo appInfo, Object params, final Launcher.AppLaunchListener listener) {
		String uri = "ssap://system.launcher/launch";
		JSONObject payload = new JSONObject();
		
		final String appId = appInfo.getId();
		String contentId = null;
		
		if (params != null) {
			try {
				contentId = (String) ((JSONObject) params).get("contentId");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		try {
			payload.put("id", appId);
			if (contentId != null) {
				payload.put("contentId", contentId);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				JSONObject obj = (JSONObject) response;
				LaunchSession launchSession = new LaunchSession();
				
				launchSession.setService(WebOSTVService.this);
				launchSession.setAppId(obj.optString("id")); // note that response uses id to mean appId
				launchSession.setSessionId(obj.optString("sessionId"));
				launchSession.setSessionType(LaunchSessionType.App);
				
				Util.postSuccess(listener, launchSession);
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		};
	
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, payload, true, responseListener);
		request.send();		
	}
	
	private void launchBrowser(final Launcher.AppLaunchListener listener) {
		final String appId = "com.webos.app.browser";
		
		String uri = "ssap://system.launcher/open";
		JSONObject payload = new JSONObject();
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
						
			@Override
			public void onSuccess(Object response) {
				JSONObject obj = (JSONObject) response;
				LaunchSession launchSession = new LaunchSession();
				
				launchSession.setService(WebOSTVService.this);
				launchSession.setAppId(obj.optString("id")); // note that response uses id to mean appId
				launchSession.setSessionId(obj.optString("sessionId"));
				launchSession.setSessionType(LaunchSessionType.App);
				launchSession.setRawData(obj);
				
				Util.postSuccess(listener, launchSession);
			}
						
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		};
					
		try {
			payload.put("id", appId);
		} catch (JSONException e) {
			e.printStackTrace();
		}
					
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, payload, true, responseListener);
		request.send();
	}

	
	@Override
	public void launchBrowser(String url, final Launcher.AppLaunchListener listener) {
		String uri = "ssap://system.launcher/open";
		JSONObject payload = new JSONObject();

		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				JSONObject obj = (JSONObject) response;
				LaunchSession launchSession = new LaunchSession();
				
				launchSession.setService(WebOSTVService.this);
				launchSession.setAppId(obj.optString("id")); // note that response uses id to mean appId
				launchSession.setSessionId(obj.optString("sessionId"));
				launchSession.setSessionType(LaunchSessionType.App);
				launchSession.setRawData(obj);
				
				Util.postSuccess(listener, launchSession);
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		};
		
		try {
			payload.put("target", url);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, payload, true, responseListener);
		request.send();
	}
	
	@Override
	public void launchYouTube(String contentId, Launcher.AppLaunchListener listener) {
		JSONObject params = new JSONObject();

		try {
			params.put("contentId", contentId);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		AppInfo appInfo = new AppInfo() {{
			setId("youtube.leanback.v4");
			setName("YouTube");
		}};
		
		launchAppWithInfo(appInfo, params, listener);
	}

	@Override
	public void launchHulu(String contentId, Launcher.AppLaunchListener listener) {
		JSONObject params = new JSONObject();

		try {
			params.put("contentId", contentId);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		AppInfo appInfo = new AppInfo() {{
			setId("hulu");
			setName("Hulu");
		}};
		
		launchAppWithInfo(appInfo, params, listener);
	}

	@Override
	public void launchNetflix(String contentId, Launcher.AppLaunchListener listener) {
		JSONObject params = new JSONObject();

		try {
			params.put("contentId", contentId);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		AppInfo appInfo = new AppInfo() {{
			setId("netflix");
			setName("Netflix");
		}};
		
		launchAppWithInfo(appInfo, params, listener);
	}
	
	@Override
	public void launchAppStore(String appId, AppLaunchListener listener) {
		AppInfo appInfo = new AppInfo("com.webos.app.discovery");
		appInfo.setName("LG Store");
		
		JSONObject params = new JSONObject();
		
		if (appId != null && appId.length() > 0) {
			String query = String.format("category/GAME_APPS/%s", appId);
			try {
				params.put("query", query);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		launchAppWithInfo(appInfo, params, listener);
	}
	
	@Override
	public void closeApp(LaunchSession launchSession, ResponseListener<Object> listener) {
		String uri = "ssap://system.launcher/close";
		String appId = launchSession.getAppId();
		
		JSONObject payload = new JSONObject();
		
		try {
			payload.put("id", appId);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(launchSession.getService(), uri, payload, true, listener);
		request.send();		
	}
	
	@Override
	public void getAppList(final AppListListener listener) {
		String uri = "ssap://com.webos.applicationManager/listApps";

        ResponseListener<Object> responseListener = new ResponseListener<Object>() {
                
        	@Override
        	public void onSuccess(Object response) {

        		try {
        			JSONObject jsonObj = (JSONObject) response;
        			
        			JSONArray apps = (JSONArray) jsonObj.get("apps");
        			List<AppInfo> appList = new ArrayList<AppInfo>();
        			
        			for (int i = 0; i < apps.length(); i++)
        			{
        				final JSONObject appObj = apps.getJSONObject(i);

        				AppInfo appInfo = new AppInfo() {{
						setId(appObj.getString("id"));
    						setName(appObj.getString("title"));
    						setRawData(appObj);
    					}};
        				
        				appList.add(appInfo);
        			}
        			
    				Util.postSuccess(listener, appList);
        		} catch (JSONException e) {
        			e.printStackTrace();
        		}
        	}

        	@Override
        	public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
        	}
        };

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, responseListener);
        request.send();        
	}

	private ServiceCommand<AppInfoListener> getRunningApp(boolean isSubscription, final AppInfoListener listener) {
		ServiceCommand<AppInfoListener> request;
		
		 ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				final JSONObject jsonObj = (JSONObject)response;
				AppInfo app = new AppInfo() {{
					setId(jsonObj.optString("appId"));
					setName(jsonObj.optString("appName"));
					setRawData(jsonObj);
				}};

				Util.postSuccess(listener, app);
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		};
		
		if (isSubscription)
			request = new URLServiceSubscription<AppInfoListener>(this, FOREGROUND_APP, null, true, responseListener);
		else
			request = new ServiceCommand<AppInfoListener>(this, FOREGROUND_APP, null, true, responseListener);
	
		request.send();
		
		return request;
	}
	
	@Override
	public void getRunningApp(AppInfoListener listener) {
		getRunningApp(false, listener);
	}
	
	@Override
	public ServiceSubscription<AppInfoListener> subscribeRunningApp(AppInfoListener listener) {
		return (URLServiceSubscription<AppInfoListener>) getRunningApp(true, listener);
	}
	
	private ServiceCommand<AppStateListener> getAppState(boolean subscription, LaunchSession launchSession, final AppStateListener listener) {
		ServiceCommand<AppStateListener> request;
		JSONObject params = new JSONObject();
		
		try {
			params.put("appId", launchSession.getAppId());
			params.put("sessionId", launchSession.getSessionId());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
			
			@Override
			public void onSuccess(Object object) {
				JSONObject json = (JSONObject) object;
				try {
					Util.postSuccess(listener, new AppState(json.getBoolean("running"), json.getBoolean("visible")));
				} catch (JSONException e) {
					Util.postError(listener, new ServiceCommandError(0, "Malformed JSONObject", null));
					e.printStackTrace();
				}
			}
		};

		if (subscription) {
			request = new URLServiceSubscription<Launcher.AppStateListener>(this, APP_STATE, params, true, responseListener);
		} else {
			request = new ServiceCommand<Launcher.AppStateListener>(this, APP_STATE, params, true, responseListener);
		}
		
		request.send();
		
		return request;
	}
	
	@Override
	public void getAppState(LaunchSession launchSession, AppStateListener listener) {
		getAppState(false, launchSession, listener);
	}
	
	@Override
	public ServiceSubscription<AppStateListener> subscribeAppState(LaunchSession launchSession, AppStateListener listener) {
		return (URLServiceSubscription<AppStateListener>) getAppState(true, launchSession, listener);
	}
	
	
	/******************
    TOAST CONTROL
    *****************/
	@Override
	public ToastControl getToastControl() {
		return this;
	};
	
	@Override
	public CapabilityPriorityLevel getToastControlCapabilityLevel() {
		return CapabilityPriorityLevel.HIGH;
	}
	
	@Override
	public void showToast(String message, ResponseListener<Object> listener) {
		showToast(message, null, null, listener);
	}
	
	@Override
    public void showToast(String message, String iconData, String iconExtension, ResponseListener<Object> listener)
    {
        JSONObject payload = new JSONObject();

        try {
        	payload.put("message", message);
                
        	if (iconData != null)
        	{
        		payload.put("iconData", iconData);
        		payload.put("iconExtension", iconExtension);
        	}
        } catch (JSONException e) {
        	e.printStackTrace();
        }
        
        sendToast(payload, listener);
    }
    
	@Override
	public void showClickableToastForApp(String message, AppInfo appInfo, JSONObject params, ResponseListener<Object> listener) {
		showClickableToastForApp(message, appInfo, params, null, null, listener);
	}

	@Override
    public void showClickableToastForApp(String message, AppInfo appInfo, JSONObject params, String iconData, String iconExtension, ResponseListener<Object> listener) {
        JSONObject payload = new JSONObject();
        
        try {
            payload.put("message", message);
            
            if (iconData != null) {
            	payload.put("iconData", iconData);
            	payload.put("iconExtension", iconExtension);
            }

            if (appInfo != null) {
            	JSONObject onClick = new JSONObject();
            	onClick.put("appId", appInfo.getId());
            	if (params != null) {
            		onClick.put("params", params);
            	}
            	payload.put("onClick", onClick);
            }
        } catch (JSONException e) {
        	e.printStackTrace();
        }

        sendToast(payload, listener);
	}

	@Override
	public void showClickableToastForURL(String message, String url, ResponseListener<Object> listener) {
		showClickableToastForURL(message, url, null, null, listener);
	}
	
	@Override
	public void showClickableToastForURL(String message, String url, String iconData, String iconExtension, ResponseListener<Object> listener) {
        JSONObject payload = new JSONObject();
        
        try {
            payload.put("message", message);
            
            if (iconData != null) {
                payload.put("iconData", iconData);
                payload.put("iconExtension", iconExtension);
            }

            if (url != null) {
                JSONObject onClick = new JSONObject();
                onClick.put("target", url);
                payload.put("onClick", onClick);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendToast(payload, listener);
	}
	
	private void sendToast(JSONObject payload, ResponseListener<Object> listener) {
		if (!payload.has("iconData"))
        {
            Context context = DiscoveryManager.getInstance().getContext();
            
            try {
            	Drawable drawable = context.getPackageManager().getApplicationIcon(context.getPackageName());
                    
            	if(drawable != null) {
                    BitmapDrawable bitDw = ((BitmapDrawable) drawable);
                    Bitmap bitmap = bitDw.getBitmap();

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

                    byte[] bitmapByte = stream.toByteArray();
                    bitmapByte = Base64.encode(bitmapByte,Base64.NO_WRAP);
                    String bitmapData = new String(bitmapByte);
                    
                    payload.put("iconData", bitmapData);
                    payload.put("iconExtension", "png");
            	}
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
		
		String uri = "palm://system.notifications/createToast";		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, payload, true, listener);
		request.send();
	}
	
	
	/******************
    VOLUME CONTROL
    *****************/
	@Override
	public VolumeControl getVolumeControl() {
		return this;
	};
	
	@Override
	public CapabilityPriorityLevel getVolumeControlCapabilityLevel() {
		return CapabilityPriorityLevel.HIGH;
	}
	
	public void volumeUp() {
		volumeUp(null);
	}
	
	@Override
	public void volumeUp(ResponseListener<Object> listener) {
		String uri = "ssap://audio/volumeUp";
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, listener);
		
		request.send();	
	}

	public void volumeDown() {
		volumeDown(null);
	}
	
	@Override
	public void volumeDown(ResponseListener<Object> listener) {
		String uri = "ssap://audio/volumeDown";
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, listener);
		
		request.send();
	}

	public void setVolume(int volume) { 
		setVolume(volume, null);
	}
	
	@Override
	public void setVolume(float volume, ResponseListener<Object> listener) {
		String uri = "ssap://audio/setVolume";
		JSONObject payload = new JSONObject();
		
		try {
			payload.put("volume", (volume * 100.0f));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, payload, true, listener);
		request.send();
	}
	
	private ServiceCommand<VolumeListener> getVolume(boolean isSubscription, final VolumeListener listener) {
		ServiceCommand<VolumeListener> request;
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
		
				try {
					JSONObject jsonObj = (JSONObject)response;
					int iVolume = (Integer) jsonObj.get("volume");
					float fVolume = (float) (iVolume / 100.0);
					
						Util.postSuccess(listener, fVolume);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void onError(ServiceCommandError error) {
					Util.postError(listener, error);
			}
		};
		
		if (isSubscription)
			request = new URLServiceSubscription<VolumeListener>(this, VOLUME, null, true, responseListener);
		else
			request = new ServiceCommand<VolumeListener>(this, VOLUME, null, true, responseListener);
		
		request.send();	
		
		return request;
	}

	@Override
	public void getVolume(VolumeListener listener) {
		getVolume(false, listener);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ServiceSubscription<VolumeListener> subscribeVolume(VolumeListener listener) {
		return (ServiceSubscription<VolumeListener>) getVolume(true, listener);
	}

	@Override
	public void setMute(boolean isMute, ResponseListener<Object> listener) {
		String uri = "ssap://audio/setMute";
		JSONObject payload = new JSONObject();
		
		try {
			payload.put("mute", isMute);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, payload, true, listener);
		request.send();
	}

	private ServiceCommand<ResponseListener<Object>> getMuteStatus(boolean isSubscription, final MuteListener listener) {
		ServiceCommand<ResponseListener<Object>> request;
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				try {
					JSONObject jsonObj = (JSONObject)response;
					boolean isMute = (Boolean) jsonObj.get("mute");
					Util.postSuccess(listener, isMute);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		};
		
		if (isSubscription)
			request = new URLServiceSubscription<ResponseListener<Object>>(this, MUTE, null, true, responseListener);
		else
			request = new ServiceCommand<ResponseListener<Object>>(this, MUTE, null, true, responseListener);
		
		request.send();	
		
		return request;
	}
		
	@Override
	public void getMute(MuteListener listener) {
		getMuteStatus(false, listener);		
	}

	@SuppressWarnings("unchecked")
	@Override
	public ServiceSubscription<MuteListener> subscribeMute(MuteListener listener) {
		return (ServiceSubscription<MuteListener>) getMuteStatus(true, listener);		
	}
	
    private ServiceCommand<ResponseListener<Object>> getVolumeStatus(boolean isSubscription, final VolumeStatusListener listener) {
        ServiceCommand<ResponseListener<Object>> request;
        
        ResponseListener<Object> responseListener = new ResponseListener<Object>() {
                
        	@Override
        	public void onSuccess(Object response) {
        		try {
        			JSONObject jsonObj = (JSONObject) response;
        			boolean isMute = (Boolean) jsonObj.get("mute");
        			int iVolume = jsonObj.getInt("volume");
        			float fVolume = (float) (iVolume / 100.0);
        			
        			Util.postSuccess(listener, new VolumeStatus(isMute, fVolume));
        		} catch (JSONException e) {
        			e.printStackTrace();
                }
            }
                
            @Override
            public void onError(ServiceCommandError error) {
    			Util.postError(listener, error);
            }
        };
        
        if (isSubscription)
        	request = new URLServiceSubscription<ResponseListener<Object>>(this, VOLUME_STATUS, null, true, responseListener);
        else
        	request = new ServiceCommand<ResponseListener<Object>>(this, VOLUME_STATUS, null, true, responseListener);

        request.send();
        
        return request;
    }

    public void getVolumeStatus(VolumeStatusListener listener) {
        getVolumeStatus(false, listener);
    }
	
	@SuppressWarnings("unchecked")
	public ServiceSubscription<VolumeStatusListener> subscribeVolumeStatus(VolumeStatusListener listener) {
        return (ServiceSubscription<VolumeStatusListener>) getVolumeStatus(true, listener);
	}
	
	
	/******************
    MEDIA PLAYER
    *****************/
	@Override
	public MediaPlayer getMediaPlayer() {
		return this;
	};
	
	@Override
	public CapabilityPriorityLevel getMediaPlayerCapabilityLevel() {
		return CapabilityPriorityLevel.NORMAL;
	}
	
	protected void displayMedia(final String type, final String url, final String mimeType, final String title, final String description, final String iconSrc, final boolean shouldLoop, final MediaPlayer.LaunchListener listener) {
		String uri = "ssap://media.viewer/open";
		JSONObject payload = null;
		
		try {
			payload = new JSONObject() {{
				put("mediaType", type == null ? NULL : type);
				put("target", url);
				put("title", title == null ? NULL : title);
				put("description", description == null ? NULL : description);
				put("mimeType", mimeType == null ? NULL : mimeType);
				put("loop", shouldLoop);
				put("iconSrc", iconSrc == null ? NULL : iconSrc);
			}};
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		if (type.equals("image")) {

			ResponseListener<Object> responseListener = new ResponseListener<Object>() {
				@Override
				public void onSuccess(Object response) {
					JSONObject obj = (JSONObject) response;
					LaunchSession launchSession = LaunchSession.launchSessionForAppId(obj.optString("id"));
					launchSession.setSessionId(obj.optString("sessionId"));
					launchSession.setService(WebOSTVService.this);
					launchSession.setSessionId(obj.optString("sessionId"));
					launchSession.setSessionType(LaunchSessionType.Media);

					Util.postSuccess(listener, new MediaLaunchObject(launchSession, WebOSTVService.this));
				}

				@Override
				public void onError(ServiceCommandError error) {
					Util.postError(listener, error);
				}
			};

			ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, payload, true, responseListener);
			request.send();
		} else {
			launchWebApp("MediaPlayer", payload, new WebAppSession.LaunchListener() {
				
				@Override
				public void onError(ServiceCommandError error) {
					Util.postError(listener, error);
				}
				
				@Override
				public void onSuccess(WebAppSession launchSession) {
					Util.postSuccess(listener, new MediaLaunchObject(launchSession.launchSession, launchSession));
				}
			});
		}
	}
	
	@Override
	public void displayImage(final String url, final String mimeType, final String title, final String description, final String iconSrc, final MediaPlayer.LaunchListener listener) {
		final String webAppId = "MediaPlayer";
		
		final WebAppSession.LaunchListener webAppLaunchListener = new WebAppSession.LaunchListener() {
			
			@Override
			public void onError(ServiceCommandError error) {
				listener.onError(error);
			}
			
			@Override
			public void onSuccess(WebAppSession webAppSession) {
				webAppSession.displayImage(url, mimeType, title, description, iconSrc, listener);
			}
		};
		
		this.getWebAppLauncher().launchWebApp(webAppId, webAppLaunchListener);
	}

	@Override
	public void playMedia(final String url, final String mimeType, final String title, final String description, final String iconSrc, final boolean shouldLoop, final MediaPlayer.LaunchListener listener) {
		final String webAppId = "MediaPlayer";
		
		final WebAppSession.LaunchListener webAppLaunchListener = new WebAppSession.LaunchListener() {
			
			@Override
			public void onError(ServiceCommandError error) {
				listener.onError(error);
			}
			
			@Override
			public void onSuccess(WebAppSession webAppSession) {
				webAppSession.playMedia(url, mimeType, title, description, iconSrc, shouldLoop, listener);
			}
		};
		
		this.getWebAppLauncher().launchWebApp(webAppId, webAppLaunchListener);
	}
	
	@Override
	public void closeMedia(LaunchSession launchSession, ResponseListener<Object> listener) {
		JSONObject payload = new JSONObject();
		
		try {
			if (launchSession.getAppId() != null && launchSession.getAppId().length() > 0)
				payload.put("id", launchSession.getAppId());
			
			if (launchSession.getSessionId() != null && launchSession.getSessionId().length() > 0)
				payload.put("sessionId", launchSession.getSessionId());
		} catch (JSONException e) {
			e.printStackTrace();
		}
	
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(launchSession.getService(), CLOSE_MEDIA_URI, payload, true, listener);
		request.send();
	}
	
	/******************
    MEDIA CONTROL
    *****************/
	@Override
	public MediaControl getMediaControl() {
		return this;
	};
	
	@Override
	public CapabilityPriorityLevel getMediaControlCapabilityLevel() {
		return CapabilityPriorityLevel.NORMAL;
	}

	@Override
	public void play(ResponseListener<Object> listener) {
		String uri = "ssap://media.controls/play";
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, listener);
	
		request.send();		
	}

	@Override
	public void pause(ResponseListener<Object> listener) {
		String uri = "ssap://media.controls/pause";
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, listener);
	
		request.send();		
	}

	@Override
	public void stop(ResponseListener<Object> listener) {
		String uri = "ssap://media.controls/stop";
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, listener);
	
		request.send();		
	}

	@Override
	public void rewind(ResponseListener<Object> listener) {
		String uri = "ssap://media.controls/rewind";
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, listener);
	
		request.send();
	}

	@Override
	public void fastForward(ResponseListener<Object> listener) {
		String uri = "ssap://media.controls/fastForward";
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, listener);
	
		request.send();	
	}
	
	@Override
	public void seek(long position, ResponseListener<Object> listener) {
		Util.postError(listener, ServiceCommandError.notSupported());
	}
	
	@Override
	public void getDuration(DurationListener listener) {
		Util.postError(listener, ServiceCommandError.notSupported());
	}
	
	@Override
	public void getPosition(PositionListener listener) {
		Util.postError(listener, ServiceCommandError.notSupported());
	}


	/******************
    TV CONTROL
    *****************/
	@Override
	public TVControl getTVControl() {
		return this;
	};
	
	@Override
	public CapabilityPriorityLevel getTVControlCapabilityLevel() {
		return CapabilityPriorityLevel.HIGH;
	}
	
	public void channelUp() {
		channelDown(null);
	}
	
	@Override
	public void channelUp(ResponseListener<Object> listener) {
		String uri = "ssap://tv/channelUp";
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, listener);
		request.send();
	}

	public void channelDown() {
		channelDown(null);
	}
	
	@Override
	public void channelDown(ResponseListener<Object> listener) {
		String uri = "ssap://tv/channelDown";

		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, listener);
		request.send();
	}

	@Override
	public void setChannel(ChannelInfo channelInfo, ResponseListener<Object> listener) {
		String uri = "ssap://tv/openChannel";
		JSONObject payload = new JSONObject();
		
		try {
			payload.put("channelNumber", channelInfo.getNumber());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, payload, true, listener);
		request.send();		
	}
	
	public void setChannelById(String channelId) {
		setChannelById(channelId, null);
	}
	
	public void setChannelById(String channelId, ResponseListener<Object> listener) {
		String uri = "ssap://tv/openChannel";
		JSONObject payload = new JSONObject();
		
		try {
			payload.put("channelId", channelId);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, payload, true, listener);
		request.send();
	}

	private ServiceCommand<ResponseListener<Object>> getCurrentChannel(boolean isSubscription, final ChannelListener listener) {
		ServiceCommand<ResponseListener<Object>> request;
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				JSONObject jsonObj = (JSONObject) response;
				ChannelInfo channel = parseRawChannelData(jsonObj);
				
				Util.postSuccess(listener, channel);
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		};
		
		if (isSubscription) {
			request = new URLServiceSubscription<ResponseListener<Object>>(this, CHANNEL, null, true, responseListener);
		}
		else
			request = new ServiceCommand<ResponseListener<Object>>(this, CHANNEL, null, true, responseListener);
		
		request.send();	
		
		return request;
	}
	
	@Override
	public void getCurrentChannel(ChannelListener listener) {
		getCurrentChannel(false, listener);
	}

	@SuppressWarnings("unchecked")
	@Override
	public ServiceSubscription<ChannelListener> subscribeCurrentChannel(ChannelListener listener) {
		return (ServiceSubscription<ChannelListener>) getCurrentChannel(true, listener);
	}
	
	private ServiceCommand<ResponseListener<Object>> getChannelList(boolean isSubscription, final ChannelListListener listener) {
		ServiceCommand<ResponseListener<Object>> request;
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				try {
					JSONObject jsonObj = (JSONObject)response;
					ArrayList<ChannelInfo> list = new ArrayList<ChannelInfo>();
					
					JSONArray array = (JSONArray) jsonObj.get("channelList");
					for (int i = 0; i < array.length(); i++) {
						JSONObject object = (JSONObject) array.get(i);
						
						ChannelInfo channel = parseRawChannelData(object);
						list.add(channel);
					}
					
						Util.postSuccess(listener, list);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		};
		
		if (isSubscription)
			request = new URLServiceSubscription<ResponseListener<Object>>(this, CHANNEL_LIST, null, true, responseListener);
		else
			request = new ServiceCommand<ResponseListener<Object>>(this, CHANNEL_LIST, null, true, responseListener);
	
		request.send();

		return request;
	}
	
	@Override
	public void getChannelList(ChannelListListener listener) {
		getChannelList(false, listener);
	}
	
	@SuppressWarnings("unchecked")
	public ServiceSubscription<ChannelListListener> subscribeChannelList(final ChannelListListener listener) {
		return (ServiceSubscription<ChannelListListener>) getChannelList(true, listener);
	}
	
	private ServiceCommand<ResponseListener<Object>> getProgramList(boolean isSubscription, final ProgramListListener listener) {
		ServiceCommand<ResponseListener<Object>> request;
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				try {
					JSONObject jsonObj = (JSONObject)response;
					JSONObject jsonChannel = (JSONObject) jsonObj.get("channel");
					ChannelInfo channel = parseRawChannelData(jsonChannel);
					JSONArray programList = (JSONArray) jsonObj.get("programList");
					
						Util.postSuccess(listener, new ProgramList(channel, programList));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void onError(ServiceCommandError error) {
					Util.postError(listener, error);
			}
		};
		
		if (isSubscription)
			request = new URLServiceSubscription<ResponseListener<Object>>(this, PROGRAM, null, true, responseListener);
		else
			request = new ServiceCommand<ResponseListener<Object>>(this, PROGRAM, null, true, responseListener);
				
		request.send();
		
		return request;
	}
	
	@Override
	public void getProgramInfo(ProgramInfoListener listener) {
		// TODO need to parse current program when program id is correct
		Util.postError(listener, ServiceCommandError.notSupported());
	}
	
	@Override
	public ServiceSubscription<ProgramInfoListener> subscribeProgramInfo(ProgramInfoListener listener) {
		Util.postError(listener, ServiceCommandError.notSupported());

		return new NotSupportedServiceSubscription<ProgramInfoListener>();
	}
	
	@Override
	public void getProgramList(ProgramListListener listener) {
		getProgramList(false, listener);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ServiceSubscription<ProgramListListener> subscribeProgramList(ProgramListListener listener) {
		return (ServiceSubscription<ProgramListListener>) getProgramList(true, listener);
	}

	@Override
	public void set3DEnabled(final boolean enabled, final ResponseListener<Object> listener) {
		String uri; 
		if (enabled == true)
			uri = "ssap://com.webos.service.tv.display/set3DOn";
		else
			uri = "ssap://com.webos.service.tv.display/set3DOff";
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, listener);

		request.send();
	}

	private ServiceCommand<State3DModeListener> get3DEnabled(boolean isSubscription, final State3DModeListener listener) {
		String uri = "ssap://com.webos.service.tv.display/get3DStatus";
	
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				JSONObject jsonObj = (JSONObject)response;

				JSONObject status;
				try {
					status = jsonObj.getJSONObject("status3D");
					boolean isEnabled = status.getBoolean("status");
					
					Util.postSuccess(listener, isEnabled);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		};
		
		ServiceCommand<State3DModeListener> request;
		if (isSubscription == true) 
			request = new URLServiceSubscription<State3DModeListener>(this, uri, null, true, responseListener);
		else 
			request = new ServiceCommand<State3DModeListener>(this, uri, null, true, responseListener);

		request.send();
		
		return request;
	}
	
	@Override
	public void get3DEnabled(final State3DModeListener listener) {
		get3DEnabled(false, listener);
	}

	@SuppressWarnings("unchecked")
	@Override
	public ServiceSubscription<State3DModeListener> subscribe3DEnabled(final State3DModeListener listener) {
		return (ServiceSubscription<State3DModeListener>) get3DEnabled(true, listener);
	}
	
	
    /**************
    EXTERNAL INPUT
    **************/
	@Override
	public ExternalInputControl getExternalInput() {
		return this;
	};
	
	@Override
	public CapabilityPriorityLevel getExternalInputControlPriorityLevel() {
		return CapabilityPriorityLevel.HIGH;
	}
	
	@Override
	public void launchInputPicker(final AppLaunchListener listener) {
		AppInfo appInfo = new AppInfo() {{
			setId("com.webos.app.inputpicker");
			setName("InputPicker");
		}};
		
		launchAppWithInfo(appInfo, null, listener);
	}

	@Override
	public void closeInputPicker(LaunchSession launchSession, ResponseListener<Object> listener) {
		closeApp(launchSession, listener);
	}
	
	@Override
	public void getExternalInputList(final ExternalInputListListener listener) {
		String uri = "ssap://tv/getExternalInputList";
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				try {
					JSONObject jsonObj = (JSONObject)response;
					JSONArray devices = (JSONArray) jsonObj.get("devices");
					Util.postSuccess(listener, externalnputInfoFromJSONArray(devices));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		};
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, responseListener);
		request.send();
	}
	
	@Override
	public void setExternalInput(ExternalInputInfo externalInputInfo , final ResponseListener<Object> listener) {
		String uri = "ssap://tv/switchInput";
		
		JSONObject payload = new JSONObject();
		
		try {
			if (externalInputInfo  != null && externalInputInfo .getId() != null) {
				payload.put("inputId", externalInputInfo.getId());
			}
			else {
				Log.w("Connect SDK", "ExternalInputInfo has no id");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, payload, true, listener);
		request.send();
	}
	
	
    /**************
    MOUSE CONTROL
    **************/
	@Override
	public MouseControl getMouseControl() {
		return this;
	};
	
	@Override
	public CapabilityPriorityLevel getMouseControlCapabilityLevel() {
		return CapabilityPriorityLevel.HIGH;
	}
	
	@Override
	public void connectMouse() {
		if (mouseSocket != null) 
			return;
		
        ResponseListener<Object> listener = new ResponseListener<Object>() {
                
        	@Override
        	public void onSuccess(Object response) {
        		try {
					JSONObject jsonObj = (JSONObject)response;
        			String socketPath = (String) jsonObj.get("socketPath");
        			mouseSocket = new WebOSTVMouseSocketConnection(socketPath);
        		} catch (JSONException e) {
        			e.printStackTrace();
        		}
        	}
                
        	@Override
        	public void onError(ServiceCommandError error) {
        	}
        };
        
        connectMouse(listener);
	}
	
	@Override
	public void disconnectMouse() {
		if (mouseSocket == null)
			return;

		mouseSocket.disconnect();
		mouseSocket = null;
	}
	
	private void connectMouse(ResponseListener<Object> listener) {
		String uri = "ssap://com.webos.service.networkinput/getPointerInputSocket";

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, listener);
        request.send();
	}
	
	@Override
	public void click() {
		if (mouseSocket != null) 
			mouseSocket.click();
		else {
			connectMouse();
		}
	}
	
	@Override
	public void move(double dx, double dy) {
		if (mouseSocket != null)
			mouseSocket.move(dx, dy);
		else 
			Log.w("Connect SDK", "Mouse Socket is not ready yet");
	}
	
	@Override
	public void move(PointF diff) {
		move(diff.x, diff.y);
	}
	
	@Override
	public void scroll(double dx, double dy) {
		if (mouseSocket != null) 
			mouseSocket.scroll(dx, dy);
		else 
			Log.w("Connect SDK", "Mouse Socket is not ready yet");
	}
	
	@Override
	public void scroll(PointF diff) {
		scroll(diff.x, diff.y);
	}
	
	
    /**************
    KEYBOARD CONTROL
    **************/
	@Override
	public TextInputControl getTextInputControl() {
		return this;
	};
	
	@Override
	public CapabilityPriorityLevel getTextInputControlCapabilityLevel() {
		return CapabilityPriorityLevel.HIGH;
	}
	
	@Override
	public ServiceSubscription<TextInputStatusListener> subscribeTextInputStatus(TextInputStatusListener listener) {
		keyboardInput = new WebOSTVKeyboardInput(this);
		return keyboardInput.connect(listener);
	}
	
	@Override
	public void sendText(String input) {
		if (keyboardInput != null) {
			keyboardInput.addToQueue(input);
		}
	}
	
	@Override
	public void sendKeyCode(int keyCode, ResponseListener<Object> listener) {
		Util.postError(listener, ServiceCommandError.notSupported());
	}

	@Override
	public void sendEnter() {
		if (keyboardInput != null) {
			keyboardInput.sendEnter();
		}
	}

	@Override
	public void sendDelete() {
		if (keyboardInput != null) {
			keyboardInput.sendDel();
		}
	}

	
    /**************
    POWER CONTROL
    **************/
	@Override
	public PowerControl getPowerControl() {
		return this;
	};
	
	@Override
	public CapabilityPriorityLevel getPowerControlCapabilityLevel() {
		return CapabilityPriorityLevel.HIGH;
	}
	
	@Override
	public void powerOff(ResponseListener<Object> listener) {
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				
			}
		};
		
        String uri = "ssap://system/turnOff";
        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, responseListener);

        request.send();
	}

	
    /**************
    KEY CONTROL
    **************/
	@Override
	public KeyControl getKeyControl() {
		return this;
	}

	@Override
	public CapabilityPriorityLevel getKeyControlCapabilityLevel() {
		return CapabilityPriorityLevel.HIGH;
	}

	private void sendSpecialKey(final String key) {
		if (mouseSocket != null) {
			mouseSocket.button(key);
		}
		else {
	        ResponseListener<Object> listener = new ResponseListener<Object>() {
	            
	        	@Override
	        	public void onSuccess(Object response) {
	        		try {
						JSONObject jsonObj = (JSONObject)response;
	        			String socketPath = (String) jsonObj.get("socketPath");
	        			mouseSocket = new WebOSTVMouseSocketConnection(socketPath);
	        			
	        			mouseSocket.button(key);
	        		} catch (JSONException e) {
	        			e.printStackTrace();
	        		}
	        	}
	                
	        	@Override
	        	public void onError(ServiceCommandError error) {
	        	}
	        };

	        connectMouse(listener);
		}
	}
	
	@Override
	public void up(ResponseListener<Object> listener) {
		sendSpecialKey("UP");
	}

	@Override
	public void down(ResponseListener<Object> listener) {
		sendSpecialKey("DOWN");
	}

	@Override
	public void left(ResponseListener<Object> listener) {
		sendSpecialKey("LEFT");
	}

	@Override
	public void right(ResponseListener<Object> listener) {
		sendSpecialKey("RIGHT");
	}

	@Override
	public void ok(ResponseListener<Object> listener) {
		if (mouseSocket != null) {
			mouseSocket.click();
		}
		else {
	        ResponseListener<Object> responseListener = new ResponseListener<Object>() {
	            
	        	@Override
	        	public void onSuccess(Object response) {
	        		try {
						JSONObject jsonObj = (JSONObject)response;
	        			String socketPath = (String) jsonObj.get("socketPath");
	        			mouseSocket = new WebOSTVMouseSocketConnection(socketPath);
	        			
	        			mouseSocket.click();
	        		} catch (JSONException e) {
	        			e.printStackTrace();
	        		}
	        	}
	                
	        	@Override
	        	public void onError(ServiceCommandError error) {
	        	}
	        };

	        connectMouse(responseListener);
		}
	}

	@Override
	public void back(ResponseListener<Object> listener) {
		sendSpecialKey("BACK");
	}

	@Override
	public void home(ResponseListener<Object> listener) {
		sendSpecialKey("HOME");
	}

	
    /**************
    Web App Launcher
    **************/
	
	@Override
	public WebAppLauncher getWebAppLauncher() {
		return this;
	}

	@Override
	public CapabilityPriorityLevel getWebAppLauncherCapabilityLevel() {
		return CapabilityPriorityLevel.HIGH;
	}

	@Override
	public void launchWebApp(final String webAppId, final WebAppSession.LaunchListener listener) {
		this.launchWebApp(webAppId, null, listener);
	}
	
	@Override
	public void launchWebApp(String webAppId, boolean relaunchIfRunning, WebAppSession.LaunchListener listener) {
		launchWebApp(webAppId, null, relaunchIfRunning, listener);
	}
	
	@Override
	public void launchWebApp(final String webAppId, JSONObject params, boolean relaunchIfRunning, final WebAppSession.LaunchListener listener) {
		if (webAppId == null) {
			Util.postError(listener, new ServiceCommandError(0, "Must pass a web App id", null));
			return;
		}
		
		if (relaunchIfRunning) {
			launchWebApp(webAppId, params, listener);
		} else {
			getLauncher().getRunningApp(new AppInfoListener() {
				
				@Override
				public void onError(ServiceCommandError error) {
					Util.postError(listener, error);
				}
				
				@Override
				public void onSuccess(AppInfo appInfo) {
					//  TODO: this will only work on pinned apps, currently
					if (appInfo.getId().indexOf(webAppId) != -1) {
						LaunchSession launchSession = LaunchSession.launchSessionForAppId(webAppId);
						launchSession.setSessionType(LaunchSessionType.WebApp);
						launchSession.setService(WebOSTVService.this);
						
						WebOSWebAppSession webAppSession = new WebOSWebAppSession(launchSession, WebOSTVService.this);
						
						Util.postSuccess(listener, webAppSession);
					}
				}
			});
		}
	}
	
	public void launchWebApp(final String webAppId, final JSONObject params, final WebAppSession.LaunchListener listener) {
		if (webAppId == null) {
			Util.postError(listener, new ServiceCommandError(-1, "You need to provide a webAppId.", null));
		}
		
		String uri = "ssap://webapp/launchWebApp";
		JSONObject payload = new JSONObject();
		
		try {
			payload.put("webAppId", webAppId);
			
			if (params != null)
				payload.put("urlParams", params);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(final Object response) {
				JSONObject obj = (JSONObject) response;
				LaunchSession launchSession = LaunchSession.launchSessionForAppId(webAppId);
				launchSession.setService(WebOSTVService.this);
				launchSession.setSessionId(obj.optString("sessionId"));
				launchSession.setSessionType(LaunchSessionType.WebApp);
				launchSession.setRawData(obj);

				Util.postSuccess(listener, new WebOSWebAppSession(launchSession, WebOSTVService.this));
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		};
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, payload, true, responseListener);
		request.send();
	}
	
	@Override
	public void closeWebApp(LaunchSession launchSession, ResponseListener<Object> listener) {
		String uri = "ssap://webapp/closeWebApp";
		JSONObject payload = new JSONObject();
		
		try {
			payload.put("webAppId", launchSession.getAppId());
			payload.put("sessionId", launchSession.getSessionId());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, payload, true, listener);
		request.send();
	}
	
	public void connectToWebApp(final WebOSWebAppSession webAppSession, final ResponseListener<Object> connectionListener) {
		connectToWebApp(webAppSession, false, connectionListener);
	}
	
	public void connectToWebApp(final WebOSWebAppSession webAppSession, final boolean joinOnly, final ResponseListener<Object> connectionListener) {
		if (webAppSession == null || webAppSession.launchSession == null || webAppSession.launchSession.getRawData() == null) {
			Util.postError(connectionListener, new ServiceCommandError(0, "You must provide a valide Webapp Session", null));
			
			return;
		}
		
		final String webAppHostId = String.format("com.webos.app.webapphost.%s", webAppSession.launchSession.getAppId());
		
		final LaunchSession launchSession = webAppSession.launchSession;
		String uri = "ssap://webapp/connectToApp";
		JSONObject payload = new JSONObject();
		
		if (launchSession.getAppId() != null) {
			try {
				payload.put("webAppId", launchSession.getAppId());
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		if (mAppToAppSubscriptions.containsKey(webAppSession.launchSession.getAppId()) && mAppToAppMessageListeners.containsKey(webAppHostId)) {
			mAppToAppMessageListeners.put(webAppHostId, webAppSession.messageHandler);
			
			Util.postSuccess(connectionListener, webAppSession);
			
			return;
		}
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				JSONObject jsonObj = (JSONObject)response;
				
				Log.d(Util.T, "Web app connection response: " + jsonObj.toString());
				
				String state = jsonObj.optString("state");
				
				if (!state.equalsIgnoreCase("CONNECTED")) {
					if (joinOnly && state.equalsIgnoreCase("WAITING_FOR_APP")) {
						Util.postError(connectionListener, new ServiceCommandError(0, "Web app is not currently running", null));
					}

					return;
				}
				
				if (webAppSession.messageHandler != null)
					mAppToAppMessageListeners.put(webAppHostId, webAppSession.messageHandler);
				
				if (connectionListener != null)
					connectionListener.onSuccess(null);
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				ServiceSubscription<ResponseListener<Object>> subscription = mAppToAppSubscriptions.get(launchSession.getAppId());
				if (subscription != null) {
					if (serviceDescription.getVersion().contains("4.0."))
						subscription.unsubscribe();

					mAppToAppMessageListeners.remove(subscription);
					mAppToAppSubscriptions.remove(webAppHostId);
				}
				
				//  TODO: There is a bit of code here that iOS is doing that I am not.  Is that needed?

				Util.postError(connectionListener, error);
			}
		};
		
		URLServiceSubscription<ResponseListener<Object>> subscription = new URLServiceSubscription<ResponseListener<Object>>(this, uri, payload, true, responseListener);
		subscription.send();
		
		mAppToAppSubscriptions.put(launchSession.getAppId(), subscription);
	}
	
	@Override
	public void joinWebApp(final LaunchSession webAppLaunchSession, final WebAppSession.LaunchListener listener) {
		final WebOSWebAppSession webAppSession = new WebOSWebAppSession(webAppLaunchSession, this);
		webAppSession.join(listener);
	}
	
	public void disconnectFromWebApp(WebOSWebAppSession webAppSession) {
		final String appId = webAppSession.launchSession.getAppId();
		
		if (appId == null)
			return;
		
		mAppToAppMessageListeners.remove(appId);
		
		ServiceSubscription<ResponseListener<Object>> connectionSubscription = mAppToAppSubscriptions.remove(appId);
		
		if (connectionSubscription != null) {
			if (!this.serviceDescription.getVersion().contains("4.0.")) {
				connectionSubscription.unsubscribe();
			}
		}
	}

	private void sendMessage(Object message, LaunchSession launchSession, ResponseListener<Object> listener) {
		if (launchSession == null || launchSession.getAppId() == null) {
			Util.postError(listener, new ServiceCommandError(0, "Must provide a valid LaunchSession object", null));
			return;
		}
		
		if (message == null) {
			Util.postError(listener, new ServiceCommandError(0, "Cannot send a null message", null));
			return;
		}
		
	    if (socket == null) {
	    	connect();
	    }

		String appId = "com.webos.app.webapphost." + launchSession.getAppId();
		JSONObject payload = new JSONObject();
		
		try {
			payload.put("type", "p2p");
			payload.put("to", appId);
			payload.put("payload", message);
			
			Object payTest = payload.get("payload");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
	    ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, null, payload, true, listener);
	    sendCommand(request);
	}
	
	public void sendMessage(String message, LaunchSession launchSession, ResponseListener<Object> listener) {
		if (message != null && message.length() > 0) {
			sendMessage((Object) message, launchSession, listener);
	    } 
		else {
	        Util.postError(listener, new ServiceCommandError(0, "Cannot send a null message", null));
	    }
	}
	
	public void sendMessage(JSONObject message, LaunchSession launchSession, ResponseListener<Object> listener) {
		if (message != null && message.length() > 0)
			sendMessage((Object) message, launchSession, listener);
		else
	        Util.postError(listener, new ServiceCommandError(0, "Cannot send a null message", null));
	}

	
    /**************
    SYSTEM CONTROL
    **************/
	public void getServiceInfo(final ServiceInfoListener listener) {
		String uri = "ssap://api/getServiceList";
	
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				try {
					JSONObject jsonObj = (JSONObject)response;
					JSONArray services = (JSONArray) jsonObj.get("services");
					Util.postSuccess(listener, services);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		});
	
		request.send();	
	}
	
	public void getSystemInfo(final SystemInfoListener listener) {
		String uri = "ssap://system/getSystemInfo";
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				try {
					JSONObject jsonObj = (JSONObject)response;
					JSONObject features = (JSONObject) jsonObj.get("features");
					Util.postSuccess(listener, features);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		});
	
		request.send();	
	}
	
	public void secureAccessTest(final SecureAccessTestListener listener) {
		String uri = "ssap://com.webos.service.secondscreen.gateway/test/secure";

        ResponseListener<Object> responseListener = new ResponseListener<Object>() {
                
            @Override
            public void onSuccess(Object response) {

                try {
                	JSONObject jsonObj = (JSONObject) response;
                    boolean isSecure = (Boolean) jsonObj.get("returnValue");
                    Util.postSuccess(listener, isSecure);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
                
            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, responseListener);
        request.send();
	}

	public void getACRAuthToken(final ACRAuthTokenListener listener) {
		String uri = "ssap://tv/getACRAuthToken";

        ResponseListener<Object> responseListener = new ResponseListener<Object>() {
                
            @Override
            public void onSuccess(Object response) {

                try {
                	JSONObject jsonObj = (JSONObject) response;
                    String authToken = (String) jsonObj.get("token");
                    Util.postSuccess(listener, authToken);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            
            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, responseListener);
        request.send();
	}

	public void getLaunchPoints(final LaunchPointsListener listener) {
		String uri = "ssap://com.webos.applicationManager/listLaunchPoints";

        ResponseListener<Object> responseListener = new ResponseListener<Object>() {
                
            @Override
            public void onSuccess(Object response) {

                try {
                	JSONObject jsonObj = (JSONObject) response;
                    JSONArray launchPoints = (JSONArray) jsonObj.get("launchPoints");
                    Util.postSuccess(listener, launchPoints);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            
            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, true, responseListener);
        request.send();		
	}
	
	@Override
	public void unsubscribe(URLServiceSubscription<?> subscription) {
		int requestId = subscription.getRequestId();
		WebOSTVService service = (WebOSTVService)subscription.getDeviceService();
		
		if (service.requests.get(requestId) != null) {
			JSONObject headers = new JSONObject();
			
			try{
				headers.put("type", "unsubscribe");
				headers.put("id", String.valueOf(requestId));				
			} catch (JSONException e)
			{
				// Safe to ignore
				e.printStackTrace();
			}
			
			service.sendMessage(headers, null);
			service.requests.remove(requestId);
		}
	}

	public void sendMessage(JSONObject packet, JSONObject payload) {
// 		JSONObject packet = new JSONObject();
		
		try {
//			for (Map.Entry<String, String> entry : headers.entrySet()) {
//				packet.put(entry.getKey(), entry.getValue());
//			}
			
			if (payload != null) {
				packet.put("payload", payload);
			}
		} catch (JSONException e) {
			throw new Error(e);
		}
		
		if ( isConnected() ) {
			this.socket.send(packet.toString());
		}
		else {
			System.err.println("connection lost");
			handleConnectionLost(false, null);
		}
	}
	
	private void setCapabilities() {
		if (DiscoveryManager.getInstance().getPairingLevel() == PairingLevel.ON) { 
			appendCapabilites(TextInputControl.Capabilities);
			appendCapabilites(MouseControl.Capabilities);
			appendCapabilites(KeyControl.Capabilities);
			appendCapabilites(PowerControl.Capabilities);
			appendCapabilites(MediaPlayer.Capabilities);
			appendCapabilites(Launcher.Capabilities);
			appendCapabilites(TVControl.Capabilities);
			appendCapabilites(ExternalInputControl.Capabilities);
			appendCapabilites(VolumeControl.Capabilities);
			appendCapabilites(MediaControl.Capabilities);
			appendCapabilites(ToastControl.Capabilities);
		} else {
			appendCapabilites(VolumeControl.Capabilities);
			appendCapabilites(MediaControl.Capabilities);
			appendCapabilites(MediaPlayer.Capabilities);
			appendCapabilites(
					Application, 
					Application_Params, 
					Application_Close, 
					Browser, 
					Browser_Params, 
					Hulu, 
					Netflix, 
					Netflix_Params, 
					YouTube, 
					YouTube_Params, 
					AppStore, 
					AppStore_Params, 
					AppState, 
					AppState_Subscribe
			);
		}
		
		if (!serviceDescription.getVersion().contains("4.0.0")) {
			appendCapabilites(WebAppLauncher.Capabilities);
		} else {
			appendCapabilites(
					Launch, 
					Launch_Params, 
					WebAppLauncher.Close
			);
		}
	}

	public List<String> getPermissions() {
		if (permissions != null)
			return permissions;
		
		List<String> defaultPermissions = new ArrayList<String>();
		for (String perm: kWebOSTVServiceOpenPermissions) {
			defaultPermissions.add(perm);
		}
		
		if (DiscoveryManager.getInstance().getPairingLevel() == PairingLevel.ON) {
			for (String perm: kWebOSTVServiceProtectedPermissions) {
				defaultPermissions.add(perm);
			}
			
			for (String perm: kWebOSTVServicePersonalActivityPermissions) {
				defaultPermissions.add(perm);
			}
	    }

	    permissions = defaultPermissions;
	    
	    return permissions;
	}
	
	public void setPermissions(List<String> permissions) {
		this.permissions = permissions;

		WebOSTVServiceConfig config = (WebOSTVServiceConfig) serviceConfig;
		
		if (config.getClientKey() != null) {
			config.setClientKey(null);
			
			if (isConnected()) {
				Log.w("Connect SDK", "Permissions changed -- you will need to re-pair to the TV.");
				disconnect();
			}
		}
	}
	
	private ChannelInfo parseRawChannelData(JSONObject channelRawData) {
		String channelName = null;
		String channelId = null;
		String channelNumber = null;
		int minorNumber;
		int majorNumber;
		
		ChannelInfo channelInfo = new ChannelInfo();
		channelInfo.setRawData(channelRawData);
		
		try {
			if (!channelRawData.isNull("channelName")) 
				channelName = (String) channelRawData.get("channelName");
			
			if (!channelRawData.isNull("channelId")) 
				channelId = (String) channelRawData.get("channelId");
			
			channelNumber = channelRawData.optString("channelNumber");
			
			if (!channelRawData.isNull("majorNumber"))
				majorNumber = (Integer) channelRawData.get("majorNumber");
			else 
				majorNumber = parseMajorNumber(channelNumber);
			
			if (!channelRawData.isNull("minorNumber"))
				minorNumber = (Integer) channelRawData.get("minorNumber");
			else
				minorNumber = parseMinorNumber(channelNumber);
			
			channelInfo.setName(channelName);
			channelInfo.setId(channelId);
			channelInfo.setNumber(channelNumber);
			channelInfo.setMajorNumber(majorNumber);
			channelInfo.setMinorNumber(minorNumber);
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return channelInfo;
	}
	
	private int parseMinorNumber(String channelNumber) {
		if (channelNumber != null) {
			String tokens[] = channelNumber.split("-");
			return Integer.valueOf(tokens[tokens.length-1]);
		}
		else 
			return 0;
	}
		
	private int parseMajorNumber(String channelNumber) {
		if (channelNumber != null) {
			String tokens[] = channelNumber.split("-");
			return Integer.valueOf(tokens[0]);
		}
		else 
			return 0;
	}
	
	private List<ExternalInputInfo> externalnputInfoFromJSONArray(JSONArray inputList) {
		List<ExternalInputInfo> externalInputInfoList = new ArrayList<ExternalInputInfo>();
		
		for (int i = 0; i < inputList.length(); i++) {
			try {
				JSONObject input = (JSONObject) inputList.get(i);
				
				String id = input.getString("id");
				String name = input.getString("label");
				boolean connected = input.getBoolean("connected");
				String iconURL = input.getString("icon");
				
				ExternalInputInfo inputInfo = new ExternalInputInfo();
				inputInfo.setRawData(input);
				inputInfo.setId(id);
				inputInfo.setName(name);
				inputInfo.setConnected(connected);
				inputInfo.setIconURL(iconURL);
				
				externalInputInfoList.add(inputInfo);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		return externalInputInfoList;
	}

//	@Override
//	public LaunchSession decodeLaunchSession(String type, JSONObject obj) throws JSONException {
//		if ("webostv".equals(type)) {
//			LaunchSession launchSession = LaunchSession.launchSessionFromJSONObject(obj);
//			launchSession.setService(this);
//			return launchSession;
//		}
//		return null;
//	}

	@Override
	public void getPlayState(PlayStateListener listener) {
		Util.postError(listener, ServiceCommandError.notSupported());
	}

	@Override
	public ServiceSubscription<PlayStateListener> subscribePlayState(PlayStateListener listener) {
		Util.postError(listener, ServiceCommandError.notSupported());

		return null;
	}
	
	protected void handleConnected() {
		state = State.REGISTERING;
	    sendRegister();
	}
	
	protected void handleConnectError(Exception ex) {
		System.err.println("connect error: " + ex.toString());
		
		Util.runOnUI(new Runnable() {
			
			@Override
			public void run() {
				for ( ConnectableDeviceListenerPair pair: deviceListeners)
					pair.listener.onConnectionFailed(pair.device, new ServiceCommandError(0, "connection error", null));
			}
		});
    }
	
	protected void sendRegister() {
		JSONObject headers = new JSONObject();
		JSONObject payload = new JSONObject();

		try {
			headers.put("type", "register");
			
			if ( !(serviceConfig instanceof WebOSTVServiceConfig) ) {
				serviceConfig = new WebOSTVServiceConfig(serviceConfig.getServiceUUID());
			}
			
			if (((WebOSTVServiceConfig)serviceConfig).getClientKey() != null) {
				payload.put("client-key", ((WebOSTVServiceConfig)serviceConfig).getClientKey());
			}
			else {
				if ( DiscoveryManager.getInstance().getPairingLevel() == PairingLevel.ON ) {
					Util.runOnUI(new Runnable() {
						
						@Override
						public void run() {
							for ( ConnectableDeviceListenerPair pair: deviceListeners ) {
								pair.listener.onPairingRequired(pair.device, WebOSTVService.this, pairingType);
							}
						}
					});
				}
			}
			
			if (manifest != null) {
				payload.put("manifest", manifest);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		sendMessage(headers, payload);
	}
	
	protected void handleRegistered() {
		state = State.REGISTERED;

		if (!commandQueue.isEmpty()) {
			LinkedHashSet<ServiceCommand<ResponseListener<Object>>> tempHashSet = new LinkedHashSet<ServiceCommand<ResponseListener<Object>>>(commandQueue);
			for (ServiceCommand<ResponseListener<Object>> command : tempHashSet) {
				Log.d("Connect SDK", "executing queued command for " + command.getTarget());
				
				sendCommandImmediately(command);
				commandQueue.remove(command);
			}
		}
		
		Util.runOnUI(new Runnable() {
			
			@Override
			public void run() {
				if (listener != null)
					listener.onConnectionSuccess(WebOSTVService.this);
			}
		});
		
//		ConnectableDevice storedDevice = connectableDeviceStore.getDevice(serviceConfig.getServiceUUID());
//		if (storedDevice == null) {
//			storedDevice = new ConnectableDevice(
//					serviceDescription.getIpAddress(), 
//					serviceDescription.getFriendlyName(), 
//					serviceDescription.getModelName(), 
//					serviceDescription.getModelNumber());
//		}
//		storedDevice.addService(WebOSTVService.this);
//		connectableDeviceStore.addDevice(storedDevice);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void sendCommand(ServiceCommand<?> command) {
		Integer requestId;
		if (command.getRequestId() == -1) {
			requestId = this.nextRequestId++;
			command.setRequestId(requestId);
		}
		else {
			requestId = command.getRequestId();
		}
		requests.put(requestId, command);
		
		if (state == State.REGISTERED) {
			this.sendCommandImmediately(command);
		} else if (state == State.CONNECTING || state == State.DISCONNECTING){
			Log.d("Connect SDK", "queuing command for " + command.getTarget());
			commandQueue.add((ServiceCommand<ResponseListener<Object>>) command);
		} else {
			Log.d("Connect SDK", "queuing command and restarting socket for " + command.getTarget());
			commandQueue.add((ServiceCommand<ResponseListener<Object>>) command);
			connect();
		}
	}
	
	protected void sendCommandImmediately(ServiceCommand<?> command) {
		JSONObject headers = new JSONObject();
		JSONObject payload = (JSONObject) command.getPayload();
		String payloadType = "";
		
		try
		{
			payloadType = payload.getString("type");
		} catch (Exception ex)
		{
			// ignore
		}
		
		if (payloadType == "p2p")
		{
			Iterator<?> iterator = payload.keys();
			
			while (iterator.hasNext())
			{
				String key = (String) iterator.next();
				
				try
				{
					headers.put(key, payload.get(key));
				} catch (JSONException ex)
				{
					// ignore
				}
			}
			
			this.sendMessage(headers, null);
		} else
		{
			try
			{
				headers.put("type", command.getHttpMethod());
				headers.put("id", String.valueOf(command.getRequestId()));
				headers.put("uri", command.getTarget());
			} catch (JSONException ex)
			{
				// TODO: handle this
			}
			
			
			this.sendMessage(headers, payload);
		}
	}
	
	private void setSSLContext(SSLContext sslContext) {
		socket.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sslContext));
	}
	
	protected void setupSSL() {
		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			customTrustManager = new TrustManager();
			sslContext.init(null, new TrustManager [] {customTrustManager}, null);
			setSSLContext(sslContext);
			
			if ( !(serviceConfig instanceof WebOSTVServiceConfig) ) {
				serviceConfig = new WebOSTVServiceConfig(serviceConfig.getServiceUUID());
			}
			customTrustManager.setExpectedCertificate(((WebOSTVServiceConfig)serviceConfig).getServerCertificate());
		} catch (KeyException e) {
		} catch (NoSuchAlgorithmException e) {
		}
	}
	
	@Override
	public boolean isConnected() {
		return socket != null && socket.getReadyState() == WebSocket.READYSTATE.OPEN;
	}
	
	@Override
	public boolean isConnectable() {
		return true;
	}

	@SuppressWarnings("unchecked")
	private void handleConnectionLost(boolean cleanDisconnect, Exception ex) {
		Util.runOnUI(new Runnable() {
			
			@Override
			public void run() {
				for (ConnectableDeviceListenerPair pair: deviceListeners) 
					pair.listener.onConnectionFailed(pair.device, new ServiceCommandError(0, "connection error", null));
			}
		});

		for (int i = 0; i < requests.size(); i++) {
			ServiceCommand<ResponseListener<Object>> request = (ServiceCommand<ResponseListener<Object>>) requests.get(requests.keyAt(i));
				
			Util.postError(request.getResponseListener(), new ServiceCommandError(0, "connection lost", null));
		}
		
		requests.clear();
	}

	public class WebOSWebSocketClient extends WebSocketClient {
		WebOSTVService owner;
		boolean connectSucceeded = false;
		
		public WebOSWebSocketClient(WebOSTVService owner, URI uri) {
			super(uri);
			this.owner = owner;
		}
		
		@Override
		public void onOpen(ServerHandshake handshakedata) {
		    connectSucceeded = true;
		    owner.handleConnected();
		}
	
		@Override
		public void onMessage(String data) {
			owner.handleMessage(data);
		}
	
		@Override
		public void onClose(int code, String reason, boolean remote) {
			System.out.println("onClose: " + code + ": " + reason);
			owner.handleConnectionLost(true, null);
		}
	
		@Override
		public void onError(Exception ex) {
			System.err.println("onError: " + ex);
			
		    if (!connectSucceeded) {
		        owner.handleConnectError(ex);
		    } else {
		    	owner.handleConnectionLost(false, ex);
		    }
		}
	}
	
	public void setServerCertificate(X509Certificate cert) {
		if ( !(serviceConfig instanceof WebOSTVServiceConfig) ) {
			serviceConfig = new WebOSTVServiceConfig(serviceConfig.getServiceUUID());
		}
		
		((WebOSTVServiceConfig)serviceConfig).setServerCertificate(cert);
	}
	
	public void setServerCertificate(String cert) {
		if ( !(serviceConfig instanceof WebOSTVServiceConfig) ) {
			serviceConfig = new WebOSTVServiceConfig(serviceConfig.getServiceUUID());
		}
		
		((WebOSTVServiceConfig)serviceConfig).setServerCertificate(loadCertificateFromPEM(cert));
	}
	
	public X509Certificate getServerCertificate() {
		if ( !(serviceConfig instanceof WebOSTVServiceConfig) ) {
			serviceConfig = new WebOSTVServiceConfig(serviceConfig.getServiceUUID());
		}
		
		return ((WebOSTVServiceConfig)serviceConfig).getServerCertificate();
	}
	
	public String getServerCertificateInString() {
		if ( !(serviceConfig instanceof WebOSTVServiceConfig) ) {
			serviceConfig = new WebOSTVServiceConfig(serviceConfig.getServiceUUID());
		}
		
		return exportCertificateToPEM(((WebOSTVServiceConfig)serviceConfig).getServerCertificate());
	}

	private String exportCertificateToPEM(X509Certificate cert) {
		try {
			return Base64.encodeToString(cert.getEncoded(), Base64.DEFAULT);
		} catch (CertificateEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private X509Certificate loadCertificateFromPEM(String pemString) {
		CertificateFactory certFactory;
		try {
			certFactory = CertificateFactory.getInstance("X.509");
			ByteArrayInputStream inputStream = new ByteArrayInputStream(pemString.getBytes("US-ASCII"));
			
			return (X509Certificate)certFactory.generateCertificate(inputStream);
		} catch (CertificateException e) {
			e.printStackTrace();
			return null;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static boolean isInteger(String s) {
	    try { 
	        Integer.parseInt(s); 
	    } catch(NumberFormatException e) { 
	        return false; 
	    }
	    // only got here if we didn't return false
	    return true;
	}
	
	class TrustManager implements X509TrustManager {
		X509Certificate expectedCert;
		X509Certificate lastCheckedCert;

		public void setExpectedCertificate(X509Certificate cert) {
			this.expectedCert = cert;
		}

		public X509Certificate getLastCheckedCertificate () {
			return lastCheckedCert;
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			Log.d("Connect SDK", "Expecting device cert " + (expectedCert != null ? expectedCert.getSubjectDN() : "(any)"));
			
			if (chain != null && chain.length > 0) {
				X509Certificate cert = chain[0];

				lastCheckedCert = cert;
				
				if (expectedCert != null) {
					byte [] certBytes = cert.getEncoded();
					byte [] expectedCertBytes = expectedCert.getEncoded();
					
					Log.d("Connect SDK", "Device presented cert " + cert.getSubjectDN());
					
					if (!Arrays.equals(certBytes, expectedCertBytes)) {
						throw new CertificateException("certificate does not match");
					}
				}
			} else {
				lastCheckedCert = null;
				throw new CertificateException("no server certificate");
			}
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}
	}
	
	@Override public void sendPairingKey(String pairingKey) { }

    public static interface ServiceInfoListener extends ResponseListener<JSONArray> { }

    public static interface SystemInfoListener extends ResponseListener<JSONObject> { }
}
