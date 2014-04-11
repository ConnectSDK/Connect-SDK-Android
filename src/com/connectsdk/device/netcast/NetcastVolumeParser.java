/*
 * NetcastVolumeParser
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics. All rights reserved.
 * Created by Hyun Kook Khang on 19 Jan 2014
 * 
 */

package com.connectsdk.device.netcast;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class NetcastVolumeParser extends DefaultHandler {
	public JSONObject volumeStatus;

	public String value;
	
	public final String MUTE = "mute";
	public final String MIN_LEVEL = "minLevel";
	public final String MAX_LEVEL = "maxLevel";
	public final String LEVEL = "level";
	
	public NetcastVolumeParser() {
		volumeStatus = new JSONObject();
		value = null;
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		try {
			if (qName.equalsIgnoreCase(MUTE)) {
				volumeStatus.put(MUTE, Boolean.parseBoolean(value));
	        }
			else if (qName.equalsIgnoreCase(MIN_LEVEL)) {
				volumeStatus.put(MIN_LEVEL, Integer.parseInt(value));
	        }
			else if (qName.equalsIgnoreCase(MAX_LEVEL)) {
				volumeStatus.put(MAX_LEVEL, Integer.parseInt(value));
	        }
			else if (qName.equalsIgnoreCase(LEVEL)) {
				volumeStatus.put(LEVEL, Integer.parseInt(value));
	        }
			value = null;
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		value = new String(ch, start, length);
	}
	
	public JSONObject getVolumeStatus() {
		return volumeStatus;
	}
}
