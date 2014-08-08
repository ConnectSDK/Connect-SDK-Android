package com.connectsdk.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.connectsdk.core.Util;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.WebAppLauncher;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;
import com.connectsdk.service.sessions.LaunchSession;
import com.connectsdk.service.sessions.LaunchSession.LaunchSessionType;
import com.connectsdk.service.sessions.MultiScreenWebAppSession;
import com.connectsdk.service.sessions.WebAppSession;
import com.samsung.multiscreen.application.Application;
import com.samsung.multiscreen.application.Application.Status;
import com.samsung.multiscreen.application.ApplicationAsyncResult;
import com.samsung.multiscreen.application.ApplicationError;
import com.samsung.multiscreen.device.Device;
import com.samsung.multiscreen.device.DeviceAsyncResult;
import com.samsung.multiscreen.device.DeviceError;
import com.samsung.multiscreen.device.DeviceFactory;

public class MultiScreenService extends DeviceService implements MediaPlayer, WebAppLauncher {
	public static final String ID = "MultiScreen";
	
	Device device;
	Map<String, MultiScreenWebAppSession> sessions;

	public MultiScreenService(ServiceDescription serviceDescription,
			ServiceConfig serviceConfig) {
		super(serviceDescription, serviceConfig);

		Map<String, Object> map = new HashMap<String, Object>();
		
		map.put("DeviceName", serviceDescription.getFriendlyName());
		map.put("DialURI", serviceDescription.getApplicationURL());
		map.put("IP", serviceDescription.getIpAddress());
		map.put("ModelDescription", serviceDescription.getModelDescription());
		map.put("ModelName", serviceDescription.getModelName());
		map.put("ServiceURI", serviceDescription.getServiceURI());
		
		this.device = DeviceFactory.createWithMap(map);
	}

