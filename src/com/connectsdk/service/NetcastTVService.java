/*
* NetcastTVService
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics. All rights reserved.
 * Created by Hyun Kook Khang on 19 Jan 2014
 * 
 */

package com.connectsdk.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import android.content.Context;
import android.graphics.PointF;
import android.util.Log;

import com.connectsdk.core.AppInfo;
import com.connectsdk.core.ChannelInfo;
import com.connectsdk.core.ExternalInputInfo;
import com.connectsdk.core.Util;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.device.ConnectableDeviceStore;
import com.connectsdk.device.netcast.NetcastAppNumberParser;
import com.connectsdk.device.netcast.NetcastApplicationsParser;
import com.connectsdk.device.netcast.NetcastChannelParser;
import com.connectsdk.device.netcast.NetcastHttpServer;
import com.connectsdk.device.netcast.NetcastVolumeParser;
import com.connectsdk.device.netcast.VirtualKeycodes;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryManager.PairingLevel;
import com.connectsdk.etc.helper.HttpMessage;
import com.connectsdk.service.capability.ExternalInputControl;
import com.connectsdk.service.capability.KeyControl;
import com.connectsdk.service.capability.Launcher;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.MouseControl;
import com.connectsdk.service.capability.PowerControl;
import com.connectsdk.service.capability.TVControl;
import com.connectsdk.service.capability.TextInputControl;
import com.connectsdk.service.capability.VolumeControl;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.NotSupportedServiceSubscription;
import com.connectsdk.service.command.ServiceCommand;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.command.URLServiceSubscription;
import com.connectsdk.service.config.NetcastTVServiceConfig;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;
import com.connectsdk.service.sessions.LaunchSession;
import com.connectsdk.service.sessions.LaunchSession.LaunchSessionType;

public class NetcastTVService extends DeviceService implements Launcher, MediaControl, MediaPlayer, TVControl, VolumeControl, ExternalInputControl, MouseControl, TextInputControl, PowerControl, KeyControl {
	public static final String UDAP_PATH_PAIRING = "/udap/api/pairing";
	public static final String UDAP_PATH_DATA = "/udap/api/data";
	public static final String UDAP_PATH_COMMAND = "/udap/api/command";
	public static final String UDAP_PATH_EVENT = "/udap/api/event";

	public static final String UDAP_PATH_APPTOAPP_DATA = "/udap/api/apptoapp/data/";
	public static final String UDAP_PATH_APPTOAPP_COMMAND = "/udap/api/apptoapp/command/";
	public static final String UDAP_PATH_APP_STORE = "/roap/api/command/";

	public static final String UDAP_API_PAIRING = "pairing";
	public static final String UDAP_API_COMMAND = "command";
	public static final String UDAP_API_EVENT = "event";

	public final static String TARGET_CHANNEL_LIST = "channel_list";
	public final static String TARGET_CURRENT_CHANNEL = "cur_channel";
	public final static String TARGET_VOLUME_INFO = "volume_info";
	public final static String TARGET_APPLIST_GET = "applist_get";
	public final static String TARGET_APPNUM_GET = "appnum_get";
	public final static String TARGET_3D_MODE = "3DMode";
	public final static String TARGET_IS_3D = "is_3D";
	
	enum State {
    	NONE,
    	INITIAL,
    	CONNECTING,
    	PAIRING,
    	PAIRED,
    	DISCONNECTING
    };
	
	HttpClient httpClient;
	NetcastHttpServer httpServer;
	ServerSocket netcastHttpServerSocket;
	
	DLNAService dlnaService;
	
	LaunchSession inputPickerSession;
	
	List<AppInfo> applications;
	List<URLServiceSubscription<?>> subscriptions;
	StringBuilder keyboardString;
	
	State state = State.INITIAL;
	Context context;
    
	public NetcastTVService(ServiceDescription serviceDescription, ServiceConfig serviceConfig, ConnectableDeviceStore connectableDeviceStore) {
		super(serviceDescription, serviceConfig, connectableDeviceStore);

		setCapabilities();
		
		applications = new ArrayList<AppInfo>();
		subscriptions = new ArrayList<URLServiceSubscription<?>>();

		keyboardString = new StringBuilder();
		
		httpClient = new DefaultHttpClient();
		ClientConnectionManager mgr = httpClient.getConnectionManager();
		HttpParams params = httpClient.getParams();
		httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(params, mgr.getSchemeRegistry()), params);
		
		state = State.INITIAL;
		
		inputPickerSession = null;
		
		dlnaService = new DLNAService(serviceDescription, serviceConfig, connectableDeviceStore);
		
