package com.connectsdk.core;

import org.json.JSONException;
import org.json.JSONObject;

public interface JSONDeserializable {
	public void fromJSONObject(JSONObject obj) throws JSONException;
}
