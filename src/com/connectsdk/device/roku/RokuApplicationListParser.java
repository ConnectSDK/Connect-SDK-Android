package com.connectsdk.device.roku;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.connectsdk.core.AppInfo;

public class RokuApplicationListParser extends DefaultHandler {
	public String value;
	
	public final String APP = "app";
	public final String ID = "id";
	
	public List<AppInfo> appList;
	public AppInfo appInfo;
	
	public RokuApplicationListParser() {
		value = null;
		appList = new ArrayList<AppInfo>();
	}

	@Override
	public void startElement(String uri, String localName, String qName, final Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase(APP)) {
			final int index = attributes.getIndex(ID);
			
			if ( index != -1 ) {
				appInfo = new AppInfo() {{
					setId(attributes.getValue(index));
				}};
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase(APP)) {
			appInfo.setName(value);
			appList.add(appInfo);
        }
		value = null;
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		value = new String(ch, start, length);
	}
	
	public List<AppInfo> getApplicationList() {
		return appList;
	}
}
