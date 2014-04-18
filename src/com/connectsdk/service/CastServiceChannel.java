/*
 * CastServiceChannel
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 24 Feb 2014
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

import org.json.JSONException;
import org.json.JSONObject;

import com.connectsdk.core.Util;
import com.connectsdk.service.sessions.CastWebAppSession;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;

public class CastServiceChannel implements Cast.MessageReceivedCallback{
	String webAppId;
	CastWebAppSession session;
	
	public CastServiceChannel(String webAppId, CastWebAppSession session) {
		this.webAppId = webAppId;
		this.session = session;
	}
	
	public String getNamespace() {
		return "urn:x-cast:com.connectsdk";
	}

	@Override
	public void onMessageReceived(CastDevice castDevice, String namespace, final String message) {
		if (session.getWebAppSessionListener() == null)
			return;
		
		JSONObject messageJSON = null;
		
		try {
			messageJSON = new JSONObject(message);
		} catch (JSONException e) { }
		
		final JSONObject mMessage = messageJSON;
		
		Util.runOnUI(new Runnable() {
			
			@Override
			public void run() {
				if (mMessage == null) {
					session.getWebAppSessionListener().onReceiveMessage(session, message);
				} else {
					session.getWebAppSessionListener().onReceiveMessage(session, mMessage);
				}
			}
		});
	}
}
