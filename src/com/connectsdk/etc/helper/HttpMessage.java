package com.connectsdk.etc.helper;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

public class HttpMessage {
	public final static String CONTENT_TYPE_HEADER = "Content-Type";
	public final static String CONTENT_TYPE = "text/xml; charset=utf-8";
	public final static String UDAP_USER_AGENT = "UDAP/2.0";
	public final static String LG_ELECTRONICS = "LG Electronics";
	public final static String USER_AGENT = "User-Agent";
	public final static String SOAP_ACTION = "\"urn:schemas-upnp-org:service:AVTransport:1#%s\"";
	public final static String SOAP_HEADER = "Soapaction";

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
	
	public static String encode(String str) {
		try {
			return URLEncoder.encode(str, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String decode(String str) {
		try {
			return URLDecoder.decode(str, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
}
