/*
 * MediaPlayer
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on Jan 19 2014
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
import com.connectsdk.service.sessions.LaunchSession;

public interface MediaPlayer extends CapabilityMethods {
	public final static String Any = "MediaPlayer.Any";

	public final static String Display_Image = "MediaPlayer.Display.Image";
	public final static String Display_Video = "MediaPlayer.Display.Video";
	public final static String Display_Audio = "MediaPlayer.Display.Audio";
	public final static String Close = "MediaPlayer.Close";
	public final static String MetaData_Title = "MediaControl.MetaData.Title";
	public final static String MetaData_Description = "MediaControl.MetaData.Description";
	public final static String MetaData_Thumbnail = "MediaControl.MetaData.Thumbnail";
	public final static String MetaData_MimeType = "MediaControl.MetaData.MimeType";

	public final static String[] Capabilities = {
	    Display_Image,
	    Display_Video,
	    Display_Audio, 
	    Close,
	    MetaData_Title,
	    MetaData_Description,
	    MetaData_Thumbnail,
	    MetaData_MimeType
	};

	public MediaPlayer getMediaPlayer();
	public CapabilityPriorityLevel getMediaPlayerCapabilityLevel();

	public void displayImage(String url, String mimeType, String title, String description, String iconSrc, LaunchListener listener);
	public void playMedia(String url, String mimeType, String title, String description, String iconSrc, boolean shouldLoop, LaunchListener listener);
	
	public void closeMedia(LaunchSession launchSession, ResponseListener<Object> listener);

	/**
	 * Success block that is called upon successfully playing/displaying a media file.
	 *
	 * Passes a MediaLaunchObject which contains the objects for controlling media playback.
	 */
	public static interface LaunchListener extends ResponseListener<MediaLaunchObject> { }
	
	/**
	 * Helper class used with the MediaPlayer.LaunchListener to return the current media playback.
	 */
	public static class MediaLaunchObject {
		/** The LaunchSession object for the media launched. */
		public LaunchSession launchSession;
		/** The MediaControl object for the media launched. */
		public MediaControl mediaControl;
		
		public MediaLaunchObject(LaunchSession launchSession, MediaControl mediaControl) {
			this.launchSession = launchSession;
			this.mediaControl = mediaControl;
		}
	}
}
