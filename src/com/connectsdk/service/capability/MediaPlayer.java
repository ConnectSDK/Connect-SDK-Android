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

import java.util.ArrayList;
import java.util.List;

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
	public static class MediaInfo {

		public MediaInfo(String url, String mimeType, String title, String description,
			 List<ImageInfo> allImages) {
			super();
			this.url = url;
			this.mimeType = mimeType;
			this.title = title;
			this.description = description;
			this.allImages = allImages;
		}

		public MediaInfo(String url, String mimeType, String title, String description) {
			super();
			this.url = url;
			this.mimeType = mimeType;
			this.title = title;
			this.description = description;
		}
		
		public String url, mimeType, description, title;

		public List<ImageInfo> allImages;

		public long duration;

		public String getMimeType() {
			return mimeType;
		}

		public void setMimeType(String mimeType) {
			this.mimeType = mimeType;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public List<ImageInfo> getImages() {
			return allImages;
		}

		public void setImages(List<ImageInfo> images) {
			this.allImages = images;
		}

		public long getDuration() {
			return duration;
		}

		public void setDuration(long duration) {
			this.duration = duration;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public void addImages(ImageInfo... images) {

			List<ImageInfo> list = new ArrayList<ImageInfo>();
			for (int i = 0; i < images.length; i++) {
				list.add(images[i]);
			}

			this.setImages(list);

		}

		public static class ImageInfo {

			public ImageInfo(String url, ImageType type, int width, int height) {
				super();
				this.url = url;
				this.type = type;
				this.width = width;
				this.height = height;
			}
			
			public ImageInfo(String url) {
				super();
				this.url = url;
			}

			public enum ImageType {
				Thumb, Video_Poster, Album_Art, Unknown;
			}

			public String url;
			public ImageType type;
			public int width;
			public int height;

			public String getUrl() {
				return url;
			}

			public void setUrl(String url) {
				this.url = url;
			}

			public ImageType getType() {
				return type;
			}

			public void setType(ImageType type) {
				this.type = type;
			}

			public int getWidth() {
				return width;
			}

			public void setWidth(int width) {
				this.width = width;
			}

			public int getHeight() {
				return height;
			}

			public void setHeight(int height) {
				this.height = height;
			}

		}

	}

	void displayImage(MediaInfo mediaInfo, LaunchListener listener);

	void playMedia(MediaInfo mediaInfo, boolean shouldLoop,
			LaunchListener listener);

}
