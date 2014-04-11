/*
 * ServiceCommandError
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics. All rights reserved.
 * Created by Hyun Kook Khang on 19 Jan 2014
 * 
 */

package com.connectsdk.service.command;


public class ServiceCommandError extends Error {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4232138682873631468L;

	int code;
	Object payload;
	
	public static ServiceCommandError notSupported() {
		return new ServiceCommandError(503, "not supported", null);
	}
	
	public ServiceCommandError(int code, String desc, Object payload) {
		super(desc);
		this.code = code;
		this.payload = payload;
	}

	public int getCode() {
		return code;
	}

	public Object getPayload() {
		return payload;
	}
	
	public static ServiceCommandError getError(int code) {
		String desc = null;
        if ( code == 400 ) {
        	desc = "Bad Request";
        }
        else if ( code == 401 ) {
        	desc = "Unauthorized";
        }
        else if ( code == 500 ) { 
        	desc = "Internal Server Error";
        }
        else if ( code == 503 ) {
        	desc = "Service Unavailable";
        }
        else {
        	desc = "Unknown Error";
        }
        
        return new ServiceCommandError(code, desc, null);
	}
}
