package com.connectsdk.service.upnp;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

public class DLNANotifyParser {
	private static final String ns = null;
	
	public JSONObject parse(InputStream in) throws XmlPullParserException, IOException, JSONException {
    	try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readEvent(parser);
        } finally {
            in.close();
        }
	}
	
	public JSONObject readEvent(XmlPullParser parser) throws IOException, XmlPullParserException, JSONException {
		JSONObject event = new JSONObject();
		
	    parser.require(XmlPullParser.START_TAG, ns, "Event");
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("Event")) {
	        }
	        else if (name.equals("InstanceID")) {
	        }
	        else {
	        	event.put(name, readEventValue(name, parser));
	        }
	    }
	    return event;
	}
	
	private String readEventValue(String target, XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, ns, target);
	    String value = parser.getAttributeValue(null, "val");
        parser.nextTag();
	    parser.require(XmlPullParser.END_TAG, ns, target);
	    return value;
	}
}