	public static JSONObject discoveryParameters() {
		JSONObject params = new JSONObject();
		
		try {
			params.put("serviceId", ID);
//			params.put("filter",  "urn:samsung.com:service:MultiScreenService:1");
			params.put("filter",  "urn:dial-multiscreen-org:service:dial:1");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return params;
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
		
		sessions = new HashMap<String, MultiScreenWebAppSession>();
		
		reportConnected(true);
	}
	
	@Override
	public void disconnect() {
		for (MultiScreenWebAppSession session: sessions.values()) {
			session.disconnectFromWebApp();
		}
		
		connected = false;
		
		if (mServiceReachability != null)
			mServiceReachability.stop();
		
		Util.runOnUI(new Runnable() {
			
			@Override
			public void run() {
				if (listener != null)
					listener.onDisconnect(MultiScreenService.this, null);
			}
		});
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
	public void displayImage(final String url, final String mimeType, final String title,
			final String description, final String iconSrc, final LaunchListener listener) {
		final String webAppId = "ConnectSDKSampler";

		getWebAppLauncher().joinWebApp(webAppId, new WebAppSession.LaunchListener() {
			
			@Override
			public void onSuccess(WebAppSession webAppSession) {
				webAppSession.getMediaPlayer().displayImage(url, mimeType, title, description, iconSrc, listener);
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				getWebAppLauncher().launchWebApp(webAppId, new WebAppSession.LaunchListener() {
					
					@Override
					public void onError(ServiceCommandError error) {
						if (listener != null) {
							Util.postError(listener, error);
						}
					}
					
					@Override
					public void onSuccess(final WebAppSession webAppSession) {
						webAppSession.connect(new ResponseListener<Object>() {
							
							@Override
							public void onError(ServiceCommandError error) {
								Util.postError(listener, error);
							}
							
							@Override
							public void onSuccess(Object object) {
								webAppSession.getMediaPlayer().displayImage(url, mimeType, title, description, iconSrc, listener);
							}
						});
					}
				});
			}
		});
	}

	@Override
	public void playMedia(final String url, final String mimeType, final String title,
			final String description, final String iconSrc, final boolean shouldLoop,
			final LaunchListener listener) {
		final String webAppId = "ConnectSDKSampler";

		getWebAppLauncher().joinWebApp(webAppId, new WebAppSession.LaunchListener() {
			
			@Override
			public void onSuccess(WebAppSession webAppSession) {
				webAppSession.playMedia(url, mimeType, title, description, iconSrc, shouldLoop, listener);
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				getWebAppLauncher().launchWebApp(webAppId, new WebAppSession.LaunchListener() {
					
					@Override
					public void onError(ServiceCommandError error) {
						if (listener != null) {
							Util.postError(listener, error);
						}
					}
					
					@Override
					public void onSuccess(final WebAppSession webAppSession) {
						webAppSession.connect(new ResponseListener<Object>() {
							
							@Override
							public void onError(ServiceCommandError error) {
								Util.postError(listener, error);
							}
							
							@Override
							public void onSuccess(Object object) {
								webAppSession.playMedia(url, mimeType, title, description, iconSrc, shouldLoop, listener);
							}
						});
					}
				});
			}
		});
	}

	@Override
	public void closeMedia(LaunchSession launchSession,
			ResponseListener<Object> listener) {
		getWebAppLauncher().closeWebApp(launchSession, listener);
	}
	
	@Override
	public WebAppLauncher getWebAppLauncher() {
		return this;
	}

	@Override
	public CapabilityPriorityLevel getWebAppLauncherCapabilityLevel() {
		return CapabilityPriorityLevel.HIGH;
	}

	@Override
	public void launchWebApp(String webAppId, WebAppSession.LaunchListener listener) {
		launchWebApp(webAppId, null, true, listener);
	}
	
	@Override
	public void launchWebApp(
			String webAppId,
			JSONObject params,
			final com.connectsdk.service.sessions.WebAppSession.LaunchListener listener) {
		launchWebApp(webAppId, params, true, listener);
	}

	@Override
	public void launchWebApp(
			String webAppId,
			boolean relaunchIfRunning,
			com.connectsdk.service.sessions.WebAppSession.LaunchListener listener) {
		launchWebApp(webAppId, null, relaunchIfRunning, listener);
	}

	@Override
	public void launchWebApp(
			final String webAppId,
			JSONObject params,
			boolean relaunchIfRunning,
			final com.connectsdk.service.sessions.WebAppSession.LaunchListener listener) {
		ServiceCommandError error = null;
		
	    if (webAppId == null || webAppId.length() == 0) {
	    	error = new ServiceCommandError(0, "You must provide a valid web app id", null);
	    }

	    if (device == null) {
	    	error = new ServiceCommandError(0, "Could not find a reference to the native device object", null);
	    }

	    if (error != null) {
	        if (listener != null) {
	        	Util.postError(listener, error);
	        }

	        return;
	    }
	    
	    if (params == null) {
	    	params = new JSONObject();
	    }
	    final JSONObject fParams = params;
		
	    device.getApplication(webAppId, new DeviceAsyncResult<Application>() {
			
			@Override
			public void onResult(final Application application) {
				Map<String, String> parameters = new HashMap<String, String>();
				
				Iterator keys = fParams.keys();
				while (keys.hasNext()) {
					String key = (String) keys.next();
					try {
						parameters.put(key, fParams.getString(key));
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				
				application.launch(parameters, new ApplicationAsyncResult<Boolean>() {

					@Override
					public void onError(ApplicationError error) {
						Util.postError(listener, new ServiceCommandError((int)error.getCode(), error.getMessage(), error));
					}

					@Override
					public void onResult(Boolean launchSuccess) {
						if (launchSuccess) {
							LaunchSession launchSession = LaunchSession.launchSessionForAppId(webAppId);
							launchSession.setSessionType(LaunchSessionType.WebApp);
							launchSession.setService(MultiScreenService.this);

							MultiScreenWebAppSession webAppSession = sessions.get(webAppId);
									
							if (webAppSession == null) {
								webAppSession = new MultiScreenWebAppSession(launchSession, MultiScreenService.this);
								sessions.put(webAppId, webAppSession);
							}
							
		                    webAppSession.setApplication(application);

							if (listener != null) {
								Util.postSuccess(listener, webAppSession);
							}
						} 
						else {
							if (listener != null) {
								Util.postError(listener, new ServiceCommandError(0, "Experienced an unknown error launching app", null));
							}
						}
					}
				});
			}
			
			@Override
			public void onError(DeviceError error) {
				Util.postError(listener, new ServiceCommandError((int)error.getCode(), error.getMessage(), error));
			}
		});
	}

	@Override
	public void joinWebApp(
			final LaunchSession webAppLaunchSession,
			final com.connectsdk.service.sessions.WebAppSession.LaunchListener listener) {

		device.getApplication(webAppLaunchSession.getAppId(), new DeviceAsyncResult<Application>() {
			
			@Override
			public void onResult(Application application) {
				final MultiScreenWebAppSession webAppSession;
				
				if (sessions.containsKey(webAppLaunchSession.getAppId())) {
					webAppSession = sessions.get(webAppLaunchSession.getAppId());
				}
				else {
					webAppSession = new MultiScreenWebAppSession(webAppLaunchSession, MultiScreenService.this);
					sessions.put(webAppLaunchSession.getAppId(), webAppSession);
				}
				
				webAppSession.setApplication(application);
				webAppSession.join(new ResponseListener<Object>() {
					
					@Override
					public void onError(ServiceCommandError error) {
						Util.postError(listener, error);
					}
					
					@Override
					public void onSuccess(Object object) {
						Util.postSuccess(listener, webAppSession);
					}
				});
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
	public void joinWebApp(String webAppId,
			com.connectsdk.service.sessions.WebAppSession.LaunchListener listener) {
	    LaunchSession launchSession = LaunchSession.launchSessionForAppId(webAppId);
	    launchSession.setSessionType(LaunchSessionType.WebApp);
	    launchSession.setService(this);

	    getWebAppLauncher().joinWebApp(launchSession, listener);
	}
	
	@Override
	public void closeWebApp(final LaunchSession launchSession,
			final ResponseListener<Object> listener) {
		ServiceCommandError error = null;
		
		if (launchSession == null || launchSession.getAppId() == null || launchSession.getAppId().length() == 0) {
			error = new ServiceCommandError(0, "You must provide a valid launch session", null);
		}
		
		if (device == null) {
			error = new ServiceCommandError(0, "Could not find a reference to the native device object", null);
		}
		
		if (error != null) {
			if (listener != null) {
				Util.postError(listener, error);
			}

			return;
		}

		device.getApplication(launchSession.getAppId(), new DeviceAsyncResult<Application>() {
			
			@Override
			public void onError(DeviceError error) {
				if (listener != null) {
					Util.postError(listener, new ServiceCommandError((int)error.getCode(), error.getMessage(), error));
				}
			}
			
			@Override
			public void onResult(Application application) {
				if (application.getLastKnownStatus() == Status.RUNNING) {
					application.terminate(new ApplicationAsyncResult<Boolean>() {
						
						@Override
						public void onResult(Boolean terminateSuccess) {
							if (terminateSuccess) {
								sessions.remove(launchSession.getAppId());
								
								if (listener != null) {
									Util.postSuccess(listener, null);
								}
							}
							else {
								if (listener != null) {
									Util.postError(listener, new ServiceCommandError(0, "Experienced an unknown error terminating app", null));
								}
							}
						}
						
						@Override
						public void onError(ApplicationError error) {
							Util.postError(listener, new ServiceCommandError((int)error.getCode(), error.getMessage(), error));
						}
					});
				}
				else {
					if (listener != null) {
						Util.postSuccess(listener, null);
					}
				}
			}
		});
	}
	
	public Device getDevice() {
		return device;
	}
	
	@Override
	protected void updateCapabilities() {
		List<String> capabilities = new ArrayList<String>();
	
		for (String capability : MediaPlayer.Capabilities) { capabilities.add(capability); }
		
		capabilities.add(MediaControl.Play);
		capabilities.add(MediaControl.Pause);
		capabilities.add(MediaControl.Duration);
		capabilities.add(MediaControl.Seek);
		capabilities.add(MediaControl.Position);
		capabilities.add(MediaControl.PlayState);
		capabilities.add(MediaControl.PlayState_Subscribe);
		
		capabilities.add(Launch);
		capabilities.add(Launch_Params);
		capabilities.add(Join);
		capabilities.add(Connect);
		capabilities.add(Disconnect);
		capabilities.add(Message_Send);
		capabilities.add(Message_Send_JSON);
		capabilities.add(Message_Receive);
		capabilities.add(Message_Receive_JSON);
		capabilities.add(WebAppLauncher.Close);
		
		setCapabilities(capabilities);
	}
}
