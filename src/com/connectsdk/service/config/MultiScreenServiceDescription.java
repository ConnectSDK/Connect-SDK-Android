package com.connectsdk.service.config;

import com.samsung.multiscreen.device.Device;

public class MultiScreenServiceDescription extends ServiceDescription {
	Device multiScreenDevice;
	
	public MultiScreenServiceDescription(String serviceFilter, String UUID, String ipAddress, Device multiScreenDevice) {
		super(serviceFilter, UUID, ipAddress);
		this.multiScreenDevice = multiScreenDevice;
	}

	public Device getMultiScreenDevice() {
		return multiScreenDevice;
	}

	public void setMultiScreenDevice(Device multiScreenDevice) {
		this.multiScreenDevice = multiScreenDevice;
	}
	
}
