package com.connectsdk.etc.helper;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

public class HttpMessage {
	public final static String CONTENT_TYPE_HEADER = "Content-Type";
	public final static String CONTENT_TYPE = "text/xml; charset=utf-8";
	public final static String UDAP_USER_AGENT = "UDAP/2.0";
	public final static String LG_ELECTRONICS = "LG Electronics";
	public final static String USER_AGENT = "User-Agent";

	public static HttpPost getHttpPost(String uri) {
		HttpPost post = null;
		try {
			post = new HttpPost(uri);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		post.setHeader("Content-Type", CONTENT_TYPE);
		
		return post;
	}
	
	public static HttpPost getUDAPHttpPost(String uri) {
		HttpPost post = getHttpPost(uri);
		post.setHeader("User-Agent", UDAP_USER_AGENT);
		
		return post;
	}
	
	public static HttpPost getDLNAHttpPost(String uri, String action) {
		String soapAction = "\"urn:schemas-upnp-org:service:AVTransport:1#" + action + "\"";

		HttpPost post = getHttpPost(uri);
        post.setHeader("Soapaction", soapAction);
        
        return post;
    }
	
	public static HttpGet getHttpGet(String url) { 
		return new HttpGet(url);
	}
	
	public static HttpGet getUDAPHttpGet(String uri) { 
		HttpGet get = getHttpGet(uri);
		get.setHeader("User-Agent", UDAP_USER_AGENT);
		
		return get;
	}
	
	public static HttpDelete getHttpDelete(String url) { 
		return new HttpDelete(url);
	}
	
	public static String percentEncoding(String str) {
		if ( str == null ) 
			return null;
		
		str = str.replace("%", "%25");
		str = str.replace("!", "%21");
		str = str.replace("*", "%2A");
		str = str.replace("'", "%27");
		str = str.replace("(", "%28");
		str = str.replace(")", "%29");
		str = str.replace(";", "%3B");
		str = str.replace(":", "%3A");
		str = str.replace("@", "%40");
		str = str.replace("&", "%26");
		str = str.replace("=", "%3D");
		str = str.replace("+", "%2B");
		str = str.replace("$", "%24");
		str = str.replace(",", "%2C");
		str = str.replace("/", "%2F");
		str = str.replace("?", "%3F");
		str = str.replace("#", "%23");
		str = str.replace("[", "%5B");
		str = str.replace("]", "%2D");
		str = str.replace(" ", "%20");

		return str;
	}
	
	public static String percentDecoding(String str) {
		if ( str == null ) 
			return null;

		str = str.replace("%21", "!");
		str = str.replace("%2A", "*");
		str = str.replace("%27", "'");
		str = str.replace("%28", "(");
		str = str.replace("%29", ")");
		str = str.replace("%3B", ";");
		str = str.replace("%3A", ":");
		str = str.replace("%40", "@");
		str = str.replace("%26", "&");
		str = str.replace("%3D", "=");
		str = str.replace("%2B", "+");
		str = str.replace("%24", "$");
		str = str.replace("%2C", ",");
		str = str.replace("%2F", "/");
		str = str.replace("%3F", "?");
		str = str.replace("%23", "#");
		str = str.replace("%5B", "[");
		str = str.replace("%2D", "]");
		str = str.replace("%25", "%");
		str = str.replace("%20", " ");
		
		return str;
	}
}
