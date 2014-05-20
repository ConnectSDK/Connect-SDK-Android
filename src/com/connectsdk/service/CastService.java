/*
 * CastService
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on Feb 23 2014
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
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.VolumeControl;
import com.connectsdk.service.capability.WebAppLauncher;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.NotSupportedServiceSubscription;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
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
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.cast.RemoteMediaPlayer.MediaChannelResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.images.WebImage;

public class CastService extends DeviceService implements MediaPlayer, MediaControl, VolumeControl, WebAppLauncher {
	
	public static final String ID = "Chromecast";

	public final static String TAG = "Connect SDK";

	GoogleApiClient mApiClient;
    Cast.Listener mCastClientListener;
    ConnectionCallbacks mConnectionCallbacks;
    ConnectionFailedListener mConnectionFailedListener;
    
    CastDevice castDevice;
    RemoteMediaPlayer mMediaPlayer;
    
    boolean isConnected = false;
    
    protected static final double VOLUME_INCREMENT = 0.05;
    
	public CastService(ServiceDescription serviceDescription, ServiceConfig serviceConfig) {
		super(serviceDescription, serviceConfig);
		
		mCastClientListener = new CastListener();
        mConnectionCallbacks = new ConnectionCallbacks();
        mConnectionFailedListener = new ConnectionFailedListener();
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
        mApiClient.connect();
	}

	@Override
	public void disconnect() {
		mApiClient.disconnect();
		isConnected = false;
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
		if (mMediaPlayer == null) {
			return;
		}
	    
		// TODO handle responselistener callback
		try {
			mMediaPlayer.play(mApiClient);
		} catch (Exception e) {
			// NOTE: older versions of Play Services required a check for IOException
			Log.w("Connect SDK", "Unable to play", e);
		}
	}

	@Override
	public void pause(ResponseListener<Object> listener) {
        if (mMediaPlayer == null) {
            return;
        }
        
		// TODO handle responselistener callback
        try {
			mMediaPlayer.pause(mApiClient);
		} catch (Exception e) {
			// NOTE: older versions of Play Services required a check for IOException
            Log.w("Connect SDK", "Unable to pause", e);
		}
	}

	@Override
	public void stop(ResponseListener<Object> listener) {
		if (mMediaPlayer == null) {
			return;
		}

		// TODO handle responselistener callback
		try {
			mMediaPlayer.stop(mApiClient);
		} catch (Exception e) {
			// NOTE: older versions of Play Services required a check for IOException
			Log.w("Connect SDK", "Unable to stop");
		}
	}

	@Override
	public void rewind(ResponseListener<Object> listener) {
		if (listener != null) 
			Util.postError(listener, ServiceCommandError.notSupported());
	}

	@Override
	public void fastForward(ResponseListener<Object> listener) {
		if (listener != null) 
			Util.postError(listener, ServiceCommandError.notSupported());
	}
	
	@Override
	public void seek(long position, final ResponseListener<Object> listener) {
		int resumeState = RemoteMediaPlayer.RESUME_STATE_UNCHANGED;
		
        mMediaPlayer.seek(mApiClient, position, resumeState).setResultCallback(
                new ResultCallback<MediaChannelResult>() {

                	@Override
                    public void onResult(MediaChannelResult result) {
                        Status status = result.getStatus();
                        if (status.isSuccess()) {
                        	Log.d("Connect SDK", "Seek Successfull");
                        	Util.postSuccess(listener, result);
                        } else {
                            Log.w("Connect SDK", "Unable to seek: " + status.getStatusCode());
                            Util.postError(listener, new ServiceCommandError(status.getStatusCode(), status.toString(), status));
                        }
                    }

                });
	}

	@Override
	public void getDuration(DurationListener listener) {
		if (mMediaPlayer == null) {
			Util.postError(listener, new ServiceCommandError(0, "Unable to get duration", null));
		}
		else {
			Util.postSuccess(listener, mMediaPlayer.getStreamDuration());
		}
	}
	
	@Override
	public void getPosition(PositionListener listener) {
		if (mMediaPlayer == null) {
			Util.postError(listener, new ServiceCommandError(0, "Unable to get position", null));
		}
		else {
			Util.postSuccess(listener, mMediaPlayer.getApproximateStreamPosition());
		}
	}
	
	
	@Override
	public MediaPlayer getMediaPlayer() {
		return this;
	}

	@Override
	public CapabilityPriorityLevel getMediaPlayerCapabilityLevel() {
		return CapabilityPriorityLevel.NORMAL;
	}
	
	private void attachMediaPlayer() {
        if (mMediaPlayer != null) {
            return;
        }

        mMediaPlayer = new RemoteMediaPlayer();
        mMediaPlayer.setOnStatusUpdatedListener(new RemoteMediaPlayer.OnStatusUpdatedListener() {

            @Override
            public void onStatusUpdated() {
                Log.d("Connect SDK", "MediaControlChannel.onStatusUpdated");
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
	
	@SuppressWarnings("unused")
	private void reattachMediaPlayer() {
        if ((mMediaPlayer != null) && (mApiClient != null)) {
            try {
                Cast.CastApi.setMessageReceivedCallbacks(mApiClient, mMediaPlayer.getNamespace(),
                        mMediaPlayer);
            } catch (IOException e) {
                Log.w("Connect SDK", "Exception while launching application", e);
            }
        }
    }

    private void detachMediaPlayer() {
        if ((mMediaPlayer != null) && (mApiClient != null)) {
            try {
                Cast.CastApi.removeMessageReceivedCallbacks(mApiClient,
                        mMediaPlayer.getNamespace());
            } catch (IOException e) {
                Log.w("Connect SDK", "Exception while launching application", e);
            }
        }
        mMediaPlayer = null;
    }
	
	private void playMedia(MediaInfo media, final LaunchListener listener) {
        if (media == null) {
        	Util.postError(listener, new ServiceCommandError(500, "MediaInfo is null", null));
            return;
        }
        
        if (mMediaPlayer == null) {
        	Util.postError(listener, new ServiceCommandError(500, "Trying to play a video with no active media session", null));
            return;
        }

        if (mApiClient == null) {
        	Util.postError(listener, new ServiceCommandError(500, "GoogleApiClient is null", null));
            return;
        }

        if (!mApiClient.isConnected()) {
			Util.postError(listener, new ServiceCommandError(-1, "The Google Cast API Client is not connected", null));
			return;
		}
		
		mMediaPlayer.load(mApiClient, media, true).setResultCallback(
    			new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {

    				@Override
    				public void onResult(MediaChannelResult result) {
    					if (result.getStatus().isSuccess()) {
    						LaunchSession launchSession = LaunchSession.launchSessionForAppId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID);
    						launchSession.setService(CastService.this);
    						launchSession.setSessionType(LaunchSessionType.Media);

    						Util.postSuccess(listener, new MediaLaunchObject(launchSession, CastService.this));
    					} else {
    			        	Util.postError(listener, new ServiceCommandError(result.getStatus().getStatusCode(), result.getStatus().toString(), result));
    					}
    				}
    			});
    }

	@Override
	public void displayImage(String url, String mimeType, String title,
			String description, String iconSrc, LaunchListener listener) {
		
		if (!mApiClient.isConnected()) {
			Util.postError(listener, new ServiceCommandError(-1, "The Google Cast API Client is not connected", null));
			return;
		}
		
		MediaMetadata mMediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_PHOTO);
		mMediaMetadata.putString(MediaMetadata.KEY_TITLE, title);
		mMediaMetadata.putString(MediaMetadata.KEY_SUBTITLE, description);

		if (iconSrc != null) {
			Uri iconUri = Uri.parse(iconSrc);
			WebImage image = new WebImage(iconUri, 100, 100);
			mMediaMetadata.addImage(image);
		}
		
		MediaInfo mediaInfo = new MediaInfo.Builder(url)
			    .setContentType(mimeType)
			    .setStreamType(MediaInfo.STREAM_TYPE_NONE)
			    .setMetadata(mMediaMetadata)
			              .build();

        Cast.CastApi.launchApplication(mApiClient, CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID, false)
    		.setResultCallback(new ApplicationConnectionResultCallback(mediaInfo, listener));
	}

	@Override
	public void playMedia(String url, String mimeType, String title,
			String description, String iconSrc, boolean shouldLoop,
			LaunchListener listener) {

		if (!mApiClient.isConnected()) {
			Util.postError(listener, new ServiceCommandError(-1, "The Google Cast API Client is not connected", null));
			return;
		}
		
		MediaMetadata mMediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
		mMediaMetadata.putString(MediaMetadata.KEY_TITLE, title);
		mMediaMetadata.putString(MediaMetadata.KEY_SUBTITLE, description);

		if (iconSrc != null) {
			Uri iconUri = Uri.parse(iconSrc);
			WebImage image = new WebImage(iconUri, 100, 100);
			mMediaMetadata.addImage(image);
		}

		MediaInfo mediaInfo = new MediaInfo.Builder(url)
			    .setContentType(mimeType)
			    .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
			    .setMetadata(mMediaMetadata)
			              .build();

        Cast.CastApi.launchApplication(mApiClient, CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID, false)
    		.setResultCallback(new ApplicationConnectionResultCallback(mediaInfo, listener));
	}

	@Override
	public void closeMedia(final LaunchSession launchSession, final ResponseListener<Object> listener) {
		if (!mApiClient.isConnected()) {
			Util.postError(listener, new ServiceCommandError(-1, "The Google Cast API Client is not connected", null));
			return;
		}
		
		Cast.CastApi.stopApplication(mApiClient).setResultCallback(new ResultCallback<Status>() {
			
			@Override
			public void onResult(Status result) {
				if (result.isSuccess()) {
					((CastService) launchSession.getService()).detachMediaPlayer();
					
					Util.postSuccess(listener, result);
				} else {
					Util.postError(listener, new ServiceCommandError(result.getStatusCode(), result.getStatus().toString(), result));
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
		return CapabilityPriorityLevel.NORMAL;
	}

	@Override
	public void launchWebApp(final String webAppId, final WebAppSession.LaunchListener listener) {
//		Cast.CastApi.stopApplication(mApiClient).setResultCallback(new ResultCallback<Status>() {
//			
//			@Override
//			public void onResult(Status result) {
//				if (result.isSuccess()) {
//				}
//			}
//		});
		
		launchWebApp(webAppId, true, listener);
	}
	
	@Override
	public void joinWebApp(final LaunchSession webAppLaunchSession, final WebAppSession.LaunchListener listener) {
		if (!mApiClient.isConnected()) {
			Util.postError(listener, new ServiceCommandError(-1, "The Google Cast API Client is not connected", null));
			return;
		}
		
		ResultCallback<Cast.ApplicationConnectionResult> resultCallback = new ResultCallback<Cast.ApplicationConnectionResult>() {

			@Override
			public void onResult(Cast.ApplicationConnectionResult result) {
				Status status = result.getStatus();

				if (status.isSuccess()) {
					final CastWebAppSession webAppSession = new CastWebAppSession(webAppLaunchSession, CastService.this);
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
				else {
					Util.postError(listener, new ServiceCommandError(result.getStatus().getStatusCode(), result.getStatus().toString(), result));
				}
			}
		};
		
		Cast.CastApi.joinApplication(mApiClient, webAppLaunchSession.getAppId(), webAppLaunchSession.getSessionId()).setResultCallback(resultCallback);
	}
	
	@Override
	public void joinWebApp(String webAppId, WebAppSession.LaunchListener listener) {
		LaunchSession launchSession = LaunchSession.launchSessionForAppId(webAppId);
		launchSession.setSessionType(LaunchSessionType.WebApp);
		launchSession.setService(this);
		
		joinWebApp(launchSession, listener);
	}

	@Override
	public void launchWebApp(String webAppId, JSONObject params, WebAppSession.LaunchListener listener) {
		launchWebApp(webAppId, true, listener);
	}
	
	@Override
	public void launchWebApp(final String webAppId, boolean relaunchIfRunning, final WebAppSession.LaunchListener listener) {
		Log.d(TAG, "CastService::launchWebApp() | webAppId = " + webAppId);
		
		if (!mApiClient.isConnected()) {
			Util.postError(listener, new ServiceCommandError(-1, "The Google Cast API Client is not connected", null));
			return;
		}
		
		Cast.CastApi.launchApplication(mApiClient, webAppId, relaunchIfRunning)
		.setResultCallback(
				new ResultCallback<Cast.ApplicationConnectionResult>() {

					@Override
					public void onResult(Cast.ApplicationConnectionResult result) {
						Status status = result.getStatus();

						if (status.isSuccess()) {
    						LaunchSession launchSession = LaunchSession.launchSessionForAppId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID);
    						launchSession.setService(CastService.this);
    						launchSession.setSessionType(LaunchSessionType.Media);

    						Util.postSuccess(listener, new CastWebAppSession(launchSession, CastService.this));
						}
						else {
							Util.postError(listener, new ServiceCommandError(result.getStatus().getStatusCode(), result.getStatus().toString(), result));
						}
					}
				});
	}
	
	@Override
	public void launchWebApp(String webAppId, JSONObject params, boolean relaunchIfRunning, WebAppSession.LaunchListener listener) {
		launchWebApp(webAppId, relaunchIfRunning, listener);
	}
	
	@Override
	public void closeWebApp(LaunchSession launchSession, final ResponseListener<Object> listener) {
		if (!mApiClient.isConnected()) {
			Util.postError(listener, new ServiceCommandError(-1, "The Google Cast API Client is not connected", null));
			return;
		}
		
		ResultCallback<Status> resultCallback = new ResultCallback<Status>() {
			@Override
			public void onResult(final Status result) {
				if (result.isSuccess())
					Util.postSuccess(listener, null);
				else
					Util.postError(listener, new ServiceCommandError(0, "TV Error", null));
			}
		};
		
		Cast.CastApi.stopApplication(mApiClient).setResultCallback(resultCallback);
	}
	
	
	@Override
	public VolumeControl getVolumeControl() {
		return this;
	}

	@Override
	public CapabilityPriorityLevel getVolumeControlCapabilityLevel() {
		return CapabilityPriorityLevel.NORMAL;
	}

	@Override
	public void volumeUp(final ResponseListener<Object> listener) {
		getVolume(new VolumeListener() {
			
			@Override
			public void onSuccess(Float volume) {
		        try {
		        	float newVolume; 
		        	if (volume + VOLUME_INCREMENT >= 1.0) {
		        		newVolume = 1;
		        	}
		        	else {
		        		newVolume = (float) (volume + VOLUME_INCREMENT);
		        	}
		        	
					Cast.CastApi.setVolume(mApiClient, newVolume);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
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
			public void onSuccess(Float volume) {
		        try {
		        	float newVolume; 
		        	if (volume - VOLUME_INCREMENT <= 0) {
		        		newVolume = 0;
		        	}
		        	else {
		        		newVolume = (float) (volume - VOLUME_INCREMENT);
		        	}
		        	
					Cast.CastApi.setVolume(mApiClient, newVolume);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
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
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}			
	}

	@Override
	public void getVolume(VolumeListener listener) {
        float volume = (float) Cast.CastApi.getVolume(mApiClient);
        Util.postSuccess(listener, volume);
	}

	@Override
	public void setMute(boolean isMute, ResponseListener<Object> listener) {
        try {
			Cast.CastApi.setMute(mApiClient, isMute);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void getMute(MuteListener listener) {
		boolean isMute = Cast.CastApi.isMute(mApiClient);
		
		Util.postSuccess(listener, isMute);
	}

	@Override
	public ServiceSubscription<VolumeListener> subscribeVolume(VolumeListener listener) {
		Util.postError(listener, ServiceCommandError.notSupported());
		return new NotSupportedServiceSubscription<VolumeControl.VolumeListener>();
	}

	@Override
	public ServiceSubscription<MuteListener> subscribeMute(MuteListener listener) {
		Util.postError(listener, ServiceCommandError.notSupported());
		return new NotSupportedServiceSubscription<MuteListener>();
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
		capabilities.add(WebAppLauncher.Close);
		
		setCapabilities(capabilities);
	}
	
    private class CastListener extends Cast.Listener {
        @Override
        public void onApplicationDisconnected(int statusCode) {
            Log.d("Connect SDK", "Cast.Listener.onApplicationDisconnected: " + statusCode);
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
            isConnected = true;
 
            reportConnected(true);
        }
    }

    private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(final ConnectionResult result) {
            Log.d("Connect SDK", "ConnectionFailedListener.onConnectionFailed");
            
            detachMediaPlayer();
            isConnected = false;
            
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
    
    private final class ApplicationConnectionResultCallback implements
    		ResultCallback<Cast.ApplicationConnectionResult> {
    	
    	MediaInfo mediaInfo;
    	LaunchListener listener;
    	
    	public ApplicationConnectionResultCallback(MediaInfo mediaInfo, LaunchListener listener) {
    		this.mediaInfo = mediaInfo;
    		this.listener = listener;
    	}

    	@Override
    	public void onResult(ApplicationConnectionResult result) {
    		Status status = result.getStatus();
    		Log.d("Connect SDK", "ApplicationConnectionResultCallback.onResult: statusCode: " + status.getStatusCode());
    
    		if (status.isSuccess()) {
    			ApplicationMetadata applicationMetadata = result.getApplicationMetadata();
    			String sessionId = result.getSessionId();
    			String applicationStatus = result.getApplicationStatus();
    			boolean wasLaunched = result.getWasLaunched();
    			Log.d("Connect SDK", "application name: " + applicationMetadata.getName()
    					+ ", status: " + applicationStatus + ", sessionId: " + sessionId
    					+ ", wasLaunched: " + wasLaunched);

    			attachMediaPlayer();
    			playMedia(mediaInfo, listener);
    		} else {
    			Util.postError(listener, new ServiceCommandError(status.getStatusCode(), status.getStatus().toString(), status));
    		}
    	}
    }
    
    public CastDevice getDevice() {
    	return castDevice;
    }
    
    @Override
    public void getPlayState(PlayStateListener listener) {
    	Util.postError(listener, ServiceCommandError.notSupported());
    }
    
    public GoogleApiClient getApiClient() {
    	return mApiClient;
    }
    
    @Override
    public void setServiceDescription(ServiceDescription serviceDescription) {
    	super.setServiceDescription(serviceDescription);
		
		if (serviceDescription instanceof CastServiceDescription)
			this.castDevice = ((CastServiceDescription)serviceDescription).getCastDevice();
		
        if (this.castDevice != null) {
        	Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                      	.builder(castDevice, mCastClientListener);

        	mApiClient = new GoogleApiClient.Builder(DiscoveryManager.getInstance().getContext())
                              	.addApi(Cast.API, apiOptionsBuilder.build())
                              	.addConnectionCallbacks(mConnectionCallbacks)
                              	.addOnConnectionFailedListener(mConnectionFailedListener)
                              	.build();
        }
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
    	return isConnected;
    }

	@Override
	public ServiceSubscription<PlayStateListener> subscribePlayState(PlayStateListener listener) {
		Util.postError(listener, ServiceCommandError.notSupported());
		return null;
	}
}