		if ( DiscoveryManager.getInstance().getPairingLevel() == PairingLevel.ON ) {
			isServiceReady = false;
		}
	}
	
	public static JSONObject discoveryParameters() {
		JSONObject params = new JSONObject();
		
		try {
			params.put("serviceId", "Netcast TV");
//			params.put("filter", "udap:rootservice");
			params.put("filter", "urn:schemas-upnp-org:device:MediaRenderer:1");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return params;
	}
	
	@Override
	public void setServiceDescription(ServiceDescription serviceDescription) {
		super.setServiceDescription(serviceDescription);
	}
	
	@Override
	public void connect() {
		if (state != State.INITIAL) {
			Log.w("Connect SDK", "already connecting; not trying to connect again: " + state);
			return; // don't try to connect again while connected
		}
		
		if ( !(serviceConfig instanceof NetcastTVServiceConfig) ) {
			serviceConfig = new NetcastTVServiceConfig(serviceConfig.getServiceUUID());
		}
		
		if ( DiscoveryManager.getInstance().getPairingLevel() == PairingLevel.ON ) {
			if ( ((NetcastTVServiceConfig) serviceConfig).getPairingKey() != null 
					&& ((NetcastTVServiceConfig)serviceConfig).getPairingKey().length() != 0) {
				
				sendPairingKey(((NetcastTVServiceConfig) serviceConfig).getPairingKey());
			}
			else {
				showPairingKeyOnTV();
			}
			
			Util.runInBackground(new Runnable() {
				
				@Override
				public void run() {
					try {
						netcastHttpServerSocket = new ServerSocket();
						netcastHttpServerSocket.setReuseAddress(true);
						netcastHttpServerSocket.bind(new InetSocketAddress(serviceDescription.getPort()));
						
						Socket inSocket = netcastHttpServerSocket.accept();
						while (true) // listen until user halts execution
						{
							httpServer = new NetcastHttpServer(NetcastTVService.this, inSocket, serviceDescription.getIpAddress(), DiscoveryManager.getInstance().getContext()); // instantiate HttpServer
							httpServer.setSubscriptions(subscriptions);
							// create new thread
							Util.runInBackground(httpServer);
						}
					} catch (IOException e) {
						System.err.println("Server error: " + e);
					} finally {
						if ( netcastHttpServerSocket != null ) {
							try {
								netcastHttpServerSocket.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
			});
		}
	}
	
	@Override
	public void disconnect() {
		if ( DiscoveryManager.getInstance().getPairingLevel() == PairingLevel.ON ) {
			isServiceReady = false;
		}
		
		endPairing(null);
		
		Util.runOnUI(new Runnable() {
			
			@Override
			public void run() {
				for (ConnectableDeviceListenerPair pair: deviceListeners)
					pair.listener.onDeviceDisconnected(pair.device);
				
				deviceListeners.clear();
			}
		});
		
		if ( netcastHttpServerSocket != null ) {
			try {
				netcastHttpServerSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		state = State.INITIAL;
	}
	
	@Override
	public boolean isConnected() {
		if ( state == State.PAIRED || DiscoveryManager.getInstance().getPairingLevel() == PairingLevel.OFF)
			return true;
		else 
			return false;
	}
	
	@Override
	public boolean isConnectable() {
		return true;
	}
	
	public void hostByeBye () {
		disconnect();
	}
	
	//============= Auth ==============================
	public void showPairingKeyOnTV() {
		state = State.CONNECTING;
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				for (ConnectableDeviceListenerPair pair: deviceListeners) {
            		pair.listener.onPairingRequired(pair.device, NetcastTVService.this, PairingType.PIN_CODE);
            	}				
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				state = State.INITIAL;

            	for (ConnectableDeviceListenerPair pair: deviceListeners) {
            		pair.listener.onConnectionFailed(pair.device, error);
            	}
			}
		};
		
		String requestURL = getUDAPRequestURL(UDAP_PATH_PAIRING);
		
		Map <String,String> params = new HashMap<String,String>();
		params.put("name", "showKey");
		
		String httpMessage = getUDAPMessageBody(UDAP_API_PAIRING, params);
		
		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(this, requestURL, httpMessage, responseListener);
		command.send();
	}
	
	// TODO add this when user cancel pairing
	public void removePairingKeyOnTV() {
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
			}
			
			@Override
			public void onError(ServiceCommandError error) {
			}
		};
		
		String requestURL = getUDAPRequestURL(UDAP_PATH_PAIRING);
		
		Map <String,String> params = new HashMap<String,String>();
		params.put("name", "CancelAuthKeyReq");
		
		String httpMessage = getUDAPMessageBody(UDAP_API_PAIRING, params);
		
		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(this, requestURL, httpMessage, responseListener);
		command.send();
	}
	
	@Override
	public void sendPairingKey(final String pairingKey) {
		state = State.PAIRING;

		if ( !(serviceConfig instanceof NetcastTVServiceConfig) ) {
			serviceConfig = new NetcastTVServiceConfig(serviceConfig.getServiceUUID());
		}
		
		((NetcastTVServiceConfig)serviceConfig).setPairingKey(pairingKey);
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				state = State.PAIRED;
				
				List<ConnectableDevice> storedDevices = connectableDeviceStore.getStoredDevices();
        		boolean isNewDevice = true;
        		
        		for ( int i = 0; i < storedDevices.size(); i++ ) {
        			ConnectableDevice storedDevice = storedDevices.get(i);
        			
        			for (DeviceService service: storedDevice.getServices()) {
        				ServiceConfig sc = service.getServiceConfig();
        				
        				if ( sc.getServiceUUID().equals(serviceConfig.getServiceUUID()) ) {
        					service.setServiceConfig(serviceConfig);
        					storedDevice.addService(NetcastTVService.this);

        					connectableDeviceStore.updateDevice(storedDevice);
        					isNewDevice = false;

        					break;
        				}
        			}
        			
        			if ( isNewDevice == false )
        				break;
        		}
        		
        		if ( isNewDevice == true ) {
        			ConnectableDevice newDevice = new ConnectableDevice(
        					serviceDescription.getIpAddress(), 
        					serviceDescription.getFriendlyName(), 
        					serviceDescription.getModelName(), 
        					serviceDescription.getModelNumber());
        			
        			newDevice.addService(NetcastTVService.this);
        			connectableDeviceStore.addDevice(newDevice);
        		}
        		
        		if ( serviceReadyListener != null ) {
        			isServiceReady = true;
        			serviceReadyListener.onServiceReady();
        		}
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				state = State.INITIAL;

				for (ConnectableDeviceListenerPair pair: deviceListeners) {
            		pair.listener.onConnectionFailed(pair.device, error);
            	}
			}
		};
		
		String requestURL = getUDAPRequestURL(UDAP_PATH_PAIRING);
		
		Map <String,String> params = new HashMap<String,String>();
		params.put("name", "hello");
		params.put("value", pairingKey);
		params.put("port", String.valueOf(serviceDescription.getPort()));
		
		String httpMessage = getUDAPMessageBody(UDAP_API_PAIRING, params);
		
		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(this, requestURL, httpMessage, responseListener);
		command.send();
	}
	
	private void endPairing(ResponseListener<Object> listener) {
		String requestURL = getUDAPRequestURL(UDAP_PATH_PAIRING);
		
		Map <String,String> params = new HashMap<String,String>();
		params.put("name", "byebye");
		params.put("port", String.valueOf(serviceDescription.getPort()));
		
		String httpMessage = getUDAPMessageBody(UDAP_API_PAIRING, params);
		
		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(this, requestURL, httpMessage, listener);
		command.send();
	}
	
	
	/******************
    LAUNCHER
    *****************/
	public Launcher getLauncher() {
		return this;
	};
	
	@Override
	public CapabilityPriorityLevel getLauncherCapabilityLevel() {
		return CapabilityPriorityLevel.NORMAL;
	}
	
	class NetcastTVLaunchSessionR extends LaunchSession {
		String appName;
		NetcastTVService service;
		
		NetcastTVLaunchSessionR (NetcastTVService service, String auid, String appName) {
			this.service = service;
			appId = auid;
		}
		
		NetcastTVLaunchSessionR (NetcastTVService service, JSONObject obj) throws JSONException {
			this.service = service;
			fromJSONObject(obj);
		}
		
		public void close(ResponseListener<Object> responseListener) {
		}
		
		@Override
		public JSONObject toJSONObject() throws JSONException {
			JSONObject obj = super.toJSONObject();
			obj.put("type", "netcasttv");
			obj.put("appName", appName);
			return obj;
		}
		
		@Override
		public void fromJSONObject(JSONObject obj) throws JSONException {
			super.fromJSONObject(obj);
			appName = obj.optString("appName");
		}
	}
	
	public void getApplication(final String appName, final AppInfoListener listener) {
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				final String strObj = (String) response;
				
	            AppInfo appId = new AppInfo() {{
	            	setId(decToHex(strObj));
	            }};

	            if ( appId != null ) {
		            Util.postSuccess(listener, appId);
	            }
			}
			
			@Override
			public void onError(ServiceCommandError error) {
	            if ( listener != null ) 
	            	Util.postError(listener, error);
			}
		};
		
		String uri = UDAP_PATH_APPTOAPP_DATA + appName;
		String requestURL = getUDAPRequestURL(uri);
		
		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(this, requestURL, null, responseListener);
		command.setHttpMethod(ServiceCommand.TYPE_GET);
		command.send();
	}
	
	@Override
	public void launchApp(final String appId, final AppLaunchListener listener) {
		getAppInfoForId(appId, new AppInfoListener() {
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
			
			@Override
			public void onSuccess(AppInfo appInfo) {
				launchAppWithInfo(appInfo, listener);
			}
		});
	}
	
	private void getAppInfoForId(final String appId, final AppInfoListener listener) {
		getAppList(new AppListListener() {
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
			
			@Override
			public void onSuccess(List<AppInfo> object) {
				for (AppInfo info : object) {
					if (info.getName().equalsIgnoreCase(appId)) {
						Util.postSuccess(listener, info);
						return;
					}
				}
				Util.postError(listener, new ServiceCommandError(0, "Unable to find the App with id", null));
			}
		});
	}

	private void launchApplication(final String appName, final String auid, final String contentId, final Launcher.AppLaunchListener listener) {
		JSONObject jsonObj = new JSONObject();

		try {
			jsonObj.put("id", auid);
			jsonObj.put("title", appName);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				LaunchSession launchSession = LaunchSession.launchSessionForAppId(auid);
				launchSession.setAppName(appName);
				launchSession.setService(NetcastTVService.this);
				launchSession.setSessionType(LaunchSessionType.App);

				Util.postSuccess(listener, launchSession);
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		};
		
		String requestURL = getUDAPRequestURL(UDAP_PATH_APPTOAPP_COMMAND);
		
		Map <String,String> params = new HashMap<String,String>();
		params.put("name", "AppExecute");
		params.put("auid", auid);
		if ( appName != null ) {
			params.put("appname", appName);
		}
		if ( contentId != null ) {
			params.put("contentid", contentId);
		}
		
		String httpMessage = getUDAPMessageBody(UDAP_API_COMMAND, params);
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, requestURL, httpMessage, responseListener);
		request.send();
	}
	
	@Override
	public void launchAppWithInfo(AppInfo appInfo, Launcher.AppLaunchListener listener) {
		launchAppWithInfo(appInfo, null, listener);
	}

	@Override
	public void launchAppWithInfo(AppInfo appInfo, Object params, Launcher.AppLaunchListener listener) {
		String appName = HttpMessage.percentEncoding(appInfo.getName());
		String appId = appInfo.getId();
		String contentId = null;
		JSONObject mParams = null;
		if (params instanceof JSONObject)
			mParams = (JSONObject) params;
		
		if (mParams != null) {
			try {
				contentId = (String) mParams.get("contentId");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		launchApplication(appName, appId, contentId, listener);
	}

	@Override
	public void launchBrowser(String url, final Launcher.AppLaunchListener listener) {
		if ( !(url == null || url.length() == 0) ) 
			Log.w("Connect SDK", "Netcast TV does not support deeplink for Browser");

		final String appName = "Internet";

		getApplication(appName, new AppInfoListener() {
			
			@Override
			public void onSuccess(AppInfo appInfo) {
				String contentId = null;
				launchApplication(appName, appInfo.getId(), contentId, listener);
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		});
	}

	@Override
	public void launchYouTube(final String contentId, final Launcher.AppLaunchListener listener) {
		final String appName = "YouTube";

		getApplication(appName, new AppInfoListener() {
			
			@Override
			public void onSuccess(AppInfo appInfo) {
				launchApplication(appName, appInfo.getId(), contentId, listener);
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		});
	}

	@Override
	public void launchHulu(final String contentId, final Launcher.AppLaunchListener listener) {
		final String appName = "Hulu";

		getApplication(appName, new AppInfoListener() {
			
			@Override
			public void onSuccess(AppInfo appInfo) {
				launchApplication(appName, appInfo.getId(), contentId, listener);
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		});		
	}

	@Override
	public void launchNetflix(final String contentId, final Launcher.AppLaunchListener listener) {
		final String appName = "Netflix";

		getApplication(appName, new AppInfoListener() {
			
			@Override
			public void onSuccess(final AppInfo appInfo) {
				JSONObject jsonObj = new JSONObject();

				try {
					jsonObj.put("id", appInfo.getId());
					jsonObj.put("name", appName);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
				ResponseListener<Object> responseListener = new ResponseListener<Object>() {
					
					@Override
					public void onSuccess(Object response) {
						LaunchSession launchSession = LaunchSession.launchSessionForAppId(appInfo.getId());
						launchSession.setAppName(appName);
						launchSession.setService(NetcastTVService.this);
						launchSession.setSessionType(LaunchSessionType.App);

						Util.postSuccess(listener, launchSession);
					}
					
					@Override
					public void onError(ServiceCommandError error) {
						if ( listener != null ) 
							Util.postError(listener, error);
					}
				};
				
				String requestURL = getUDAPRequestURL(UDAP_PATH_APPTOAPP_COMMAND);
				
				Map <String,String> params = new HashMap<String,String>();
				params.put("name", "SearchCMDPlaySDPContent");
				params.put("content_type", "1");
				params.put("conts_exec_type", "20");
				params.put("conts_plex_type_flag", "N");
				params.put("conts_search_id", "2023237");
				params.put("conts_age", "18");
				params.put("exec_id", "netflix");
				params.put("item_id", "-Q m=http%3A%2F%2Fapi.netflix.com%2Fcatalog%2Ftitles%2Fmovies%2F" + contentId + "&amp;source_type=4&amp;trackId=6054700&amp;trackUrl=https%3A%2F%2Fapi.netflix.com%2FAPI_APP_ID_6261%3F%23Search%3F");
				params.put("app_type", "");
				
				String httpMessage = getUDAPMessageBody(UDAP_API_COMMAND, params);
				
				ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(NetcastTVService.this, requestURL, httpMessage, responseListener);
				request.send();
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				if ( listener != null ) 
					Util.postError(listener, error);
			}
		});		
	}
	
	@Override
	public void launchAppStore(final String appId, final AppLaunchListener listener) {
		String targetPath = getUDAPRequestURL(UDAP_PATH_APP_STORE);
		
		Map<String, String> params = new HashMap<String, String>() {{
			put("content_type", "");
			put("conts_plex_type_flag", "");
			put("conts_search_id", "");
			put("conts_age", "");
			put("exec_id", "");
			put("item_id", HttpMessage.percentEncoding(appId));
			put("app_type", "S");
		}};
		
		String httpMessage = getUDAPMessageBody(UDAP_API_COMMAND, params);

		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				LaunchSession launchSession = LaunchSession.launchSessionForAppId(appId);
				launchSession.setAppName("LG Smart World"); // TODO: this will not work in Korea, use "LG 스마트 월드" instead
				launchSession.setService(NetcastTVService.this);
				launchSession.setSessionType(LaunchSessionType.App);

				Util.postSuccess(listener, launchSession);
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		};	
		new ServiceCommand<ResponseListener<Object>>(this, targetPath, httpMessage, responseListener);
	}
	
	@Override
	public void closeApp(LaunchSession launchSession, ResponseListener<Object> listener) {
		String requestURL = getUDAPRequestURL(UDAP_PATH_APPTOAPP_COMMAND);
		
		Map <String,String> params = new HashMap<String,String>();
		params.put("name", "AppTerminate");
		params.put("auid", launchSession.getAppId());
		if (launchSession.getAppName() != null) 
			params.put("appname", HttpMessage.percentEncoding(launchSession.getAppName()));

		String httpMessage = getUDAPMessageBody(UDAP_API_COMMAND, params);
		
		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(launchSession.getService(), requestURL, httpMessage, listener);
		command.send();
	}
	
	private void getTotalNumberOfApplications(final int type, final AppCountListener listener) {
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				String strObj = (String) response;
				
				int applicationNumber = parseAppNumberXmlToJSON(strObj);
				
				Util.postSuccess(listener, applicationNumber);
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		};
		
		String requestURL = getUDAPRequestURL(UDAP_PATH_DATA, TARGET_APPNUM_GET, String.valueOf(type));
		
		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(this, requestURL, null, responseListener);
		command.setHttpMethod(ServiceCommand.TYPE_GET);
		command.send();
	}
	
	private void getApplications(final int type, final int number, final AppListListener listener) {
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				String strObj = (String) response;
				
	            JSONArray applicationArray = parseApplicationsXmlToJSON(strObj);
	            List<AppInfo> appList = new ArrayList<AppInfo>();
	            
	            for (int i = 0; i < applicationArray.length(); i++)
	            {
					try {
						final JSONObject appJSON = applicationArray.getJSONObject(i);
						
						AppInfo appInfo = new AppInfo() {{
							setId(appJSON.getString("id"));
							setName(appJSON.getString("title"));
						}};
						
						appList.add(appInfo);
					} catch (JSONException e) {
						e.printStackTrace();
					}
	            }
	            
            	if ( listener != null ) {
            		Util.postSuccess(listener, appList);
            	}
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				if ( listener != null ) {
//					listener.onGetApplicationsFailed(error)
				}
			}
		};
		
		String requestURL = getUDAPRequestURL(UDAP_PATH_DATA, TARGET_APPLIST_GET, String.valueOf(type), "0", String.valueOf(number));
		
		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(this, requestURL, null, responseListener);
		command.setHttpMethod(ServiceCommand.TYPE_GET);
		command.send();
	}
	
	@Override
	public void getAppList(final AppListListener listener) {
		getTotalNumberOfApplications(2, new AppCountListener() {
			
			@Override
			public void onSuccess(final Integer count) {
				getApplications(2, count, new AppListListener() {
					
					@Override
					public void onSuccess(List<AppInfo> apps) {
						applications = apps;
						
						getTotalNumberOfApplications(3, new AppCountListener() {
							
							@Override
							public void onSuccess(final Integer count) {
								getApplications(3, count, new AppListListener() {
									
									@Override
									public void onSuccess(List<AppInfo> apps) {
										applications = apps;
										
										Util.postSuccess(listener, apps);
									}
									
									@Override
									public void onError(ServiceCommandError error) {
										Util.postError(listener, error);
									}
								});
							}
							
							@Override
							public void onError(ServiceCommandError error) {
								Util.postError(listener, error);
							}
						});
					}
					
					@Override
					public void onError(ServiceCommandError error) {
						Util.postError(listener, error);
					}
				});
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		});
	}
	
	@Override
	public void getRunningApp(AppInfoListener listener) {
		// Do nothing - Not Supported
		Util.postError(listener, ServiceCommandError.notSupported());
	}
	
	@Override
	public ServiceSubscription<AppInfoListener> subscribeRunningApp(AppInfoListener listener) {
		// Do nothing - Not Supported
		Util.postError(listener, ServiceCommandError.notSupported());
		
		return new NotSupportedServiceSubscription<AppInfoListener>();
	}
	
	@Override
	public void getAppState(final LaunchSession launchSession, final AppStateListener listener) {
		String requestURL = String.format(Locale.US, "%s%s", 
				getUDAPRequestURL(UDAP_PATH_APPTOAPP_DATA), 
				String.format(Locale.US, "/%s/status", launchSession.getAppId()));
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
			
			@Override
			public void onSuccess(Object object) {
				String response = (String) object;
				AppState appState;
				if (response.equalsIgnoreCase("NONE"))
					appState = new AppState(false, false);
				else if (response.equalsIgnoreCase("LOAD"))
					appState = new AppState(false, true);
				else if (response.equalsIgnoreCase("RUN_NF"))
					appState = new AppState(true, false);
				else if (response.equalsIgnoreCase("TERM"))
					appState = new AppState(false, true);
				else
					appState = new AppState(false, false);
				
				Util.postSuccess(listener, appState);
			}
		};
		
		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(this, requestURL, null, responseListener);
		command.setHttpMethod(ServiceCommand.TYPE_GET);
		command.send();
	}
	
	@Override
	public ServiceSubscription<AppStateListener> subscribeAppState(LaunchSession launchSession, AppStateListener listener) {
		Util.postError(listener, ServiceCommandError.notSupported());

		return null;
	}
	
	
	/******************
    TV CONTROL
    *****************/
	@Override
	public TVControl getTVControl() {
		return this;
	}
	
	@Override
	public CapabilityPriorityLevel getTVControlCapabilityLevel() {
		return CapabilityPriorityLevel.HIGH;
	}
	
	@Override
	public void getChannelList(final ChannelListListener listener) {
		String requestURL = getUDAPRequestURL(UDAP_PATH_DATA, TARGET_CHANNEL_LIST);

		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				String strObj = (String)response;
				
				try {
					SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
					InputStream stream = new ByteArrayInputStream(strObj.getBytes("UTF-8"));
					SAXParser saxParser = saxParserFactory.newSAXParser();

					NetcastChannelParser parser = new NetcastChannelParser();
					saxParser.parse(stream, parser);
					
					JSONArray channelArray = parser.getJSONChannelArray();
					ArrayList<ChannelInfo> channelList = new ArrayList<ChannelInfo>();
					
					for (int i = 0; i < channelArray.length(); i++) {
						 JSONObject rawData;
						 try {
							 rawData = (JSONObject) channelArray.get(i);
						 
							 ChannelInfo channel = NetcastChannelParser.parseRawChannelData(rawData);
							 channelList.add(channel);
						 } catch (JSONException e) {
							 e.printStackTrace();
						 }
					}
					
					Util.postSuccess(listener, channelList);
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				} catch (SAXException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		};
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, requestURL, null, responseListener);
		request.setHttpMethod(ServiceCommand.TYPE_GET);
		request.send();
	}
	
	@Override
	public void channelUp(ResponseListener<Object> listener) {
		sendKeyCode(VirtualKeycodes.CHANNEL_UP.getCode(), listener);
	}

	@Override
	public void channelDown(ResponseListener<Object> listener) {
		sendKeyCode(VirtualKeycodes.CHANNEL_DOWN.getCode(), listener);
	}

	@Override
	public void setChannel(final ChannelInfo channelInfo, final ResponseListener<Object> listener) {
		getChannelList(new ChannelListListener() {
			
			@Override
			public void onSuccess(List<ChannelInfo> channelList) {
				String requestURL = getUDAPRequestURL(UDAP_PATH_COMMAND);

				Map <String,String> params = new HashMap<String,String>();
				
				for (int i = 0; i < channelList.size(); i++) {
					ChannelInfo ch = channelList.get(i);
					JSONObject rawData = ch.getRawData();
					
					try {
						String major = channelInfo.getNumber().split("-")[0];
						String minor = channelInfo.getNumber().split("-")[1];
						
						int majorNumber = ch.getMajorNumber();
						int minorNumber = ch.getMinorNumber();
						
						String sourceIndex = (String) rawData.get("sourceIndex");
						int physicalNum = (Integer) rawData.get("physicalNumber");
						
						if ( Integer.valueOf(major) == majorNumber 
								&& Integer.valueOf(minor) == minorNumber ) {
							params.put("name", "HandleChannelChange");
							params.put("major", major);
							params.put("minor", minor);
							params.put("sourceIndex", sourceIndex);
							params.put("physicalNum", String.valueOf(physicalNum));

							break;
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				
				String httpMessage = getUDAPMessageBody(UDAP_API_COMMAND, params);
				
				ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(NetcastTVService.this, requestURL, httpMessage, listener);
				request.send();
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		});
	}

	@Override
	public void getCurrentChannel(final ChannelListener listener) {
		String requestURL = getUDAPRequestURL(UDAP_PATH_DATA, TARGET_CURRENT_CHANNEL);

		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				String strObj = (String)response;
				
				try {
					SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
					InputStream stream = new ByteArrayInputStream(strObj.getBytes("UTF-8"));
					SAXParser saxParser = saxParserFactory.newSAXParser();

					NetcastChannelParser parser = new NetcastChannelParser();
					saxParser.parse(stream, parser);
					
					JSONArray channelArray = parser.getJSONChannelArray();
					
					if ( channelArray.length() > 0 ) {
						JSONObject rawData = (JSONObject) channelArray.get(0);

						ChannelInfo channel = NetcastChannelParser.parseRawChannelData(rawData);
						
						Util.postSuccess(listener, channel);
					}
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				} catch (SAXException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		};
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, requestURL, null, responseListener);
		request.send();
	}

	@Override
	public ServiceSubscription<ChannelListener> subscribeCurrentChannel(final ChannelListener listener) {
		getCurrentChannel(listener);	// This is for the initial Current TV Channel Info.
		
		URLServiceSubscription<ChannelListener> request = new URLServiceSubscription<ChannelListener>(this, "ChannelChanged", null, null);
		request.setHttpMethod(ServiceCommand.TYPE_GET);
		request.addListener(listener);
		subscriptions.add(request);

		return request;
	}
	
	@Override
	public void getProgramInfo(ProgramInfoListener listener) {
		// Do nothing - Not Supported
		Util.postError(listener, ServiceCommandError.notSupported());
	}

	@Override
	public ServiceSubscription<ProgramInfoListener> subscribeProgramInfo(ProgramInfoListener listener) {
		// Do nothing - Not Supported
		Util.postError(listener, ServiceCommandError.notSupported());

		return null;
	}
	
	@Override
	public void getProgramList(ProgramListListener listener) {
		// Do nothing - Not Supported
		Util.postError(listener, ServiceCommandError.notSupported());
	}

	@Override
	public ServiceSubscription<ProgramListListener> subscribeProgramList(ProgramListListener listener) {
		// Do nothing - Not Supported
		Util.postError(listener, ServiceCommandError.notSupported());

		return null;
	}

	@Override
	public void set3DEnabled(final boolean enabled, final ResponseListener<Object> listener) {
		get3DEnabled(new State3DModeListener() {
			
			@Override
			public void onSuccess(Boolean isEnabled) {
				if ( enabled != isEnabled ) {
					sendKeyCode(VirtualKeycodes.VIDEO_3D.getCode(), listener);
				}
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		});
	}

	@Override
	public void get3DEnabled(final State3DModeListener listener) {
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				String strObj = (String) response;
				String upperStr = strObj.toUpperCase(Locale.ENGLISH);
				
				Util.postSuccess(listener, upperStr.contains("TRUE"));
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		};
		
		String requestURL = getUDAPRequestURL(UDAP_PATH_DATA, TARGET_IS_3D);

		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, requestURL, null, responseListener);
		request.setHttpMethod(ServiceCommand.TYPE_GET);
		request.send();	
	}

	@Override
	public ServiceSubscription<State3DModeListener> subscribe3DEnabled(final State3DModeListener listener) {
		get3DEnabled(listener);

		URLServiceSubscription<State3DModeListener> request = new URLServiceSubscription<State3DModeListener>(this, TARGET_3D_MODE, null, null);
		request.setHttpMethod(ServiceCommand.TYPE_GET);
		request.addListener(listener);
		subscriptions.add(request);

		return request;
	}

	
	
	/**************
    VOLUME
    **************/
	@Override
	public VolumeControl getVolumeControl() {
		return this;
	};
	
	@Override
	public CapabilityPriorityLevel getVolumeControlCapabilityLevel() {
		return CapabilityPriorityLevel.HIGH;
	}
	
	@Override
	public void volumeUp(ResponseListener<Object> listener) {
		sendKeyCode(VirtualKeycodes.VOLUME_UP.getCode(), listener);
	}

	@Override
	public void volumeDown(ResponseListener<Object> listener) {
		sendKeyCode(VirtualKeycodes.VOLUME_DOWN.getCode(), listener);
	}

	@Override
	public void setVolume(float volume, ResponseListener<Object> listener) {
		// Do nothing - not supported
			Util.postError(listener, ServiceCommandError.notSupported());
	}

	@Override
	public void getVolume(final VolumeListener listener) {
		getVolumeStatus(new VolumeStatusListener() {

			@Override
			public void onSuccess(VolumeStatus status) {
				Util.postSuccess(listener, status.volume);
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		});
	}

	@Override
	public void setMute(final boolean isMute, final ResponseListener<Object> listener) {
		getVolumeStatus(new VolumeStatusListener() {
			
			@Override
			public void onSuccess(VolumeStatus status) {
				if ( isMute != status.isMute ) {
					sendKeyCode(VirtualKeycodes.MUTE.getCode(), listener);
				}
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		});		
	}

	@Override
	public void getMute(final MuteListener listener) {
		getVolumeStatus(new VolumeStatusListener() {
			
			@Override
			public void onSuccess(VolumeStatus status) {
				Util.postSuccess(listener, status.isMute);
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		});
	}

	@Override
	public ServiceSubscription<VolumeListener> subscribeVolume(VolumeListener listener) {
		// Do nothing - not supported
		Util.postError(listener, ServiceCommandError.notSupported());
		
		return null;
	}

	@Override
	public ServiceSubscription<MuteListener> subscribeMute(MuteListener listener) {
		// Do nothing - not supported
		Util.postError(listener, ServiceCommandError.notSupported());
		
		return null;
	}
	
	private void getVolumeStatus(final VolumeStatusListener listener) {
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				String strObj = (String) response;
	            JSONObject volumeStatus = parseVolumeXmlToJSON(strObj);
				try {
		            boolean isMute = (Boolean) volumeStatus.get("mute");
		            int volume = (Integer) volumeStatus.get("level");
		            
		            Util.postSuccess(listener, new VolumeStatus(isMute, volume));
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		};
		
		String requestURL = getUDAPRequestURL(UDAP_PATH_DATA, TARGET_VOLUME_INFO);

		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, requestURL, null, responseListener);
		request.setHttpMethod(ServiceCommand.TYPE_GET);
		request.send();	
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
		final String appName = "Input List";
		final String encodedStr = HttpMessage.percentEncoding(appName);

		getApplication(encodedStr, new AppInfoListener() {
			
			@Override
			public void onSuccess(final AppInfo appInfo) {
				Launcher.AppLaunchListener launchListener = new Launcher.AppLaunchListener() {
					
					@Override
					public void onSuccess(LaunchSession session) {
						if ( inputPickerSession == null ) {
							inputPickerSession = session;
						}

						Util.postSuccess(listener, session);
					}
					
					@Override
					public void onError(ServiceCommandError error) {
						Util.postError(listener, error);
					}
				};
				
				launchApplication(appName, appInfo.getId(), null, launchListener);
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		});
	}
	
	@Override
	public void closeInputPicker(LaunchSession launchSession, ResponseListener<Object> listener) {
		if (inputPickerSession != null) {
			inputPickerSession.close(listener);
		}
	}
	
	@Override
	public void getExternalInputList(ExternalInputListListener listener) {
		// Do nothing - not Supported
		Util.postError(listener, ServiceCommandError.notSupported());
	}

	@Override
	public void setExternalInput(ExternalInputInfo input, ResponseListener<Object> listener) {
		// Do nothing - not Supported
		Util.postError(listener, ServiceCommandError.notSupported());
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
		return CapabilityPriorityLevel.HIGH;
	}
	
	@Override
	public void displayImage(final String url, final String mimeType, final String title, final String description, final String iconSrc, final MediaPlayer.LaunchListener listener) {
		if ( dlnaService != null ) {
			dlnaService.displayImage(url, mimeType, title, description, iconSrc, listener);
		}
		else {
			System.err.println("DLNA Service is not ready yet");
		}
	}
	
	@Override
	public void playMedia(final String url, final String mimeType, final String title, final String description, final String iconSrc, final boolean shouldLoop, final MediaPlayer.LaunchListener listener) {
		if ( dlnaService != null ) {
			dlnaService.playMedia(url, mimeType, title, description, iconSrc, shouldLoop, listener);
		}
		else {
			System.err.println("DLNA Service is not ready yet");
		}
	}
	
	@Override
	public void closeMedia(LaunchSession launchSession, ResponseListener<Object> listener) {
		if (dlnaService == null) {
			Util.postError(listener, new ServiceCommandError(0, "Service is not connected", null));
			return;
		}

		dlnaService.closeMedia(launchSession, listener);
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
		sendKeyCode(VirtualKeycodes.PLAY.getCode(), listener);
	}

	@Override
	public void pause(ResponseListener<Object> listener) {
		sendKeyCode(VirtualKeycodes.PAUSE.getCode(), listener);
	}

	@Override
	public void stop(final ResponseListener<Object> listener) {
		sendKeyCode(VirtualKeycodes.STOP.getCode(), listener);
	}

	@Override
	public void rewind(ResponseListener<Object> listener) {
		sendKeyCode(VirtualKeycodes.REWIND.getCode(), listener);
	}

	@Override
	public void fastForward(ResponseListener<Object> listener) {
		sendKeyCode(VirtualKeycodes.FAST_FORWARD.getCode(), listener);
	}
	
	@Override
	public void seek(long position, ResponseListener<Object> listener) {
		if ( dlnaService != null ) {
			dlnaService.seek(position, listener);
		}
	}
	
	@Override
	public void getDuration(DurationListener listener) {
		if ( dlnaService != null ) {
			dlnaService.getDuration(listener);
		}
	}
	
	@Override
	public void getPosition(PositionListener listener) {
		if ( dlnaService != null ) {
			dlnaService.getPosition(listener);
		}
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
	
	private void setMouseCursorVisible(boolean visible, ResponseListener<Object> listener) {
		String requestURL = getUDAPRequestURL(UDAP_PATH_EVENT);
		
		Map <String,String> params = new HashMap<String,String>();
		params.put("name", "CursorVisible");
		params.put("value", visible? "true" : "false");
		params.put("mode", "auto");
		
		String httpMessage = getUDAPMessageBody(UDAP_API_EVENT, params);

		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, requestURL, httpMessage, listener);
		request.send();
	}
	
	@Override
	public void connectMouse() {
		ResponseListener<Object> listener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				Log.d("Connect SDK", "Netcast TV's mouse has been connected");
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Log.w("Connect SDK", "Netcast TV's mouse connection has been failed");
			}
		};
		
		setMouseCursorVisible(true, listener);
	}
	
	@Override
	public void disconnectMouse() {
		setMouseCursorVisible(false, null);
	}
	
	@Override
	public void click() {
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Log.w("Connect SDK", "Netcast TV's mouse click has been failed");
			}
		};
		
		String requestURL = getUDAPRequestURL(UDAP_PATH_COMMAND);
		
		Map <String,String> params = new HashMap<String,String>();
		params.put("name", "HandleTouchClick");
		
		String httpMessage = getUDAPMessageBody(UDAP_API_COMMAND, params);

		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, requestURL, httpMessage, responseListener);
		request.send();
	}

	@Override
	public void move(final double dx, final double dy) {
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Log.w("Connect SDK", "Netcast TV's mouse move has been failed");
			}
		};
		
		String requestURL = getUDAPRequestURL(UDAP_PATH_COMMAND);
		
		int x = (int)dx;
		int y = (int)dy;

		Map <String,String> params = new HashMap<String,String>();
		params.put("name", "HandleTouchMove");
		params.put("x", String.valueOf(x));
		params.put("y", String.valueOf(y));
		
		String httpMessage = getUDAPMessageBody(UDAP_API_COMMAND, params);

		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, requestURL, httpMessage, responseListener);
		request.send();
	}
	
	@Override
	public void move(PointF diff) {
		move(diff.x, diff.y);
	}

	@Override
	public void scroll(double dx, double dy) {
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Log.w("Connect SDK", "Netcast TV's mouse scroll has been failed");
			}
		};
		
		String requestURL = getUDAPRequestURL(UDAP_PATH_COMMAND);
		
		Map <String,String> params = new HashMap<String,String>();
		params.put("name", "HandleTouchWheel");
		if ( dy > 0 ) 
			params.put("value", "up");
		else 
			params.put("value", "down");
		
		String httpMessage = getUDAPMessageBody(UDAP_API_COMMAND, params);

		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, requestURL, httpMessage, responseListener);
		request.send();
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
	public ServiceSubscription<TextInputStatusListener> subscribeTextInputStatus(final TextInputStatusListener listener) {
		keyboardString = new StringBuilder();

		URLServiceSubscription<TextInputStatusListener> request = new URLServiceSubscription<TextInputStatusListener>(this, "KeyboardVisible", null, null);
		request.addListener(listener);
		subscriptions.add(request);

		return request;
	}
	
	@Override
	public void sendText(final String input) {
		Log.d("Connect SDK", "Add to Queue: " + input);
		keyboardString.append(input);
		handleKeyboardInput("Editing", keyboardString.toString());
	}
	
	@Override
	public void sendEnter() {
		
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Log.w("Connect SDK", "Netcast TV's enter key has been failed");
			}
		};
		handleKeyboardInput("EditEnd", keyboardString.toString());
		sendKeyCode(VirtualKeycodes.RED.getCode(), responseListener);		// Send RED Key to enter the "ENTER" button
	}

	@Override
	public void sendDelete() {
		if ( keyboardString.length() > 1 ) {
			keyboardString.deleteCharAt(keyboardString.length()-1);
		}
		else {
			keyboardString = new StringBuilder();
		}
		
		handleKeyboardInput("Editing", keyboardString.toString());
	}
	
	private void handleKeyboardInput(final String state, final String buffer) {
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Log.w("Connect SDK", "Netcast TV's keyboard input has been failed");
			}
		};
		
		String requestURL = getUDAPRequestURL(UDAP_PATH_EVENT);

		Map <String,String> params = new HashMap<String,String>();
		params.put("name", "TextEdited");
		params.put("state", state);
		params.put("value", buffer);
		
		String httpMessage = getUDAPMessageBody(UDAP_API_EVENT, params);

		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, requestURL, httpMessage, responseListener);
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

	@Override
	public void up(final ResponseListener<Object> listener) {
		sendKeyCode(VirtualKeycodes.KEY_UP.getCode(), listener);
	}

	@Override
	public void down(final ResponseListener<Object> listener) {
		sendKeyCode(VirtualKeycodes.KEY_DOWN.getCode(), listener);
	}

	@Override
	public void left(final ResponseListener<Object> listener) {
		sendKeyCode(VirtualKeycodes.KEY_LEFT.getCode(), listener);
	}

	@Override
	public void right(final ResponseListener<Object> listener) {
		sendKeyCode(VirtualKeycodes.KEY_RIGHT.getCode(), listener);
	}

	@Override
	public void ok(final ResponseListener<Object> listener) {
		sendKeyCode(VirtualKeycodes.OK.getCode(), listener);
	}

	@Override
	public void back(final ResponseListener<Object> listener) {
		sendKeyCode(VirtualKeycodes.BACK.getCode(), listener);
	}

	@Override
	public void home(final ResponseListener<Object> listener) {
		sendKeyCode(VirtualKeycodes.HOME.getCode(), listener);
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
				Log.w("Connect SDK", "Netcast TV's power off has been failed");
			}
		};
		
		sendKeyCode(VirtualKeycodes.POWER.getCode(), responseListener);
	}
	
	private JSONObject parseVolumeXmlToJSON(String data) {
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		try {
			InputStream stream = new ByteArrayInputStream(data.getBytes("UTF-8"));
			
			SAXParser saxParser = saxParserFactory.newSAXParser();
			NetcastVolumeParser handler = new NetcastVolumeParser();
			saxParser.parse(stream, handler);

			return handler.getVolumeStatus();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private int parseAppNumberXmlToJSON(String data) {
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		try {
			InputStream stream = new ByteArrayInputStream(data.getBytes("UTF-8"));
			
			SAXParser saxParser = saxParserFactory.newSAXParser();
			NetcastAppNumberParser handler = new NetcastAppNumberParser();
			saxParser.parse(stream, handler);

			return handler.getApplicationNumber();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	private JSONArray parseApplicationsXmlToJSON(String data) {
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		try {
			InputStream stream = new ByteArrayInputStream(data.getBytes("UTF-8"));
			
			SAXParser saxParser = saxParserFactory.newSAXParser();
			NetcastApplicationsParser handler = new NetcastApplicationsParser();
			saxParser.parse(stream, handler);

			return handler.getApplications();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String getHttpMessageForHandleKeyInput(final int keycode) {
		String strKeycode = String.valueOf(keycode);
		
		Map <String,String> params = new HashMap<String,String>();
		params.put("name", "HandleKeyInput");
		params.put("value", strKeycode);
		
		return getUDAPMessageBody(UDAP_API_COMMAND, params);
	}
	
	@Override
	public void sendKeyCode(final int keycode, final ResponseListener<Object> listener) {
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				try {
					Thread.sleep(150);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				String requestURL = getUDAPRequestURL(UDAP_PATH_COMMAND);
				String httpMessage = getHttpMessageForHandleKeyInput(keycode);

				ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(NetcastTVService.this, requestURL, httpMessage, listener);
				request.send();
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		};
		
		setMouseCursorVisible(false, responseListener);
	}
	
	private String getUDAPRequestURL(String path) {
		return getUDAPRequestURL(path, null);
	}
	
	private String getUDAPRequestURL(String path, String target) {
		return getUDAPRequestURL(path, target, null);
	}
	
	private String getUDAPRequestURL(String path, String target, String type) {
		return getUDAPRequestURL(path, target, type, null, null);
	}

	private String getUDAPRequestURL(String path, String target, String type, String index, String number) {
		// Type Values
		// 1: List of all apps
		// 2: List of apps in the Premium category
		// 3: List of apps in the My Apps category
		
		StringBuilder sb = new StringBuilder();
		sb.append("http://");
		sb.append(serviceDescription.getIpAddress());
		sb.append(":");
		sb.append(serviceDescription.getPort());
		sb.append(path);
		
		if ( target != null ) { 
			sb.append("?target=");
			sb.append(target);
			
			if ( type != null ) {
				sb.append("&type=");
				sb.append(type);
			}
			
			if ( index != null ) {
				sb.append("&index=");
				sb.append(index);
			}
			
			if ( number != null ) {
				sb.append("&number=");
				sb.append(number);
			}
		}
		
		return sb.toString();
	}
	
	private String getUDAPMessageBody(String api, Map<String, String> params) {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		sb.append("<envelope>");
		sb.append("<api type=\"" + api + "\">");
		
		for (Map.Entry<String, String> entry : params.entrySet()) {
		    String key = entry.getKey();
		    String value = entry.getValue();
	        
		    sb.append(createNode(key, value));
		}
		
		sb.append("</api>");
		sb.append("</envelope>");
		
		return sb.toString();
	}
	
	private String createNode(String tag, String value) {
		StringBuilder sb = new StringBuilder();

		sb.append("<" + tag + ">");
		sb.append(value);
		sb.append("</" + tag + ">");
		
		return sb.toString();
	}
	
	public String decToHex(String dec) {
		if ( dec != null && dec.length() > 0 ) 
			return decToHex(Long.parseLong(dec));
		return null;
	}
	
	public String decToHex(long dec) {
		return String.format("%016x",dec);
	}
	
	@Override
	public void sendCommand(final ServiceCommand<?> mCommand) {
		Thread thread = new Thread(new Runnable() {
			
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				final ServiceCommand<ResponseListener<Object>> command = (ServiceCommand<ResponseListener<Object>>) mCommand;

				Object payload = command.getPayload();
				
				HttpRequestBase request = command.getRequest();
				request.addHeader(HttpMessage.USER_AGENT, HttpMessage.UDAP_USER_AGENT);
				HttpResponse response = null;

				if (payload != null && command.getHttpMethod().equalsIgnoreCase(ServiceCommand.TYPE_POST)) {
					HttpEntity entity = null;
					
					try {
						if (payload instanceof String) {
							entity = new StringEntity((String) payload);
						} else if (payload instanceof JSONObject) {
							entity = new StringEntity(((JSONObject) payload).toString());
						}
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					
					((HttpPost) request).setEntity(entity);
				}

				try {
					response = httpClient.execute(request);
					
					final int code = response.getStatusLine().getStatusCode();
					
					if ( code == 200 ) { 
			            HttpEntity entity = response.getEntity();
			            final String message = EntityUtils.toString(entity, "UTF-8");
			            
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
		
		thread.start();
	}
	
	@Override
	public void unsubscribe(URLServiceSubscription<?> subscription) {
		subscriptions.remove(subscription);
	}
	
//	@Override
//	public LaunchSession decodeLaunchSession(String type, JSONObject obj) throws JSONException {
//		if ("netcasttv".equals(type)) {
//			LaunchSession launchSession = LaunchSession.launchSessionFromJSONObject(obj);
//			launchSession.setService(this);
//
//			return launchSession;
//		}
//		return null;
//	}
	
	private void setCapabilities() {
		if (DiscoveryManager.getInstance().getPairingLevel() == PairingLevel.ON) {
			appendCapabilites(TextInputControl.Capabilities);
			appendCapabilites(MouseControl.Capabilities);
			appendCapabilites(KeyControl.Capabilities);
			appendCapabilites(PowerControl.Capabilities);
			appendCapabilites(MediaPlayer.Capabilities);
			
			appendCapabilites(
					Play, 
					Pause, 
					Stop, 
					Rewind, 
					FastForward, 
					Duration, 
					Position, 
					Seek, 
					MetaData_Title, 
					MetaData_MimeType, 

					Application, 
					Application_Close, 
					Application_List, 
					Browser, 
					Hulu, 
					Netflix, 
					Netflix_Params, 
					YouTube, 
					YouTube_Params, 
					AppStore, 
					AppStore_Params, 

					Channel_Up, 
					Channel_Down, 
					Channel_Get, 
					Channel_List, 
					Channel_Subscribe, 
					Get_3D, 
					Set_3D, 
					Subscribe_3D, 

					Picker_Launch, 
					Picker_Close, 

					Volume_Get, 
					Volume_Up_Down, 
					Mute_Get, 
					Mute_Set
			);
		} else {
			appendCapabilites(MediaPlayer.Capabilities);
			appendCapabilites(
					Play, 
					Pause, 
					Stop, 
					Rewind, 
					FastForward, 
					
					YouTube, 
					YouTube_Params
			);
		}
	}

	@Override
	public void getPlayState(PlayStateListener listener) {
			Util.postError(listener, ServiceCommandError.notSupported());
	}

	@Override
	public ServiceSubscription<PlayStateListener> subscribePlayState(PlayStateListener listener) {
			Util.postError(listener, ServiceCommandError.notSupported());
		return null;
	}
}
