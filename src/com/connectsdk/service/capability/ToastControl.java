/*
 * ToastControl
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics. All rights reserved.
 * Created by Hyun Kook Khang on 19 Jan 2014
 * 
 */

package com.connectsdk.service.capability;

import org.json.JSONObject;

import com.connectsdk.core.AppInfo;
import com.connectsdk.service.capability.listeners.ResponseListener;

public interface ToastControl extends CapabilityMethods {
	public final static String Any = "ToastControl.Any";

	public final static String Show_Toast = "ToastControl.Show";
	public final static String Show_Clickable_Toast_App = "ToastControl.Show.Clickable.App";
	public final static String Show_Clickable_Toast_App_Params = "ToastControl.Show.Clickable.App.Params";
	public final static String Show_Clickable_Toast_URL = "ToastControl.Show.Clickable.URL";

	public final static String[] Capabilities = {
	    Show_Toast,
	    Show_Clickable_Toast_App,
	    Show_Clickable_Toast_App_Params,
	    Show_Clickable_Toast_URL
	};

	public ToastControl getToastControl();
	public CapabilityPriorityLevel getToastControlCapabilityLevel();

	public void showToast(String message, ResponseListener<Object> listener);
	public void showToast(String message, String iconData, String iconExtension, ResponseListener<Object> listener);

	public void showClickableToastForApp(String message, AppInfo appInfo, JSONObject params, ResponseListener<Object> listener);
	public void showClickableToastForApp(String message, AppInfo appInfo, JSONObject params, String iconData, String iconExtension, ResponseListener<Object> listener);

	public void showClickableToastForURL(String message, String url, ResponseListener<Object> listener);
	public void showClickableToastForURL(String message, String url, String iconData, String iconExtension, ResponseListener<Object> listener);
}