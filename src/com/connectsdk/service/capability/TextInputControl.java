/*
 * TextInputControl
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics. All rights reserved.
 * Created by Hyun Kook Khang on 19 Jan 2014
 * 
 */

package com.connectsdk.service.capability;

import com.connectsdk.core.TextInputStatusInfo;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceSubscription;

public interface TextInputControl extends CapabilityMethods {
	public final static String Any = "TextInputControl.Any";

	public final static String Send = "TextInputControl.Send";
	public final static String Send_Enter = "TextInputControl.Enter";
	public final static String Send_Delete = "TextInputControl.Delete";
	public final static String Subscribe = "TextInputControl.Subscribe";

	public final static String[] Capabilities = {
	    Send,
	    Send_Enter,
	    Send_Delete,
	    Subscribe
	};

	public TextInputControl getTextInputControl();
	public CapabilityPriorityLevel getTextInputControlCapabilityLevel();

	public ServiceSubscription<TextInputStatusListener> subscribeTextInputStatus(TextInputStatusListener listener);

	public void sendText(String input);
	public void sendEnter();
	public void sendDelete();

	/**
	 * Response block that is fired on any change of keyboard visibility.
	 *
	 * Passes TextInputStatusInfo object that provides keyboard type & visibility information
	 */
	public static interface TextInputStatusListener extends ResponseListener<TextInputStatusInfo> { }
}
