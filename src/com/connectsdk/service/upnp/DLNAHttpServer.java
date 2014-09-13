package com.connectsdk.service.upnp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import android.text.Html;

import com.connectsdk.core.Util;
import com.connectsdk.service.capability.MediaControl.PlayStateStatus;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.URLServiceSubscription;

public class DLNAHttpServer {
	ServerSocket welcomeSocket;
	
	int port = 49291;

	boolean running = false;

	CopyOnWriteArrayList<URLServiceSubscription<?>> subscriptions;
	
	public DLNAHttpServer() {
	    subscriptions = new CopyOnWriteArrayList<URLServiceSubscription<?>>();
	}

	public void start() {
		if (running)
			return;
		
		running = true;
		
		try {
			welcomeSocket = new ServerSocket(this.port);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		
		while (running) {
			if (welcomeSocket == null || welcomeSocket.isClosed()) {
				stop();
				break;
			}
			
			Socket connectionSocket = null;
			BufferedReader inFromClient = null;
			DataOutputStream outToClient = null;
			
			try {
				connectionSocket = welcomeSocket.accept();
			} catch (IOException ex) {
				ex.printStackTrace();
				// this socket may have been closed, so we'll stop
				stop();
				return;
			}
			
			int c = 0;
			
			String body = null;
			
			try {
				inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

				StringBuilder sb = new StringBuilder();
				
				while ((c = inFromClient.read()) != -1) {
					sb.append((char)c);

					if (sb.toString().endsWith("\r\n\r\n"))
						break;
				}
				
				sb = new StringBuilder();
				
				while ((c = inFromClient.read()) != -1) {
					sb.append((char)c);
					body = sb.toString();

					if (body.endsWith("</e:propertyset>"))
						break;
				}

				body = Html.fromHtml(body).toString();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			
			PrintWriter out = null;
			
			try {
				outToClient = new DataOutputStream(connectionSocket.getOutputStream());
				out = new PrintWriter(outToClient);
				out.println("HTTP/1.1 200 OK");
				out.println("Connection: Close");
				out.println("Content-Length: 0");
				out.println();
				out.flush();
			} catch (IOException ex) {
				ex.printStackTrace();
			} finally {
				try {
					inFromClient.close();
					out.close();
					outToClient.close();
					connectionSocket.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
			
			InputStream stream = null;
			
			try {
				stream = new ByteArrayInputStream(body.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException ex) {
				ex.printStackTrace();
			}
			
			JSONObject event;
			DLNANotifyParser parser = new DLNANotifyParser();
			
			try {
				event = parser.parse(stream);
				
				if (event.has("TransportState")) {
					String transportState = event.getString("TransportState");
					PlayStateStatus status = PlayStateStatus.convertTransportStateToPlayStateStatus(transportState);
					
					for (URLServiceSubscription<?> sub: subscriptions) {
						if (sub.getTarget().equalsIgnoreCase("playState")) {
							for (int i = 0; i < sub.getListeners().size(); i++) {
								@SuppressWarnings("unchecked")
								ResponseListener<Object> listener = (ResponseListener<Object>) sub.getListeners().get(i);
								Util.postSuccess(listener, status);
							}
						}
					}
				}
				else if (event.has("Volume")) {
					int intVolume = event.getInt("Volume");
					float volume = (float) intVolume / 100;

					for (URLServiceSubscription<?> sub : subscriptions) {
						if (sub.getTarget().equalsIgnoreCase("volume")) {
							for (int i = 0; i < sub.getListeners().size(); i++) {
								@SuppressWarnings("unchecked")
								ResponseListener<Object> listener = (ResponseListener<Object>) sub.getListeners().get(i);
								Util.postSuccess(listener, volume);
							}
						}
					}
				}
				else if (event.has("Mute")) {
					String muteStatus = event.getString("Mute");
					boolean mute;
					
					try {
						mute = (Integer.parseInt(muteStatus) == 1) ? true : false;
				    } catch(NumberFormatException e) {
				    	mute = Boolean.parseBoolean(muteStatus);
				    }

					for (URLServiceSubscription<?> sub : subscriptions) {
						if (sub.getTarget().equalsIgnoreCase("mute")) {
							for (int i = 0; i < sub.getListeners().size(); i++) {
								@SuppressWarnings("unchecked")
								ResponseListener<Object> listener = (ResponseListener<Object>) sub.getListeners().get(i);
								Util.postSuccess(listener, mute);
							}
						}
					}
				}
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void stop() {
		if (!running)
			return;
		
		for (URLServiceSubscription<?> sub: subscriptions) {
			sub.unsubscribe();
		}
		subscriptions.clear();
		
		if (welcomeSocket != null && !welcomeSocket.isClosed()) {
			try {
				welcomeSocket.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		welcomeSocket = null;
		running = false;
	}
	
	public int getPort() {
		return port;
	}
	
	public List<URLServiceSubscription<?>> getSubscriptions() {
		return subscriptions;
	}

	public void setSubscriptions(List<URLServiceSubscription<?>> subscriptions) {
		this.subscriptions = new CopyOnWriteArrayList<URLServiceSubscription<?>>(subscriptions);
	}
}
