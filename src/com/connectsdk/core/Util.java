package com.connectsdk.core;

import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import android.os.Handler;
import android.os.Looper;

import com.connectsdk.service.capability.listeners.ErrorListener;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;

public final class Util {
	static public String T = "Connect SDK";
	static private Handler handler;
	static private final int NUM_OF_THREADS = 20;

	static private Executor executor = Executors.newFixedThreadPool(NUM_OF_THREADS, new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread th = new Thread(r);
			th.setName("2nd Screen BG");
			return th;
		}
	});
	
	public static void runOnUI(Runnable runnable) {
		if (handler == null) {
			handler = new Handler(Looper.getMainLooper());
		}
		
		handler.post(runnable);
	}
	
	public static void runInBackground(Runnable runnable, boolean forceNewThread) {
		if (forceNewThread || isMain()) {
			executor.execute(runnable);
		} else {
			runnable.run();
		}
		
	}
	
	public static void runInBackground(Runnable runnable) {
		runInBackground(runnable, false);
	}
	
	public static Executor getExecutor() {
		return executor;
	}
	
	public static boolean isMain() {
		return Looper.myLooper() == Looper.getMainLooper();
	}
	
	public static <T> void postSuccess(final ResponseListener<T> listener, final T object) {
		if (listener == null)
			return;

		Util.runOnUI(new Runnable() {
			
			@Override
			public void run() {
				listener.onSuccess(object);
			}
		});
	}
	
	public static void postError(final ErrorListener listener, final ServiceCommandError error) {
		if (listener == null)
			return;

		Util.runOnUI(new Runnable() {
			
			@Override
			public void run() {
				listener.onError(error);
			}
		});
	}
	
	public static byte[] convertIpAddress(int ip) {
		return new byte[] {
				(byte) (ip & 0xFF), 
				(byte) ((ip >> 8) & 0xFF), 
				(byte) ((ip >> 16) & 0xFF), 
				(byte) ((ip >> 24) & 0xFF)};
	}
	
	public static long getTime() {
		return TimeUnit.MILLISECONDS.toSeconds(new Date().getTime());
	}
}