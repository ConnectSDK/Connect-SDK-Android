package com.connectsdk.service.sessions;

public interface WebAppSessionListener {

	/**
	 * This method is called when a message is received from a web app.
	 *
	 * @param webAppSession WebAppSession that corresponds to the web app that sent the message
	 * @param message Object from the web app, either an String or a JSONObject
	 */
	public void onReceiveMessage(WebAppSession webAppSession, Object message);
	
	/**
	 * This method is called when a web app's communication channel (WebSocket, etc) has become disconnected.
	 *
	 * @param webAppSession WebAppSession that became disconnected
	 */	
	public void onWebAppSessionDisconnect(WebAppSession webAppSession);
}
