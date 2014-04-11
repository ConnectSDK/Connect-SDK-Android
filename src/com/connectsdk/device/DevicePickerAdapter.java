/*
 * DevicePickerAdaper
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics. All rights reserved.
 * Created by Hyun Kook Khang on 19 Jan 2014
 * 
 */

package com.connectsdk.device;

import java.util.HashMap;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


public class DevicePickerAdapter extends ArrayAdapter<ConnectableDevice> {
	int resource, textViewResourceId;
	HashMap<String, ConnectableDevice> currentDevices = new HashMap<String, ConnectableDevice>();
	
	DevicePickerAdapter(Context context) {
		this(context, android.R.layout.simple_list_item_1);
	}
	
	DevicePickerAdapter(Context context, int resource) {
		this(context, resource, android.R.id.text1);
	}
	
	DevicePickerAdapter(Context context, int resource, int textViewResourceId) {
		super(context, resource, textViewResourceId);
		this.resource = resource;
		this.textViewResourceId = textViewResourceId;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		
		if (convertView == null) {
			view = View.inflate(getContext(), resource, null);
		}
		
		ConnectableDevice device = this.getItem(position);
		String text;
		if ( device.getFriendlyName() != null ) {
			text = device.getFriendlyName();
		}
		else {
			text = device.getModelName();
		}

		view.setBackgroundColor(Color.BLACK);
		
		TextView textView = (TextView) view.findViewById(textViewResourceId);
		textView.setText(text);
		textView.setTextColor(Color.WHITE);
		
		return view;
	}
}
