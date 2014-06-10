package com.connectsdk.service.sessions;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.connectsdk.core.Util;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.MultiScreenService;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.samsung.multiscreen.channel.Channel;
import com.samsung.multiscreen.channel.ChannelAsyncResult;
import com.samsung.multiscreen.channel.ChannelClient;
import com.samsung.multiscreen.channel.ChannelError;
import com.samsung.multiscreen.channel.IChannelListener;
import com.samsung.multiscreen.device.Device;
import com.samsung.multiscreen.device.DeviceAsyncResult;
import com.samsung.multiscreen.device.DeviceError;

public class MultiScreenWebAppSession extends WebAppSession {
	protected MultiScreenService service;

	private final String channelId = "urn:x-cast:com.connectsdk";
	private Channel mChannel;
	private IChannelListener channelListener = new IChannelListener() {
		
		@Override
		public void onDisconnect() {
			System.out.println("[DEBUG] onDisconnect");
			
		}
		
		@Override
		public void onConnect() {
			System.out.println("[DEBUG] onConnect");
			
		}
		
		@Override
		public void onClientMessage(ChannelClient client, final String message) {
			System.out.println("[DEBUG] onClientMessage() message: " + message + ", client: " + client);
	        
	 		Util.runOnUI(new Runnable() {
				
				@Override
				public void run() {
					getWebAppSessionListener().onReceiveMessage(MultiScreenWebAppSession.this, message);
				}
			});
		}
		
		@Override
		public void onClientDisconnected(ChannelClient client) {
			System.out.println("[DEBUG] onClientDisconnected: " + client.getId());

		}
		
		@Override
		public void onClientConnected(ChannelClient client) {
			System.out.println("[DEBUG] onClientConnected: " + client.getId());
			
		}
	};

	public MultiScreenWebAppSession(LaunchSession launchSession,
			DeviceService service) {
		super(launchSession, service);
		
		this.service = (MultiScreenService) service;
	}

	@Override
	public void close(ResponseListener<Object> listener) {
		System.out.println("[DEBUG] close");
		launchSession.close(listener);
	}

	@Override
	public void connect(final ResponseListener<Object> connectionListener) {
//		Map<String, String> clientAttributes = new HashMap<String, String>();
//		clientAttributes.put("name", "Ant");
//		
//		connectToChannel(service.getMultiScreenDevice(), channelId, clientAttributes, connectionListener);

		connectToChannel(service.getMultiScreenDevice(), channelId, connectionListener);
	}
	
	private void connectToChannel(Device device, String channelId, 
			final ResponseListener<Object> connectionListener) {
		
		connectToChannel(device, channelId, null, connectionListener);
//		device.connectToChannel(channelId, new DeviceAsyncResult<Channel>() {
//
//			@Override
//			public void onResult(Channel channel) {
//				System.out.println("[DEBUG] onResult: " + channel.toString());
//				mChannel = channel;
//				mChannel.setListener(channelListener);
//				Util.postSuccess(connectionListener, null);
//			}
//			
//			@Override
//			public void onError(DeviceError error) {
//				System.out.println("[DEBUG] onError: " + error.toString());
//				Util.postError(connectionListener, new ServiceCommandError(0, "Failed to connect channel", null));
//			}
//		});
	}
	
	private void connectToChannel(Device device, String channelId, 
			Map<String, String> clientAttributes, final ResponseListener<Object> connectionListener) {

		device.connectToChannel(channelId, clientAttributes, new DeviceAsyncResult<Channel>() {

			@Override
			public void onResult(Channel channel) {
				System.out.println("[DEBUG] onResult: " + channel.toString());
				mChannel = channel;
				mChannel.setListener(channelListener);
				Util.postSuccess(connectionListener, null);
			}
			
			@Override
			public void onError(DeviceError error) {
				System.out.println("[DEBUG] onError: " + error.toString());
				Util.postError(connectionListener, new ServiceCommandError(0, "Failed to connect channel", null));
			}
		});
		
	}

	@Override
	public void disconnectFromWebApp() {
		if (mChannel != null) {
			mChannel.disconnect(new ChannelAsyncResult<Boolean>() {
				
				@Override
				public void onResult(Boolean result) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onError(ChannelError error) {
					// TODO Auto-generated method stub
					
				}
			});
		}
	}

	@Override
	public void sendMessage(String message, ResponseListener<Object> listener) {
		sendToHost(message.toString());
	}

	@Override
	public void sendMessage(JSONObject message,
		ResponseListener<Object> listener) {
		
		sendMessage(message.toString(), listener);
	}
	
	private void broadcast(String message) {
		if (mChannel != null) {
			mChannel.broadcast(message);
		}
	}
	
	private void sendToAll(String message) {
		if (mChannel != null) {
			mChannel.sendToAll(message);
		}
	}
	
	private void sendToHost(String message) {
		if (mChannel != null) {
			mChannel.sendToHost(message);
		}
	}
	
	private void sendToClient(ChannelClient client, String message) {
		if (mChannel != null) {
			mChannel.sendToClient(client, message);
		}
	}
	
	private void sendToClientList(List<ChannelClient> clientList, String message) {
		if (mChannel != null) {
			mChannel.sendToClientList(clientList, message);
		}
	}
}
