package com.connectsdk;

public class CastStatus {
  private int castState;
  private CastDevice castDevice;

  public CastStatus() {
    this(CastState.NO_DEVICE_AVAILABLE, CastDevice.fromConnectableDevice(null));
  }

  public CastStatus(int castState, CastDevice castDevice) {
    this.castState = castState;
    this.castDevice = castDevice;
  }

  public int getCastState() {
    return castState;
  }

  public void setCastState(int castState) {
    this.castState = castState;
    if (
      castState == CastState.NO_DEVICE_AVAILABLE ||
      castState == CastState.NOT_CONNECTED
    ) {
      castDevice = CastDevice.fromConnectableDevice(null);
    }
  }

  public CastDevice getCastDevice() {
    return castDevice;
  }

  public void setCastDevice(CastDevice castDevice) {
    this.castDevice = castDevice;
  }
}
