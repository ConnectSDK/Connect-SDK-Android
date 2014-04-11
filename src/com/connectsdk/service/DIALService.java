package com.connectsdk.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.connectsdk.core.AppInfo;
import com.connectsdk.core.Util;
import com.connectsdk.device.ConnectableDeviceStore;
import com.connectsdk.service.capability.Launcher;
import com.connectsdk.service.capability.Launcher.AppState;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.NotSupportedServiceSubscription;
import com.connectsdk.service.command.ServiceCommand;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;
import com.connectsdk.service.sessions.LaunchSession;

public class DIALService extends DeviceService implements Launcher {
//	interface AppStateListener {
//		void onAppStateSuccess(String state);
//		void onAppStateFailed(ServiceCommandError error);
//	}
	
	HttpClient httpClient;

	public DIALService(ServiceDescription serviceDescription, ServiceConfig serviceConfig, ConnectableDeviceStore connectableDeviceStore) {
		super(serviceDescription, serviceConfig, connectableDeviceStore);
		
		setCapabilities();
		
		httpClient = new DefaultHttpClient();
		ClientConnectionManager mgr = httpClient.getConnectionManager();
		HttpParams params = httpClient.getParams();
		httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(params, mgr.getSchemeRegistry()), params);

		probeForAppSupport();
	}

	public static JSONObject discoveryParameters() {
		JSONObject params = new JSONObject();
		
		try {
			params.put("serviceId", "DIAL");
			params.put("filter",  "urn:dial-multiscreen-org:service:dial:1");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return params;
	}
	
	@Override
	public Launcher getLauncher() {
		return this;
	}

	@Override
	public CapabilityPriorityLevel getLauncherCapabilityLevel() {
		return CapabilityPriorityLevel.NORMAL;
	}
	
	@Override
	public void launchApp(String appId, AppLaunchListener listener) {
		launchApp(appId, null, listener);
	}
	
	private void launchApp(String appId, JSONObject params, AppLaunchListener listener) {
		if (appId == null || appId.length() == 0) {
			Util.postError(listener, new ServiceCommandError(0, "Must pass a valid appId", null));
			return;
		}
		
		AppInfo appInfo = new AppInfo();
		appInfo.setId(appId);
		
		launchAppWithInfo(appInfo, listener);
	}
	
//	private void launchApplication(final String appName, String contentId, final AppLaunchListener listener) {
//		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
//			
//			@Override
//			public void onSuccess(Object response) {
//				LaunchSession launchSession = new LaunchSession();
//				launchSession.setService(DIALService.this);
//				launchSession.setAppName(appName);
//
//				Util.postSuccess(listener, launchSession);
//			}
//			
//			@Override
//			public void onError(ServiceCommandError error) {
//				Util.postError(listener, error);
//			}
//		};
//		
//		String uri = requestURL(appName);
//		
//		String payload = null;
//		if ( contentId != null ) {
//			StringBuilder sb = new StringBuilder();
//			sb.append("v");
//			sb.append("=");
//			sb.append(contentId);
//			
//			payload = sb.toString();
//		}
//		
//		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, payload, responseListener);
//		request.send();
//	}

	@Override
	public void launchAppWithInfo(AppInfo appInfo, AppLaunchListener listener) {
		launchAppWithInfo(appInfo, null, listener);
	}

	@Override
	public void launchAppWithInfo(final AppInfo appInfo, Object params, final AppLaunchListener listener) {
		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(this, requestURL(appInfo.getName()), params, new ResponseListener<Object>() {
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, new ServiceCommandError(0, "Problem Launching app", null));
			}
			
			@Override
			public void onSuccess(Object object) {
				LaunchSession launchSession = LaunchSession.launchSessionForAppId(appInfo.getId());
				launchSession.setAppName(appInfo.getName());
				launchSession.setRawData(object);
				launchSession.setService(DIALService.this);
				
				Util.postSuccess(listener, launchSession);
			}
		});
		
		command.send();
	}

	@Override
	public void launchBrowser(String url, AppLaunchListener listener) {
		Util.postError(listener, ServiceCommandError.notSupported());
	}
	
	@Override
	public void closeApp(final LaunchSession launchSession, final ResponseListener<Object> listener) {
		getAppState(launchSession.getAppName(), new AppStateListener() {
			
			@Override
			public void onSuccess(AppState state) {
				String uri = requestURL(launchSession.getAppName());
				
				if (state.running) {
					uri += "/run";
				}
				
				ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(launchSession.getService(), uri, null, listener);
				command.setHttpMethod(ServiceCommand.TYPE_DEL);
				command.send();
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		});
	}

	@Override
	public void launchYouTube(String contentId, final AppLaunchListener listener) {
		String params = null;
		AppInfo appInfo = new AppInfo("YouTube");
		appInfo.setName(appInfo.getId());

		if (contentId != null && contentId.length() > 0)
			params = String.format("v=%@&t=0.0", contentId);

		launchAppWithInfo(appInfo, params, listener);
	}

	@Override
	public void launchHulu(String contentId, AppLaunchListener listener) {
		Util.postError(listener, ServiceCommandError.notSupported());
	}

	@Override
	public void launchNetflix(final String contentId, AppLaunchListener listener) {
		JSONObject params = null;
		
		if (contentId != null && contentId.length() > 0) {
			try {
				new JSONObject() {{
					put("v", contentId);
				}};
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		AppInfo appInfo = new AppInfo("Netflix");
		appInfo.setName(appInfo.getId());
		
		launchAppWithInfo(appInfo, params, listener);
	}
	
	@Override
		public void launchAppStore(String appId, AppLaunchListener listener) {
			Util.postError(listener, ServiceCommandError.notSupported());
		}

	private void getAppState(String appName, final AppStateListener listener) {
		ResponseListener<Object> responseListener = new ResponseListener<Object>() {
			
			@Override
			public void onSuccess(Object response) {
				String str = (String)response;
				String[] stateTAG = new String[2];
				stateTAG[0] = "<state>";
				stateTAG[1] = "</state>";
				
				
				int start = str.indexOf(stateTAG[0]);
				int end = str.indexOf(stateTAG[1]);
				
				if ( start != -1 && end != -1 ) {
					start += stateTAG[0].length();
					
					String state = str.substring(start, end);
					
					// TODO: This isn't actually reporting anything.
//					if ( listener != null ) 
//						listener.onAppStateSuccess(state);
				}
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		};
		
		String uri = requestURL(appName);
		
		ServiceCommand<ResponseListener<Object>> request = new ServiceCommand<ResponseListener<Object>>(this, uri, null, responseListener);
		request.setHttpMethod(ServiceCommand.TYPE_GET);

		request.send();
	}
	
	@Override
	public void getAppList(AppListListener listener) {
		Util.postError(listener, ServiceCommandError.notSupported());
	}

	@Override
	public void getRunningApp(AppInfoListener listener) {
		Util.postError(listener, ServiceCommandError.notSupported());
	}

	@Override
	public ServiceSubscription<AppInfoListener> subscribeRunningApp(AppInfoListener listener) {
		Util.postError(listener, ServiceCommandError.notSupported());
		
		return new NotSupportedServiceSubscription<AppInfoListener>();
	}
	
	@Override
	public void getAppState(LaunchSession launchSession, AppStateListener listener) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public ServiceSubscription<AppStateListener> subscribeAppState(
			LaunchSession launchSession,
			com.connectsdk.service.capability.Launcher.AppStateListener listener) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void sendCommand(final ServiceCommand<?> mCommand) {
		Util.runInBackground(new Runnable() {
			
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				ServiceCommand<ResponseListener<Object>> command = (ServiceCommand<ResponseListener<Object>>) mCommand;
				Object payload = command.getPayload();
				
				HttpRequestBase request = command.getRequest();
				HttpResponse response = null;
				int code = -1;
				
				if (payload != null && command.getHttpMethod().equalsIgnoreCase(ServiceCommand.TYPE_POST)) {
					HttpPost post = (HttpPost) request;
					HttpEntity entity = null;
					try {
						if (payload instanceof String) {
							entity = new StringEntity((String) payload);
						
						} else if (payload instanceof JSONObject) {
							entity = new StringEntity((String) payload);
						}
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
						//  Error is handled below if entity is null;
					}

					if (entity == null) {
						Util.postError(command.getResponseListener(), new ServiceCommandError(0, "Unknown Error while preparing to send message", null));

						return;
					}
					
					post.setEntity(entity);
				}
			
				try {
					response = httpClient.execute(request);
					
					code = response.getStatusLine().getStatusCode();
					
					if ( code == 200 || code == 201) { 
			            HttpEntity entity = response.getEntity();
			            String message = EntityUtils.toString(entity, "UTF-8");

						Util.postSuccess(command.getResponseListener(), message);
					}
					else {
						Util.postError(command.getResponseListener(), ServiceCommandError.getError(code));
					}
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	private String requestURL(String appName) {
		StringBuilder sb = new StringBuilder();
		
		sb.append(serviceDescription.getApplicationURL());
		sb.append(appName);
		
		return sb.toString();
	}
	
	private void setCapabilities() {
		appendCapabilites(
				Application, 
				Application_Params, 
				Application_Close, 
				AppState
		);
	}
	
	private void hasApplication(String appID, ResponseListener<Object> listener) {
		String uri = requestURL(appID);
		
		ServiceCommand<ResponseListener<Object>> command = new ServiceCommand<ResponseListener<Object>>(this, uri, null, listener);
		command.setHttpMethod(ServiceCommand.TYPE_GET);
		command.send();
	}
	
	private void probeForAppSupport() {
		List<String> appsToProbe = new ArrayList<String>();
		appsToProbe.add("YouTube");
		appsToProbe.add("Netflix");
		
		for (final String appID : appsToProbe) {
			hasApplication(appID, new ResponseListener<Object>() {
				
				@Override public void onError(ServiceCommandError error) { }
				
				@Override
				public void onSuccess(Object object) {
					addCapability("Launcher." + appID);
				}
			});
		}
	}
}
