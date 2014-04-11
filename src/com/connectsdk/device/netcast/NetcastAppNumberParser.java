/*
 * NetcastAppNumberParser
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics. All rights reserved.
 * Created by Hyun Kook Khang on 19 Jan 2014
 * 
 */

package com.connectsdk.device.netcast;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class NetcastAppNumberParser extends DefaultHandler {
	public String value;
	
	public final String TYPE = "type";
	public final String NUMBER = "number";
	
	int count;
	
	public NetcastAppNumberParser() {
		value = null;
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase(TYPE)) {
        }
		else if (qName.equalsIgnoreCase(NUMBER)) {
			count = Integer.parseInt(value);
        }
		value = null;
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		value = new String(ch, start, length);
	}
	
	public int getApplicationNumber() {
		return count;
	}
}
