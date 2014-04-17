/*
 * MediaControl
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

import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceSubscription;

public interface MediaControl extends CapabilityMethods {
	public final static String Any = "MediaControl.Any";

	public final static String Play = "MediaControl.Play";
	public final static String Pause = "MediaControl.Pause";
	public final static String Stop = "MediaControl.Stop";
	public final static String Rewind = "MediaControl.Rewind";
	public final static String FastForward = "MediaControl.FastForward";
	public final static String Seek = "MediaControl.Seek";
	public final static String Duration = "MediaControl.Duration";
	public final static String PlayState = "MediaControl.PlayState";
	public final static String PlayState_Subscribe = "MediaControl.PlayState.Subscribe";
	public final static String Position = "MediaControl.Position";

	public final static String[] Capabilities = {
	    Play,
	    Pause,
	    Stop,
	    Rewind,
	    FastForward,
	    Seek,
	    Duration,
	    PlayState,
	    PlayState_Subscribe,
	    Position,
	};
	
	public enum PlayStateStatus {
		Unknown, 
		Idle, 
		Playing, 
		Paused, 
		Buffering, 
		Finished
	};
	
	public MediaControl getMediaControl();
	public CapabilityPriorityLevel getMediaControlCapabilityLevel();

	public void play(ResponseListener<Object> listener);
	public void pause(ResponseListener<Object> listener);
	public void stop(ResponseListener<Object> listener);
	public void rewind(ResponseListener<Object> listener);
	public void fastForward(ResponseListener<Object> listener);

	public void seek(long position, ResponseListener<Object> listener);	
	public void getDuration(DurationListener listener);
	public void getPosition(PositionListener listener);	
	
	public void getPlayState(PlayStateListener listener);
	public ServiceSubscription<PlayStateListener> subscribePlayState(PlayStateListener listener);
	
	/**
	 * Success block that is called upon any change in a media file's play state.
	 *
	 * Passes a PlayStateStatus enum of the current media file
	 */
	public static interface PlayStateListener extends ResponseListener<PlayStateStatus> { }
	
	/**
	 * Success block that is called upon successfully getting the media file's current playhead position.
	 *
	 * Passes the position of the current playhead position of the current media file, in seconds
	 */
	public static interface PositionListener extends ResponseListener<Long> { }
	
	/**
	 * Success block that is called upon successfully getting the media file's duration.
	 *
	 * Passes the duration of the current media file, in seconds
	 */
	public static interface DurationListener extends ResponseListener<Long> { }
}
