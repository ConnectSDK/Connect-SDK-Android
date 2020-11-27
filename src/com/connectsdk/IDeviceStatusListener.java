package com.connectsdk;

public interface IDeviceStatusListener {
  public void onDeviceConnecting(int castState, CastDevice castDevice);
}
