package com.connectsdk;

public interface IMediaLifeCycleListener {
  public void onMediaReady(RemoteMediaControl mediaControl);

  public void onMediaError();
}
