/*
 * KeyControl
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

package com.connectsdk.service.capability;

import com.connectsdk.service.capability.CapabilityMethods;
import com.connectsdk.service.capability.listeners.ResponseListener;

public interface KeyControl extends CapabilityMethods {
	public final static String Any = "KeyControl.Any";

	public final static String Up = "KeyControl.Up";
	public final static String Down = "KeyControl.Down";
	public final static String Left = "KeyControl.Left";
	public final static String Right = "KeyControl.Right";
	public final static String OK = "KeyControl.OK";
	public final static String Back = "KeyControl.Back";
	public final static String Home = "KeyControl.Home";
	public final static String Send_Key = "KeyControl.SendKey";

	public final static String[] Capabilities = {
		Up,
	    Down,
	    Left,
	    Right,
	    OK,
	    Back,
	    Home
	};

	public KeyControl getKeyControl();
	public CapabilityPriorityLevel getKeyControlCapabilityLevel();

	public void up(ResponseListener<Object> listener);
	public void down(ResponseListener<Object> listener);
	public void left(ResponseListener<Object> listener);
	public void right(ResponseListener<Object> listener);
	public void ok(ResponseListener<Object> listener);
	public void back(ResponseListener<Object> listener);
	public void home(ResponseListener<Object> listener);
	public void sendKeyCode(int keyCode, ResponseListener<Object> listener);
}
