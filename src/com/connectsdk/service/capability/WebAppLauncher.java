/*
 * WebAppLauncher
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics. All rights reserved.
 * Created by Hyun Kook Khang on 19 Jan 2014
 * 
 */

package com.connectsdk.service.capability;

import org.json.JSONObject;

import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.sessions.LaunchSession;
import com.connectsdk.service.sessions.WebAppSession.LaunchListener;


public interface WebAppLauncher extends CapabilityMethods {
	public final static String Any = "WebAppLauncher.Any";

	public final static String Launch = "WebAppLauncher.Launch";
	public final static String Join = "WebAppLauncher.Join";
	public final static String Launch_Params = "WebAppLauncher.Launch.Params";
	public final static String Message_Send = "WebAppLauncher.Message.Send";
	public final static String Message_Receive = "WebAppLauncher.Message.Receive";
	public final static String Message_Send_JSON = "WebAppLauncher.Message.Send.JSON";
	public final static String Message_Receive_JSON = "WebAppLauncher.Message.Receive.JSON";
	public final static String Close = "WebAppLauncher.Close";

	public final static String[] Capabilities = {
	    Launch,
	    Join, 
	    Launch_Params,
	    Message_Send,
	    Message_Receive,
	    Message_Send_JSON,
	    Message_Receive_JSON,
	    Close
	};

	public WebAppLauncher getWebAppLauncher();
	public CapabilityPriorityLevel getWebAppLauncherCapabilityLevel();

	public void launchWebApp(String webAppId, LaunchListener listener);
	public void launchWebApp(String webAppId, boolean relaunchIfRunning, LaunchListener listener);
	public void launchWebApp(String webAppId, JSONObject params, LaunchListener listener);
	public void launchWebApp(String webAppId, JSONObject params, boolean relaunchIfRunning, LaunchListener listener);
	public void closeWebApp(LaunchSession launchSession, ResponseListener<Object> listener); 
}
