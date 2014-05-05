/*
 * NetcastHttpServer
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

package com.connectsdk.device.netcast;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import android.content.Context;
import android.util.Log;

import com.connectsdk.core.ChannelInfo;
import com.connectsdk.core.TextInputStatusInfo;
import com.connectsdk.core.Util;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.service.NetcastTVService;
import com.connectsdk.service.command.URLServiceSubscription;

public class NetcastHttpServer implements Runnable {
	// static constants
	static final String UDAP_PATH_EVENT = "/udap/api/event";

	// instance variables
	NetcastTVService service;
	Socket connect;

	String ipAddress;
	Context context;

	List<URLServiceSubscription<?>> subscriptions;
	
	// constructor
	public NetcastHttpServer(NetcastTVService service, Socket connect, String ipAddress, Context context) {
		this.service = service;
		this.connect = connect;
		this.ipAddress = ipAddress;
		this.context = context;
	}
	
	@SuppressWarnings("unused")
	public void run() {
		Log.d("Connect SDK", "Running Netcast HttpServer");
		String body;
		
		BufferedReader in = null;
		PrintWriter out = null;
		BufferedOutputStream dataOut = null;

		String api;

		try {
			// get character input stream from client
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			// get character output stream to client (for headers)
			out = new PrintWriter(connect.getOutputStream());
			// get binary output stream to client (for requested data)
			dataOut = new BufferedOutputStream(connect.getOutputStream());

			// get first line of request from client
			String input = in.readLine();
			// create StringTokenizer to parse request
			StringTokenizer parse = new StringTokenizer(input);
			// parse out method
			String method = parse.nextToken().toUpperCase(Locale.ENGLISH);
			// parse out file requested
			api = parse.nextToken().toLowerCase(Locale.ENGLISH);
			
			String str = null;
			while ( (str = in.readLine()) != null ) {
				if ( str.equals("") ) {
					break;
				}
			}
			
			int c;
			StringBuilder sb = new StringBuilder();
			while ( (c = in.read()) != -1 ) {
				sb.append((char)c);
				String temp = sb.toString();
				
				if ( temp.endsWith("</envelope>") )
					break;
			}
			body = sb.toString();
			
			if ( method.equals("POST") ) {
				SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
				InputStream stream = new ByteArrayInputStream(body.getBytes("UTF-8"));
				NetcastPOSTRequestParser handler = new NetcastPOSTRequestParser();
				
				SAXParser saxParser;
				try {
					saxParser = saxParserFactory.newSAXParser();
					saxParser.parse(stream, handler);
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				} catch (SAXException e) {
					e.printStackTrace();
				}

				if ( body.contains("ChannelChanged") ) {
					ChannelInfo channel = NetcastChannelParser.parseRawChannelData(handler.getJSONObject());

					Log.d("Connect SDK", "Channel Changed: " + channel.getNumber());

					for (URLServiceSubscription<?> subs: subscriptions) {
						if ( subs.getTarget().equalsIgnoreCase("ChannelChanged") ) {
							Util.postSuccess(subs.getResponseListener(), channel);
						}
					}
				}
				else if ( body.contains("KeyboardVisible") ) {
					boolean focused = false;
					
					TextInputStatusInfo keyboard = new TextInputStatusInfo();
					keyboard.setRawData(handler.getJSONObject());
					
					try {
						JSONObject currentWidget = (JSONObject) handler.getJSONObject().get("currentWidget");
						focused = (Boolean) currentWidget.get("focus");
						keyboard.setFocused(focused);
					} catch (JSONException e) {
						e.printStackTrace();
					}

					Log.d("Connect SDK", "KeyboardFocused?: " + focused);
					
					for (URLServiceSubscription<?> subs: subscriptions) {
						if ( subs.getTarget().equalsIgnoreCase("KeyboardVisible") ) {
							Util.postSuccess(subs.getResponseListener(), keyboard);
						}
					}
				}
				else if ( body.contains("TextEdited") ) {
					System.out.println("TextEdited");
					
				}
				else if ( body.contains("byebye") ) {
					Log.w("Connect SDK", "Received bye bye");
					
					ConnectableDevice device = DiscoveryManager.getInstance().getCompatibleDevices().get(ipAddress);
					
					DiscoveryManager.getInstance().getCompatibleDevices().remove(ipAddress);
					DiscoveryManager.getInstance().getAllDevices().remove(ipAddress);

					DiscoveryManager.getInstance().handleDeviceLoss(device);
					
					service.hostByeBye();
				}
				else if ( body.contains("3DMode") ) {
					try {
						String enabled = (String) handler.getJSONObject().get("value");
						boolean bEnabled;
						
						if ( enabled.equalsIgnoreCase("true") )
							bEnabled = true;
						else
							bEnabled = false;
						
						for (URLServiceSubscription<?> subs: subscriptions) {
							if ( subs.getTarget().equalsIgnoreCase("3DMode") ) {
								Util.postSuccess(subs.getResponseListener(), bEnabled);
							}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				
				out.println("HTTP/1.1 200 OK");
				out.println("Server: OS/versionUDAP/2.0product/version");
				out.println("Cache-Control: no-store, no-cache, must-revalidate");
				out.println("Date: Time when the host response is occurred");
				out.println("Connection: Close");
				out.println("Content-Length: 0");
				out.flush();
			}
			
//			// methods other than GET and HEAD are not implemented
//			if (!method.equals("GET") && !method.equals("HEAD")) {
//				// send Not Implemented message to client
//				out.println("HTTP/1.0 501 Not Implemented");
//				out.println("Server: Java HTTP Server 1.0");
////				out.println("Date: " + new Date());
//				out.println("Content-Type: text/html");
//				out.println(); // blank line between headers and content
//				out.println("<HTML>");
//				out.println("<HEAD><TITLE>Not Implemented</TITLE>" + "</HEAD>");
//				out.println("<BODY>");
//				out.println("<H2>501 Not Implemented: " + method
//						+ " method.</H2>");
//				out.println("</BODY></HTML>");
//				out.flush();
//
//				return;
//			}
		} catch (IOException e) {
			System.err.println("Server Error: " + e);
			e.printStackTrace();
		} finally {
			close(in); // close character input stream
			close(out); // close character output stream
			close(dataOut); // close binary output stream
			close(connect); // close socket connection
		}
	}

	/**
	 * close method closes the given stream.
	 * 
	 * @param stream
	 */
	public void close(Object stream) {
		if (stream == null)
			return;

		try {
			if (stream instanceof Reader) {
				((Reader) stream).close();
			} else if (stream instanceof Writer) {
				((Writer) stream).close();
			} else if (stream instanceof InputStream) {
				((InputStream) stream).close();
			} else if (stream instanceof OutputStream) {
				((OutputStream) stream).close();
			} else if (stream instanceof Socket) {
				((Socket) stream).close();
			} else {
				System.err.println("Unable to close object: " + stream);
			}
		} catch (Exception e) {
			System.err.println("Error closing stream: " + e);
		}
	}

	public void setSubscriptions(List<URLServiceSubscription<?>> subscriptions) {
		this.subscriptions = subscriptions;
	}

}