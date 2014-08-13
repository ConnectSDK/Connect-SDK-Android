/*
 * CastService
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 23 Feb 2014
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.connectsdk.core.ImageInfo;
import com.connectsdk.core.MediaInfo;
import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.VolumeControl;
import com.connectsdk.service.capability.WebAppLauncher;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.command.URLServiceSubscription;
import com.connectsdk.service.config.CastServiceDescription;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;
import com.connectsdk.service.sessions.CastWebAppSession;
import com.connectsdk.service.sessions.LaunchSession;
import com.connectsdk.service.sessions.LaunchSession.LaunchSessionType;
import com.connectsdk.service.sessions.WebAppSession;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.ApplicationConnectionResult;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.cast.RemoteMediaPlayer.MediaChannelResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.images.WebImage;

public class CastService extends DeviceService implements MediaPlayer, MediaControl, VolumeControl, WebAppLauncher {
	public interface LaunchWebAppListener{
		void onSuccess(WebAppSession webAppSession);
		void onFailure(ServiceCommandError error);
	};
	
	// @cond INTERNAL
	
	public static final String ID = "Chromecast";
	public final static String TAG = "Connect SDK";

	public final static String PLAY_STATE = "PlayState";
	public final static String CAST_SERVICE_VOLUME_SUBSCRIPTION_NAME = "volume";
	public final static String CAST_SERVICE_MUTE_SUBSCRIPTION_NAME = "mute";
	
	// @endcond
	
	String currentAppId;
	String launchingAppId;

	GoogleApiClient mApiClient;
    CastListener mCastClientListener;
    ConnectionCallbacks mConnectionCallbacks;
    ConnectionFailedListener mConnectionFailedListener;
    
    CastDevice castDevice;
    RemoteMediaPlayer mMediaPlayer;
    
    Map<String, CastWebAppSession> sessions;
	List<URLServiceSubscription<?>> subscriptions;
	
	float currentVolumeLevel;
	boolean currentMuteStatus;
    
	public CastService(ServiceDescription serviceDescription, ServiceConfig serviceConfig) {
		super(serviceDescription, serviceConfig);
		
		mCastClientListener = new CastListener();
        mConnectionCallbacks = new ConnectionCallbacks();
        mConnectionFailedListener = new ConnectionFailedListener();
        
        sessions = new HashMap<String, CastWebAppSession>();
        subscriptions = new ArrayList<URLServiceSubscription<?>>();
	}

	@Override
	public String getServiceName() {
		return ID;
	}

	public static JSONObject discoveryParameters() {
		JSONObject params = new JSONObject();
		
		try {
			params.put("serviceId", ID);
			params.put("filter",  "Chromecast");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return params;
	}
	
	
	@Override
	public void connect() {
		if (connected) 
			return;
		
		if (castDevice == null) {
			if (serviceDescription instanceof CastServiceDescription)
				this.castDevice = ((CastServiceDescription)serviceDescription).getCastDevice();
		}
		
		if (mApiClient == null) {
        	Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                      	.builder(castDevice, mCastClientListener);

        	mApiClient = new GoogleApiClient.Builder(DiscoveryManager.getInstance().getContext())
                              	.addApi(Cast.API, apiOptionsBuilder.build())
                              	.addConnectionCallbacks(mConnectionCallbacks)
                              	.addOnConnectionFailedListener(mConnectionFailedListener)
                              	.build();
        	
        	mApiClient.connect();
		}
	}

	@Override
	public void disconnect() {
		if (!connected)
			return;
		
		connected = false;
		
		Cast.CastApi.leaveApplication(mApiClient);
		mApiClient.disconnect();
		mApiClient = null;
		
		Util.runOnUI(new Runnable() {
			
			@Override
			public void run() {
				if (getListener() != null) {
					getListener().onDisconnect(CastService.this, null);
				}
			}
		});
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
		try {
			mMediaPlayer.play(mApiClient);
			
			Util.postSuccess(listener, null);
		} catch (Exception e) {
			Util.postError(listener, new ServiceCommandError(0, "Unable to play", null));
		}
	}

	@Override
	public void pause(final ResponseListener<Object> listener) {
        try {
			mMediaPlayer.pause(mApiClient);
			
			Util.postError(listener, null);
		} catch (Exception e) {
			Util.postError(listener, new ServiceCommandError(0, "Unable to pause", null));
		}
	}

	@Override
	public void stop(final ResponseListener<Object> listener) {
		try {
			mMediaPlayer.stop(mApiClient);

			Util.postError(listener, null);
		} catch (Exception e) {
			Util.postError(listener, new ServiceCommandError(0, "Unable to stop", null));
		}
	}

	@Override
	public void rewind(ResponseListener<Object> listener) {
		Util.postError(listener, ServiceCommandError.notSupported());
	}

	@Override
	public void fastForward(ResponseListener<Object> listener) {
		Util.postError(listener, ServiceCommandError.notSupported());
	}
	
	@Override
	public void seek(long position, final ResponseListener<Object> listener) {
		if (mMediaPlayer.getMediaStatus() == null) {
			Util.postError(listener, new ServiceCommandError(0, "There is no media currently available", null));
			return;
		}

        mMediaPlayer.seek(mApiClient, position, RemoteMediaPlayer.RESUME_STATE_UNCHANGED).setResultCallback(
                new ResultCallback<MediaChannelResult>() {

                	@Override
                    public void onResult(MediaChannelResult result) {
                        Status status = result.getStatus();

                        if (status.isSuccess()) {
                        	Util.postSuccess(listener, null);
                        } else {
                            Util.postError(listener, new ServiceCommandError(status.getStatusCode(), status.getStatusMessage(), status));
                        }
                    }
                });
	}

	@Override
	public void getDuration(final DurationListener listener) {
		if (mMediaPlayer.getMediaStatus() != null) {
			Util.postSuccess(listener, mMediaPlayer.getStreamDuration());
		}
		else {
			Util.postError(listener, new ServiceCommandError(0, "There is no media currently available", null));
		}
	}
	
	@Override
	public void getPosition(final PositionListener listener) {
		if (mMediaPlayer.getMediaStatus() != null) {
			Util.postSuccess(listener, mMediaPlayer.getApproximateStreamPosition());
		}
		else {
			Util.postError(listener, new ServiceCommandError(0, "There is no media currently available", null));
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
	
	private void attachMediaPlayer() {
        if (mMediaPlayer != null) {
            return;
        }

        mMediaPlayer = new RemoteMediaPlayer();
        mMediaPlayer.setOnStatusUpdatedListener(new RemoteMediaPlayer.OnStatusUpdatedListener() {

            @Override
            public void onStatusUpdated() {
                if (subscriptions.size() > 0) {
                	for (URLServiceSubscription<?> subscription: subscriptions) {
                		if (subscription.getTarget().equalsIgnoreCase(PLAY_STATE)) {
    						for (int i = 0; i < subscription.getListeners().size(); i++) {
    							@SuppressWarnings("unchecked")
    							ResponseListener<Object> listener = (ResponseListener<Object>) subscription.getListeners().get(i);
    							PlayStateStatus status = convertPlayerStateToPlayStateStatus(mMediaPlayer.getMediaStatus().getPlayerState());
    							Util.postSuccess(listener, status);
    						}
                		}
                	}
                }
            }
        });

        mMediaPlayer.setOnMetadataUpdatedListener(new RemoteMediaPlayer.OnMetadataUpdatedListener() {
            @Override
            public void onMetadataUpdated() {
                Log.d("Connect SDK", "MediaControlChannel.onMetadataUpdated");
            }
        });
        
        try {
            Cast.CastApi.setMessageReceivedCallbacks(mApiClient, mMediaPlayer.getNamespace(),
                    mMediaPlayer);
        } catch (IOException e) {
            Log.w("Connect SDK", "Exception while creating media channel", e);
        }
    }
	
    private void detachMediaPlayer() {
        if (mMediaPlayer != null) {
            try {
                Cast.CastApi.removeMessageReceivedCallbacks(mApiClient,
                        mMediaPlayer.getNamespace());
            } catch (IOException e) {
                Log.w("Connect SDK", "Exception while launching application", e);
            }
            mMediaPlayer = null;
        }
    }
	
    @Override
	public void displayImage(String url, String mimeType, String title,
			String description, String iconSrc, LaunchListener listener) {
		MediaMetadata mMediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_PHOTO);
		mMediaMetadata.putString(MediaMetadata.KEY_TITLE, title);
		mMediaMetadata.putString(MediaMetadata.KEY_SUBTITLE, description);

		if (iconSrc != null) {
			Uri iconUri = Uri.parse(iconSrc);
			WebImage image = new WebImage(iconUri, 100, 100);
			mMediaMetadata.addImage(image);
		}
		
		com.google.android.gms.cast.MediaInfo mediaInformation = new com.google.android.gms.cast.MediaInfo.Builder(url)
			    .setContentType(mimeType)
			    .setStreamType(com.google.android.gms.cast.MediaInfo.STREAM_TYPE_NONE)
			    .setMetadata(mMediaMetadata)
			    .setStreamDuration(0)
			    .setCustomData(null)
			    .build();

		playMedia(mediaInformation, CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID, listener);
    }
    
    @Override
	public void displayImage(MediaInfo mediaInfo, LaunchListener listener) {
    	ImageInfo imageInfo = mediaInfo.getImages().get(0);
    	String iconSrc = imageInfo.getUrl();
    	
		displayImage(mediaInfo.getUrl(), mediaInfo.getMimeType(), mediaInfo.getTitle(), mediaInfo.getDescription(), iconSrc, listener);
	}
    
	@Override
	public void playMedia(String url, String mimeType, String title,
			String description, String iconSrc, boolean shouldLoop,
			LaunchListener listener) {
		MediaMetadata mMediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
		mMediaMetadata.putString(MediaMetadata.KEY_TITLE, title);
		mMediaMetadata.putString(MediaMetadata.KEY_SUBTITLE, description);

		if (iconSrc != null) {
			Uri iconUri = Uri.parse(iconSrc);
			WebImage image = new WebImage(iconUri, 100, 100);
			mMediaMetadata.addImage(image);
		}

		com.google.android.gms.cast.MediaInfo mediaInformation = new com.google.android.gms.cast.MediaInfo.Builder(url)
			    .setContentType(mimeType)
			    .setStreamType(com.google.android.gms.cast.MediaInfo.STREAM_TYPE_BUFFERED)
			    .setMetadata(mMediaMetadata)
			    .setStreamDuration(1000)
			    .setCustomData(null)
			    .build();
		
		playMedia(mediaInformation, CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID, listener);
	}

	@Override
	public void playMedia(MediaInfo mediaInfo, boolean shouldLoop, LaunchListener listener) {
    	ImageInfo imageInfo = mediaInfo.getImages().get(0);
    	String iconSrc = imageInfo.getUrl();
    	
    	playMedia(mediaInfo.getUrl(), mediaInfo.getMimeType(), mediaInfo.getTitle(), mediaInfo.getDescription(), iconSrc, shouldLoop, listener);
	}
	
	private void playMedia(final com.google.android.gms.cast.MediaInfo mediaInformation, String mediaAppId, final LaunchListener listener) {
		ApplicationConnectionResultCallback webAppLaunchCallback = new ApplicationConnectionResultCallback(new LaunchWebAppListener() {
			
			@Override
			public void onSuccess(final WebAppSession webAppSession) {
		        mMediaPlayer.load(mApiClient, mediaInformation, true).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
					
					@Override
					public void onResult(MediaChannelResult result) {
						Status status = result.getStatus();
						
						if (status.isSuccess()) {
							webAppSession.launchSession.setSessionType(LaunchSessionType.Media);
							
							Util.postSuccess(listener, new MediaLaunchObject(webAppSession.launchSession, CastService.this));
						}
						else {
							Util.postError(listener, new ServiceCommandError(status.getStatusCode(), status.getStatusMessage(), status));
						}
					}
				});
			}
			
			@Override
			public void onFailure(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		});
		
		launchingAppId = mediaAppId;

		Cast.CastApi.launchApplication(mApiClient, mediaAppId,false).setResultCallback(webAppLaunchCallback);
	}

	@Override
	public void closeMedia(final LaunchSession launchSession, final ResponseListener<Object> listener) {
		Cast.CastApi.stopApplication(mApiClient, launchSession.getSessionId()).setResultCallback(new ResultCallback<Status>() {
					
			@Override
			public void onResult(Status result) {
				if (result.isSuccess()) {
					Util.postSuccess(listener, result);
				} else {
					Util.postError(listener, new ServiceCommandError(result.getStatusCode(), result.getStatusMessage(), result));
				}
			}
		});
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
		launchWebApp(webAppId, true, listener);
	}
	
	@Override
	public void launchWebApp(final String webAppId, final boolean relaunchIfRunning, final WebAppSession.LaunchListener listener) {
		launchingAppId = webAppId;
		
		Cast.CastApi.launchApplication(mApiClient, webAppId, relaunchIfRunning).setResultCallback(
				new ApplicationConnectionResultCallback(new LaunchWebAppListener() {
					
					@Override
					public void onSuccess(WebAppSession webAppSession) {
						Util.postSuccess(listener, webAppSession);
					}
					
					@Override
					public void onFailure(ServiceCommandError error) {
						Util.postError(listener, error);
					}
				})
		);
	}
	
	@Override
	public void launchWebApp(String webAppId, JSONObject params, WebAppSession.LaunchListener listener) {
		Util.postError(listener, ServiceCommandError.notSupported());
	}
	
	@Override
	public void launchWebApp(String webAppId, JSONObject params, boolean relaunchIfRunning, WebAppSession.LaunchListener listener) {
		Util.postError(listener, ServiceCommandError.notSupported());
	}
	
	@Override
	public void joinWebApp(final LaunchSession webAppLaunchSession, final WebAppSession.LaunchListener listener) {
		ApplicationConnectionResultCallback webAppLaunchCallback = new ApplicationConnectionResultCallback(new LaunchWebAppListener() {
			
			@Override
			public void onSuccess(final WebAppSession webAppSession) {
				webAppSession.connect(new ResponseListener<Object>() {
					
					@Override
					public void onSuccess(Object object) {
						Util.postSuccess(listener, webAppSession);
					}
					
					@Override
					public void onError(ServiceCommandError error) {
						Util.postError(listener, error);
					}
				});
			}
			
			@Override
			public void onFailure(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		});

		launchingAppId = webAppLaunchSession.getAppId();

		Cast.CastApi.joinApplication(mApiClient, webAppLaunchSession.getAppId()).setResultCallback(webAppLaunchCallback);
	}
	
	@Override
	public void joinWebApp(String webAppId, WebAppSession.LaunchListener listener) {
		LaunchSession launchSession = LaunchSession.launchSessionForAppId(webAppId);
		launchSession.setSessionType(LaunchSessionType.WebApp);
		launchSession.setService(this);
		
		joinWebApp(launchSession, listener);
	}

	@Override
	public void closeWebApp(LaunchSession launchSession, final ResponseListener<Object> listener) {
		Cast.CastApi.stopApplication(mApiClient).setResultCallback(new ResultCallback<Status>() {

			@Override
			public void onResult(Status status) {
				if (status.isSuccess()) {
					Util.postSuccess(listener, null);
				}
				else {
					Util.postError(listener, new ServiceCommandError(status.getStatusCode(), status.getStatusMessage(), status));
				}
			}
		});
	}
	
	@Override
	public VolumeControl getVolumeControl() {
		return this;
	}

	@Override
	public CapabilityPriorityLevel getVolumeControlCapabilityLevel() {
		return CapabilityPriorityLevel.HIGH;
	}

	@Override
	public void volumeUp(final ResponseListener<Object> listener) {
		getVolume(new VolumeListener() {
			
			@Override
			public void onSuccess(final Float volume) {
				if (volume >= 1.0) {
					Util.postSuccess(listener, null);
				}
				else {
					float newVolume = (float)(volume + 0.01);
					
					if (newVolume > 1.0)
						newVolume = (float)1.0;

					setVolume(newVolume, listener);
					
					Util.postSuccess(listener, null);
				}
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		});
	}

	@Override
	public void volumeDown(final ResponseListener<Object> listener) {
		getVolume(new VolumeListener() {
			
			@Override
			public void onSuccess(final Float volume) {
				if (volume <= 0.0) {
					Util.postSuccess(listener, null);
				}
				else {
					float newVolume = (float)(volume - 0.01);
					
					if (newVolume < 0.0)
						newVolume = (float)0.0;

					setVolume(newVolume, listener);
					
					Util.postSuccess(listener, null);
				}
			}
			
			@Override
			public void onError(ServiceCommandError error) {
				Util.postError(listener, error);
			}
		});
	}

	@Override
	public void setVolume(float volume, ResponseListener<Object> listener) {
		try {
			Cast.CastApi.setVolume(mApiClient, volume);
			
			Util.postSuccess(listener, null);
		} catch (IOException e) {
			Util.postError(listener, new ServiceCommandError(0, "setting volume level failed", null));
		}
	}

	@Override
	public void getVolume(VolumeListener listener) {
		Util.postSuccess(listener, currentVolumeLevel);
	}

	@Override
	public void setMute(boolean isMute, ResponseListener<Object> listener) {
        try {
			Cast.CastApi.setMute(mApiClient, isMute);
			
			Util.postSuccess(listener, null);
		} catch (IOException e) {
			Util.postError(listener, new ServiceCommandError(0, "setting mute status failed", null));
		}
	}

	@Override
	public void getMute(final MuteListener listener) {
		Util.postSuccess(listener, currentMuteStatus);
	}

	@Override
	public ServiceSubscription<VolumeListener> subscribeVolume(VolumeListener listener) {
		URLServiceSubscription<VolumeListener> request = new URLServiceSubscription<VolumeListener>(this, CAST_SERVICE_VOLUME_SUBSCRIPTION_NAME, null, null);
		request.addListener(listener);
		addSubscription(request);

		return request;
	}

	@Override
	public ServiceSubscription<MuteListener> subscribeMute(MuteListener listener) {
		URLServiceSubscription<MuteListener> request = new URLServiceSubscription<MuteListener>(this, CAST_SERVICE_MUTE_SUBSCRIPTION_NAME, null, null);
		request.addListener(listener);
		addSubscription(request);

		return request;
	}
	
	@Override
	protected void updateCapabilities() {
		List<String> capabilities = new ArrayList<String>();
		
		for (String capability : MediaPlayer.Capabilities) { capabilities.add(capability); }
		for (String capability : VolumeControl.Capabilities) { capabilities.add(capability); }
		
		capabilities.add(Play);
		capabilities.add(Pause);
		capabilities.add(Stop);
		capabilities.add(Duration);
		capabilities.add(Seek);
		capabilities.add(Position);
		capabilities.add(PlayState);
		capabilities.add(PlayState_Subscribe);

		capabilities.add(WebAppLauncher.Launch);
		capabilities.add(Message_Send);
		capabilities.add(Message_Receive);
		capabilities.add(Message_Send_JSON);
		capabilities.add(Message_Receive_JSON);
		capabilities.add(WebAppLauncher.Connect);
		capabilities.add(WebAppLauncher.Disconnect);
		capabilities.add(WebAppLauncher.Join);
		capabilities.add(WebAppLauncher.Close);
		
		setCapabilities(capabilities);
	}
	
    private class CastListener extends Cast.Listener {
        @Override
        public void onApplicationDisconnected(int statusCode) {
            Log.d("Connect SDK", "Cast.Listener.onApplicationDisconnected: " + statusCode);
            
            if (currentAppId == null)
            	return;
            
            CastWebAppSession webAppSession = sessions.get(currentAppId);

            if (webAppSession == null)
            	return;

            webAppSession.handleAppClose();
        }

		@Override
		public void onApplicationStatusChanged() {
			ApplicationMetadata applicationMetadata = Cast.CastApi.getApplicationMetadata(mApiClient);

			if (applicationMetadata != null)
				currentAppId = applicationMetadata.getApplicationId();
		}

		@Override
		public void onVolumeChanged() {
	        try {
	        	currentVolumeLevel = (float) Cast.CastApi.getVolume(mApiClient);
	        	currentMuteStatus = Cast.CastApi.isMute(mApiClient);
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}
			
            if (subscriptions.size() > 0) {
            	for (URLServiceSubscription<?> subscription: subscriptions) {
            		if (subscription.getTarget().equals(CAST_SERVICE_VOLUME_SUBSCRIPTION_NAME)) {
						for (int i = 0; i < subscription.getListeners().size(); i++) {
							@SuppressWarnings("unchecked")
							ResponseListener<Object> listener = (ResponseListener<Object>) subscription.getListeners().get(i);
							
							Util.postSuccess(listener, currentVolumeLevel);
						}
            		}
            		else if (subscription.getTarget().equals(CAST_SERVICE_MUTE_SUBSCRIPTION_NAME)) {
						for (int i = 0; i < subscription.getListeners().size(); i++) {
							@SuppressWarnings("unchecked")
							ResponseListener<Object> listener = (ResponseListener<Object>) subscription.getListeners().get(i);
							
							Util.postSuccess(listener, currentMuteStatus);
						}
            		}
            	}
            }
		}
    }
    
    private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnectionSuspended(final int cause) {
            Log.d("Connect SDK", "ConnectionCallbacks.onConnectionSuspended");
            
            disconnect();
            detachMediaPlayer();
            
            Util.runOnUI(new Runnable() {
				@Override
				public void run() {
					if (listener != null) {
						ServiceCommandError error;
			            
			            switch (cause) {
			            	case GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST:
			            		error = new ServiceCommandError(cause, "Peer device connection was lost", null);
			            		break;
			            	
			            	case GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED:
			            		error = new ServiceCommandError(cause, "The service has been killed", null);
			            		break;
			            	
			            	default:
			            		error = new ServiceCommandError(cause, "Unknown connection error", null);
			            }
			            
						listener.onDisconnect(CastService.this, error);
					}
				}
			});
        }

        @Override
        public void onConnected(Bundle connectionHint) {
            Log.d("Connect SDK", "ConnectionCallbacks.onConnected");

            attachMediaPlayer();
            
    		connected = true;

    		reportConnected(true);
        }
    }

    private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(final ConnectionResult result) {
            Log.d("Connect SDK", "ConnectionFailedListener.onConnectionFailed");
            
            detachMediaPlayer();
			connected = false;
            
            Util.runOnUI(new Runnable() {
				
				@Override
				public void run() {
					if (listener != null) {
						ServiceCommandError error = new ServiceCommandError(result.getErrorCode(), "Failed to connect to Google Cast device", result);
						
						listener.onConnectionFailure(CastService.this, error);
					}
				}
			});
        }
    }
    
    private class ApplicationConnectionResultCallback implements
    		ResultCallback<Cast.ApplicationConnectionResult> {
    	LaunchWebAppListener listener;
    	
    	public ApplicationConnectionResultCallback(LaunchWebAppListener listener) {
    		this.listener = listener;
    	}

    	@Override
    	public void onResult(ApplicationConnectionResult result) {
    		Status status = result.getStatus();
    		
    		if (status.isSuccess()) {
        		ApplicationMetadata applicationMetadata = result.getApplicationMetadata();
        		currentAppId = applicationMetadata.getApplicationId();

        		LaunchSession launchSession = LaunchSession.launchSessionForAppId(applicationMetadata.getApplicationId());
        		launchSession.setAppName(applicationMetadata.getName());
        		launchSession.setSessionId(result.getSessionId());
        	    launchSession.setSessionType(LaunchSessionType.WebApp);
        	    launchSession.setService(CastService.this);

        	    CastWebAppSession webAppSession = new CastWebAppSession(launchSession, CastService.this);
        	    webAppSession.setMetadata(applicationMetadata);
        	    
        	    sessions.put(applicationMetadata.getApplicationId(), webAppSession);
        	    
        	    if (listener != null) {
        	    	listener.onSuccess(webAppSession);
//        	    	Util.postSuccess(listener, webAppSession);
        	    }

        	    launchingAppId = null;
    		}
    		else {
    			if (listener != null) {
    				listener.onFailure(new ServiceCommandError(status.getStatusCode(), status.getStatusMessage(), status));
//    				Util.postError(listener, new ServiceCommandError(status.getStatusCode(), status.getStatusMessage(), status));
    			}
    		}
    	}
    }
    
    @Override
    public void getPlayState(PlayStateListener listener) {
		if (mMediaPlayer == null) {
			Util.postError(listener, new ServiceCommandError(0, "Unable to get play state", null));
			return;
		}
		
		PlayStateStatus status = convertPlayerStateToPlayStateStatus(mMediaPlayer.getMediaStatus().getPlayerState());
		Util.postSuccess(listener, status);
    }
    
    private PlayStateStatus convertPlayerStateToPlayStateStatus(int playerState) {
		PlayStateStatus status = PlayStateStatus.Unknown;
		
		switch (playerState) {
    		case MediaStatus.PLAYER_STATE_BUFFERING:
    			status = PlayStateStatus.Buffering;
    			break;
    		case MediaStatus.PLAYER_STATE_IDLE:
    			status = PlayStateStatus.Idle;
    			break;
    		case MediaStatus.PLAYER_STATE_PAUSED:
    			status = PlayStateStatus.Paused;
    			break;
    		case MediaStatus.PLAYER_STATE_PLAYING:
    			status = PlayStateStatus.Playing;
    			break;
    		case MediaStatus.PLAYER_STATE_UNKNOWN:
    		default:
    			status = PlayStateStatus.Unknown;
    			break;
		}
		
		return status;
    }
    
    public GoogleApiClient getApiClient() {
    	return mApiClient;
    }
    
    //////////////////////////////////////////////////
    //		Device Service Methods
    //////////////////////////////////////////////////
    @Override
    public boolean isConnectable() {
    	return true;
    }
    
	@Override
	public boolean isConnected() {
		return connected;
	}

	@Override
	public ServiceSubscription<PlayStateListener> subscribePlayState(PlayStateListener listener) {
		URLServiceSubscription<PlayStateListener> request = new URLServiceSubscription<PlayStateListener>(this, PLAY_STATE, null, null);
		request.addListener(listener);
		addSubscription(request);

		return request;
		
	}
	
	private void addSubscription(URLServiceSubscription<?> subscription) {
		subscriptions.add(subscription);
	}
	
	@Override
	public void unsubscribe(URLServiceSubscription<?> subscription) {
		subscriptions.remove(subscription);
	}
	
	public List<URLServiceSubscription<?>> getSubscriptions() {
		return subscriptions;
	}

	public void setSubscriptions(List<URLServiceSubscription<?>> subscriptions) {
		this.subscriptions = subscriptions;
	}
}
