package com.connectsdk.service.sessions;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.connectsdk.core.JSONDeserializable;
import com.connectsdk.core.JSONSerializable;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.capability.listeners.ResponseListener;

/**
 * Any time anything is launched onto a first screen device, there will be important session information that needs to be tracked. LaunchSession will track this data, and must be retained to perform certain actions within the session.
 */
public class LaunchSession implements JSONSerializable, JSONDeserializable {
	// @cond INTERNAL
	protected String appId;
	protected String appName;
	protected String sessionId;
	protected Object rawData;
	
	protected DeviceService service;
	protected LaunchSessionType sessionType;
	// @endcond
	
	/**
	 * LaunchSession type is used to help DeviceService's know how to close a LunchSession.
	 */
	public enum LaunchSessionType {
		/** Unknown LaunchSession type, may be unable to close this launch session */
		Unknown, 
		/** LaunchSession represents a launched app */
		App, 
		/** LaunchSession represents an external input picker that was launched */
		ExternalInputPicker, 
		/** LaunchSession represents a media app */
		Media, 
		/** LaunchSession represents a web app */
		WebApp
	}
	
	public LaunchSession() {
	}
	
	/**
	 * Instantiates a LaunchSession object for a given app ID.
	 *
	 * @param appId System-specific, unique ID of the app
	 */
	public static LaunchSession launchSessionForAppId(String appId) {
		LaunchSession launchSession = new LaunchSession();
		launchSession.appId = appId;
		
		return launchSession;
	}
	
	/**
	 * Closes the session on the first screen device. Depending on the sessionType, the associated service will have different ways of handling the close functionality.
	 *
	 * @param success (optional) SuccessBlock to be called on success
	 * @param failure (optional) FailureBlock to be called on failure
	 */
	public static LaunchSession launchSessionFromJSONObject(JSONObject json) {
		LaunchSession launchSession = new LaunchSession();
		try {
			launchSession.fromJSONObject(json);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return launchSession;
	}
	
	/** System-specific, unique ID of the app (ex. youtube.leanback.v4, 0000134, hulu) */
	public String getAppId() {
		return appId;
	}
	
	/** System-specific, unique ID of the app (ex. youtube.leanback.v4, 0000134, hulu) */
	public void setAppId(String appId) {
		this.appId = appId;
	}
	
	/** User-friendly name of the app (ex. YouTube, Browser, Hulu) */
	public String getAppName() {
		return appName;
	}
	
	/** User-friendly name of the app (ex. YouTube, Browser, Hulu) */
	public void setAppName(String appName) {
		this.appName = appName;
	}
	
	/** Unique ID for the session (only provided by certain protocols) */
	public String getSessionId() {
		return sessionId;
	}
	
	/** Unique ID for the session (only provided by certain protocols) */
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	
	/** DeviceService responsible for launching the session. */
	public DeviceService getService() {
		return service;
	}
	
	/** DeviceService responsible for launching the session. */
	public void setService(DeviceService service) {
		this.service = service;
	}
	
	/** Raw data from the first screen device about the session. In most cases, this is an NSDictionary. */
	public Object getRawData() {
		return rawData;
	}
	
	/** Raw data from the first screen device about the session. In most cases, this is an NSDictionary. */
	public void setRawData(Object rawData) {
		this.rawData = rawData;
	}
	
	/**
	 * When closing a LaunchSession, the DeviceService relies on the sessionType to determine the method of closing the session.
	 */
	public LaunchSessionType getSessionType() {
		return sessionType;
	}
	
	/**
	 * When closing a LaunchSession, the DeviceService relies on the sessionType to determine the method of closing the session.
	 */
	public void setSessionType(LaunchSessionType sessionType) {
		this.sessionType = sessionType;
	}
	
	/**
	 * Close the app/media associated with the session.
	 * @param listener
	 */
	public void close (ResponseListener<Object> listener) {
		service.closeLaunchSession(this, listener);
	}
	
	// @cond INTERNAl
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject obj = new JSONObject();
		
		obj.putOpt("appId", appId);
		obj.putOpt("sessionId", sessionId);
		obj.putOpt("name", appName);
		obj.putOpt("sessionType", sessionType.name());
		if (service != null) obj.putOpt("serviceName", service.getServiceName());
		
		if (rawData != null) {
			if (rawData instanceof JSONObject) obj.putOpt("rawData", rawData);
			if (rawData instanceof List<?>) {
				JSONArray arr = new JSONArray();
				for (Object item : (List<?>) rawData)
					arr.put(item);
				obj.putOpt("rawData", arr);
			}
			if (rawData instanceof Object[]) {
				JSONArray arr = new JSONArray();
				for (Object item : (Object[]) rawData)
					arr.put(item);
				obj.putOpt("rawData", arr);
			}
			if (rawData instanceof String) obj.putOpt("rawData", rawData);
		}
		
		return obj;
	}

	@Override
	public void fromJSONObject(JSONObject obj) throws JSONException {
		this.appId = obj.optString("appId");
		this.sessionId = obj.optString("sessionId");
		this.appName = obj.optString("name");
		this.sessionType = LaunchSessionType.valueOf(obj.optString("sessionType"));
		this.rawData = obj.opt("rawData");
	}
	
	// @endcond
	
	/**
	 * Compares two LaunchSession objects.
	 *
	 * @param launchSession LaunchSession object to compare.
	 *
	 * @return true if both LaunchSession id and sessionId values are equal
	 */
	@Override
	public boolean equals(Object o) {
		// TODO Auto-generated method stub
		return super.equals(o);
	}
}
