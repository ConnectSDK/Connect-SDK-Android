/*
 * ProgramInfo
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics. All rights reserved.
 * Created by Hyun Kook Khang on 19 Jan 2014
 * 
 */

package com.connectsdk.core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ProgramList implements JSONSerializable {
	ChannelInfo channel;
	JSONArray programList;
	
	public ProgramList(ChannelInfo channel, JSONArray programList) {
		this.channel = channel;
		this.programList = programList;
	}
	
	public ChannelInfo getChannel() {
		return channel;
	}

	public JSONArray getProgramList() {
		return programList;
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject obj = new JSONObject();
		
		obj.put("channel", channel != null ? channel.toString() : null);
		obj.put("programList", programList != null ? programList.toString() : null);
		
		return obj;
	}
}