/*
 * SSDP
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Copyright (c) 2011 stonker.lee@gmail.com https://code.google.com/p/android-dlna/
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

package com.connectsdk.core.upnp.ssdp;

import java.net.DatagramPacket;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class SSDP {
    /* New line definition */
    public static final String NEWLINE = "\r\n";
    
    public static final String ADDRESS = "239.255.255.250";
    public static final int PORT = 1900;
    public static final int SOURCE_PORT = 1901;
    
    public static final String ST = "ST";
    public static final String LOCATION = "LOCATION";
    public static final String NT = "NT";
    public static final String NTS = "NTS";
    public static final String URN = "URN";
    public static final String USN = "USN";
    public static final String APPLICATION_URL = "Application-URL";

    /* Definitions of start line */
    public static final String SL_NOTIFY = "NOTIFY * HTTP/1.1";
    public static final String SL_MSEARCH = "M-SEARCH * HTTP/1.1";
    public static final String SL_OK = "HTTP/1.1 200 OK";

    /* Definitions of search targets */
//    public static final String ST_ALL = ST + ": ssdp:all";
    public static final String ST_SSAP = ST + ": urn:lge-com:service:webos-second-screen:1";
    public static final String ST_DIAL = ST + ": urn:dial-multiscreen-org:service:dial:1";
    public static final String DEVICE_MEDIA_SERVER_1 = "urn:schemas-upnp-org:device:MediaServer:1"; 
    
//    public static final String SERVICE_CONTENT_DIRECTORY_1 = "urn:schemas-upnp-org:service:ContentDirectory:1";
//    public static final String SERVICE_CONNECTION_MANAGER_1 = "urn:schemas-upnp-org:service:ConnectionManager:1";
//    public static final String SERVICE_AV_TRANSPORT_1 = "urn:schemas-upnp-org:service:AVTransport:1";
//    
//    public static final String ST_ContentDirectory = ST + ":" + UPNP.SERVICE_CONTENT_DIRECTORY_1;
    
    /* Definitions of notification sub type */
    public static final String NTS_ALIVE = "ssdp:alive";
    public static final String NTS_BYEBYE = "ssdp:byebye";
    public static final String NTS_UPDATE = "ssdp:update";
    
    public static ParsedDatagram convertDatagram(DatagramPacket dp) {
    	return new ParsedDatagram(dp);
    }
    
    public static class ParsedDatagram {
    	public DatagramPacket dp;
    	public Map<String, String> data = new HashMap<String, String>();
    	public String type;
    	
    	static Charset ASCII_CHARSET = Charset.forName("US-ASCII");
    	
    	public ParsedDatagram(DatagramPacket packet) {
    		this.dp = packet;
    		
    		String text = new String(dp.getData(), ASCII_CHARSET);
    		
    		int pos = 0;
    		int eolPos = text.indexOf("\r\n");
    		
    		// Get first line
    		type = text.substring(0, eolPos);
    		pos = eolPos + 2;
    		
    		while (pos < text.length()) {
    			eolPos = text.indexOf("\r\n", pos);
    			
    			if (eolPos < 0) {
    				break;
    			}
    			
    			String line = text.substring(pos, eolPos);
    			pos = eolPos + 2;
    			
    			int index = line.indexOf(':');
    			if (index == -1) {
    				continue;
    			}
    			
    			String key = asciiUpper(line.substring(0, index));
    			String value = line.substring(index + 1).trim();
    			
    			data.put(key, value);
    		}
    	}
    	
        // Fast toUpperCase for ASCII strings
        private static String asciiUpper(String text) {
        	char [] chars = text.toCharArray();
        	
        	for (int i = 0; i < chars.length; i++) {
        		char c = chars[i];
        		chars[i] = (c >= 97 && c <= 122) ? (char) (c - 32) : c;
        	}
        	
        	return new String(chars);
        }
    }
}