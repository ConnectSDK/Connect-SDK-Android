/*
 * MouseControl
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics. All rights reserved.
 * Created by Hyun Kook Khang on 19 Jan 2014
 * 
 */

package com.connectsdk.service.capability;

import android.graphics.PointF;


public interface MouseControl extends CapabilityMethods {
	public final static String Any = "MouseControl.Any";

	public final static String Connect = "MouseControl.Connect";
	public final static String Disconnect = "MouseControl.Disconnect";
	public final static String Click = "MouseControl.Click";
	public final static String Move = "MouseControl.Move";
	public final static String Scroll = "MouseControl.Scroll";

	public final static String[] Capabilities = {
	    Connect,
	    Disconnect,
	    Click,
	    Move,
	    Scroll
	};

	public MouseControl getMouseControl();
	public CapabilityPriorityLevel getMouseControlCapabilityLevel();

	public void connectMouse();
	public void disconnectMouse();

	public void click();
	public void move(double dx, double dy);
	public void move(PointF distance);
	public void scroll(double dx, double dy);
	public void scroll(PointF distance);
}
