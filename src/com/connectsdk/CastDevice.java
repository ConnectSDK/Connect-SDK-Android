package com.connectsdk;

import com.connectsdk.device.ConnectableDevice;

public class CastDevice {
  private String id = "";
  private String name = "";
  private String type = "";

  private CastDevice() {}

  public static CastDevice fromConnectableDevice(ConnectableDevice connectableDevice) {
    CastDevice device = new CastDevice();
    if (connectableDevice != null) {
      device.id = connectableDevice.getId();
      if (connectableDevice.getFriendlyName() != null) {
        device.name = connectableDevice.getFriendlyName();
      } else {
        device.name = connectableDevice.getModelName();
      }
      device.type = connectableDevice.getServiceId();
    }
    return device;
  }

  public static CastDevice forDeviceNotAvailble() {
    return new CastDevice();
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }
}
