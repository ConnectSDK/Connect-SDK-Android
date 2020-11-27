package com.connectsdk.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

public class ActivityTracker implements Application.ActivityLifecycleCallbacks {
  private Activity currentActivity;

  public ActivityTracker(Activity activity) {
    this.currentActivity = activity;
  }

  @Override
  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

  @Override
  public void onActivityStarted(Activity activity) {}

  @Override
  public void onActivityResumed(Activity activity) {
    currentActivity = activity;
  }

  @Override
  public void onActivityPaused(Activity activity) {
    currentActivity = null;
  }

  @Override
  public void onActivityStopped(Activity activity) {}

  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

  @Override
  public void onActivityDestroyed(Activity activity) {}

  public Activity getCurrentActivity() {
    return currentActivity;
  }
}
