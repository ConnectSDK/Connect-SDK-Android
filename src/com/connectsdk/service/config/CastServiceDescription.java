package com.connectsdk.service.config;

import com.google.android.gms.cast.CastDevice;

public class CastServiceDescription extends ServiceDescription {
	CastDevice castDevice;
	
	public CastServiceDescription(String serviceFilter, String UUID, String ipAddress, CastDevice castDevice) {
		super(serviceFilter, UUID, ipAddress);
		this.castDevice = castDevice;
	}

	public CastDevice getCastDevice() {
		return castDevice;
	}

	public void setCastDevice(CastDevice castDevice) {
		this.castDevice = castDevice;
	}

}
