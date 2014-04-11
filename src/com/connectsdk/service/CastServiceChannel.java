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
