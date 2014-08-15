package com.connectsdk;

import java.util.HashMap;


public class DefaultPlatform {

//	registerDeviceService(WebOSTVService.class, SSDPDiscoveryProvider.class);
//	registerDeviceService(NetcastTVService.class, SSDPDiscoveryProvider.class);
//	registerDeviceService(DLNAService.class, SSDPDiscoveryProvider.class);
//	registerDeviceService(DIALService.class, SSDPDiscoveryProvider.class);
//	registerDeviceService(RokuService.class, SSDPDiscoveryProvider.class);
//	registerDeviceService(CastService.class, CastDiscoveryProvider.class);
//	registerDeviceService(AirPlayService.class, ZeroconfDiscoveryProvider.class);
//	registerDeviceService(MultiScreenService.class, SSDPDiscoveryProvider.class);
	
	private HashMap<String, String> deviceServiceMap;

	public DefaultPlatform() {
		
		deviceServiceMap = new HashMap<String, String>();
	}
	
	public HashMap<String, String> getDeviceServiceMap() {
		deviceServiceMap.put("WebOSTVService", "SSDPDiscoveryProvider");
		deviceServiceMap.put("NetcastTVService", "SSDPDiscoveryProvider");
		deviceServiceMap.put("DLNAService", "SSDPDiscoveryProvider");
		deviceServiceMap.put("DIALService", "SSDPDiscoveryProvider");
		deviceServiceMap.put("RokuService", "SSDPDiscoveryProvider");
		deviceServiceMap.put("CastService", "CastDiscoveryProvider");
		deviceServiceMap.put("AirPlayService", "ZeroconfDiscoveryProvider");
		deviceServiceMap.put("MultiScreenService", "SSDPDiscoveryProvider");
		return deviceServiceMap;
	}
	
	
	
	
	
	
	
}
