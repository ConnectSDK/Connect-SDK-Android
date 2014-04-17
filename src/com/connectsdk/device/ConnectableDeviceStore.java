/*
 * ConnectableDeviceStore
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 19 Jan 2014
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

package com.connectsdk.device;

import java.util.List;

/**
 * ConnectableDeviceStore is an interface which can be implemented to save key information about ConnectableDevices that have been discovered on the network. Any class which implements this interface can be used as DiscoveryManager's deviceStore.
 *
 * The ConnectableDevice objects loaded from the ConnectableDeviceStore are for informational use only, and should not be interacted with. DiscoveryManager uses these ConnectableDevice objects to populate re-discovered ConnectableDevices with relevant data (last connected, pairing info, etc).
 *
 * A default implementation, DefaultConnectableDeviceStore, will be used by DiscoveryManager if no other ConnectableDeviceStore is provided to DiscoveryManager when startDiscovery is called.
 *
 * ###Privacy Considerations
 * If you chose to implement ConnectableDeviceStore, it is important to keep your users' privacy in mind.
 * - There should be UI elements in your app to
 *   + completely disable ConnectableDeviceStore
 *   + purge all data from ConnectableDeviceStore (removeAll)
 * - Your ConnectableDeviceStore implementation should
 *   + avoid tracking too much data (all discovered devices)
  *  + periodically remove ConnectableDevices from the ConnectableDeviceStore if they haven't been used/connected in X amount of time
 */
public interface ConnectableDeviceStore {
	
	/**
	 * Add a ConnectableDevice to the ConnectableDeviceStore. If the ConnectableDevice is already stored, it's record will be updated.
	 *
	 * @param device ConnectableDevice to add to the ConnectableDeviceStore
	 */
	public void addDevice(ConnectableDevice device);
	
	/**
	 * Removes a ConnectableDevice's record from the ConnectableDeviceStore.
	 *
	 * @param device ConnectableDevice to remove from the ConnectableDeviceStore
	 */
	public void removeDevice(ConnectableDevice device);
	
	/**
	 * Updates a ConnectableDevice's record in the ConnectableDeviceStore.
	 *
	 * @param device ConnectableDevice to update in the ConnectableDeviceStore
	 */
	public void updateDevice(ConnectableDevice device);
	
	/**
	 * A List of all ConnectableDevices in the ConnectableDeviceStore. These ConnectableDevice objects are for informational use only, and should not be interacted with. DiscoveryManager uses these ConnectableDevice objects to populate discovered ConnectableDevices with relevant data (last connected, pairing info, etc).
	 */
	public List<ConnectableDevice> getStoredDevices();
	
	/**
	 * Clears out the ConnectableDeviceStore, removing all records.
	 */
	public void removeAll();
}
