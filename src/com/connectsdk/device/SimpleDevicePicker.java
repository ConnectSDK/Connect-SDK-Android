/*
 * SimpleDevicePicker
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics. All rights reserved.
 * Created by Jason Lai on 19 Jan 2014
 * 
 */

package com.connectsdk.device;

import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import com.connectsdk.core.Util;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.DeviceService.PairingType;
import com.connectsdk.service.command.ServiceCommandError;

// TODO: lets make this the default picker and get rid of the other one

/**
 * A helper class for selecting and connecting to devices using pre-made dialogs.
 * 
 * Requires a copy of connect_sdk_strings.xml to be added to your project.
 * 
 * Most methods MUST be called from the main ui thread.
 */
public class SimpleDevicePicker implements ConnectableDeviceListener {
	protected Activity activity;
	protected DevicePicker picker;
	protected Dialog pickerDialog;
	protected Dialog pairingDialog;
	
	protected ConnectableDevice currentDevice;
	
	protected int selectDeviceResId;
	protected int simplePairingTitleResId;
	protected int simplePairingPromptResId;
	protected int pinPairingPromptResId;
	protected int connectionFailedResId;
	
	OnSelectDeviceListener onSelectDeviceListener;
	OnDeviceReadyListener onDeviceReadyListener;
	
	public interface OnSelectDeviceListener {
		/**
		 * Called when the user selects a device.
		 * This callback can be used to prepare the device (request permissions, etc)
		 * just before attempting to connect.
		 * 
		 * @param device
		 */
		public void onSelectDevice(ConnectableDevice device);
	}
	
	public interface OnDeviceReadyListener {
		/**
		 * Called when device is ready to use (requested permissions approved).
		 * @param device
		 */
		public void onDeviceReady(ConnectableDevice device);
		
		/**
		 * Called when device is no longer ready to use.
		 * @param device
		 */
		public void onDeviceNotReady(ConnectableDevice device);
	}
	
	public SimpleDevicePicker(Activity activity) {
		this.activity = activity;
		this.picker = new DevicePicker(activity);
		
		loadStringIds();
	}
	
	public ConnectableDevice getCurrentDevice() {
		return currentDevice;
	}
	
	protected void loadStringIds() {
		selectDeviceResId = getStringId("connect_sdk_picker_select_device");
		simplePairingTitleResId = getStringId("connect_sdk_pairing_simple_title_tv");
		simplePairingPromptResId = getStringId("connect_sdk_pairing_simple_prompt_tv");
		pinPairingPromptResId = getStringId("connect_sdk_pairing_pin_prompt_tv");
		connectionFailedResId = getStringId("connect_sdk_connection_failed");
	}
	
	protected int getStringId(String key) {
		// First try to get resource from application
		int id = this.activity.getResources().getIdentifier(key, "string", activity.getPackageName());
		
		// Then try to get from Connect SDK library
		if (id == 0) {
			id = this.activity.getResources().getIdentifier(key, "string", "com.connectsdk");
		}
		
		if (id == 0) {
			Log.w("ConnectSDK", "missing string resource for \"" + key + "\"");
			
			throw new Resources.NotFoundException(key);
		}
		
		return id;
	}
	
	// TODO: break this out into a separate DevicePickerListener class
	// See: https://github.com/webOS-DevRel/2nd-Screen-iOS-SDK-Source/blob/sdk_1.1/ConnectSDK/Devices/DevicePickerDelegate.h
	public void setOnSelectDeviceListener(OnSelectDeviceListener listener) {
		onSelectDeviceListener = listener;
	}
	
	public void setOnDeviceReadyListener(OnDeviceReadyListener listener) {
		onDeviceReadyListener = listener;
	}
	
	public void showPicker() {
		hidePicker();
		
		pickerDialog = picker.getPickerDialog(activity.getString(selectDeviceResId), new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int pos,
					long id) {
				ConnectableDevice device = (ConnectableDevice) adapter.getItemAtPosition(pos);
				
				selectDevice(device);
			}
		});
		
		pickerDialog.show();
	}
	
	public void hidePicker() {
		if (pickerDialog != null) {
			pickerDialog.dismiss();
			pickerDialog = null;
		}
	}
	
	/**
	 * Connect to a device
	 * 
	 * @param device
	 */
	public void selectDevice(ConnectableDevice device) {
		if (currentDevice != null) {
			currentDevice.removeListener(this);
		}
		
		currentDevice = device;
		
		if (device != null) {
			device.addListener(this);
			
			if (onSelectDeviceListener != null) {
				onSelectDeviceListener.onSelectDevice(device);
			}
			
			device.connect();
		}
	}
	
	protected Dialog createSimplePairingDialog() {
		PairingDialog dialog = new PairingDialog(activity, currentDevice);
		return dialog.getSimplePairingDialog(simplePairingTitleResId, simplePairingPromptResId);
	}
	
	protected Dialog createPinPairingDialog() {
		PairingDialog dialog = new PairingDialog(activity, currentDevice);
		return dialog.getPairingDialog(pinPairingPromptResId);
	}
	
	protected void showPairingDialog(PairingType pairingType) {
		switch (pairingType) { 
			case FIRST_SCREEN:
				pairingDialog = createSimplePairingDialog();
				break;
				
			case PIN_CODE:
				pairingDialog = createPinPairingDialog();
				break;
			
			case NONE:
			default:
				break;
		}
		
		if (pairingDialog != null) {
			pairingDialog.show();
		}
	}
	
	/**
	 * Hide the current pairing dialog and cancels the pairing attempt.
	 */
	public void hidePairingDialog() {
		// cancel pairing
		if (pairingDialog != null) {
			pairingDialog.dismiss();
			pairingDialog = null;
		}
	}
	

	@Override
	public void onDeviceReady(ConnectableDevice device) {
		Util.runOnUI(new Runnable() {
			@Override
			public void run() {
				hidePairingDialog();
				
				if (onDeviceReadyListener != null) {
					onDeviceReadyListener.onDeviceReady(currentDevice);
				}
			}
		});		
	}

	@Override
	public void onDeviceDisconnected(ConnectableDevice device) {
		Util.runOnUI(new Runnable() {
			@Override
			public void run() {
				if (onDeviceReadyListener != null) {
					onDeviceReadyListener.onDeviceNotReady(currentDevice);
				}
			}
		});
	}

	@Override
	public void onCapabilityUpdated(ConnectableDevice device, List<String> added, List<String> removed) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionFailed(ConnectableDevice device, ServiceCommandError error) {
		Util.runOnUI(new Runnable() {
			@Override
			public void run() {
				hidePairingDialog();
				
				if (onDeviceReadyListener != null) {
					onDeviceReadyListener.onDeviceNotReady(currentDevice);
				}
				
				Toast.makeText(activity, connectionFailedResId, Toast.LENGTH_SHORT).show();
			}
		});
	}

	@Override
	public void onPairingRequired(ConnectableDevice device, DeviceService service, final PairingType pairingType) {
		Log.d("SimpleDevicePicker", "pairing required for device " + currentDevice.getFriendlyName());
		
		Util.runOnUI(new Runnable() {
			@Override
			public void run() {
				showPairingDialog(pairingType);
			}
		});
	}

}
