/*
 * MediaInfo
 * Connect SDK
 *
 * Copyright (c) 2014 LG Electronics.
 * Created by Simon Gladkoskok on 14 August 2014
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

package com.connectsdk.core;

import java.util.ArrayList;

/**
 * Normalized reference object for information about a media to display. This object can be used to pass as a parameter to displayImage or playMedia.
 * 
 */

import java.util.List;

public class MediaInfo {

	/**
	 * Default constructor method.
	 */

	public MediaInfo(String url, String mimeType, String title,
			String description) {
		super();
		this.url = url;
		this.mimeType = mimeType;
		this.title = title;
		this.description = description;
	}

	/**
	 * Default constructor method.
	 * 
	 * @param allImages
	 *            list of imageInfo objects where [0] is icon, [1] is poster
	 */

	public MediaInfo(String url, String mimeType, String title,
			String description, List<ImageInfo> allImages) {
		this(url, mimeType, title, description);
		this.allImages = allImages;
	}

	// @cond INTERNAL
	private String url, mimeType, description, title;

	private List<ImageInfo> allImages;

	private long duration;

	// @endcond

	/**
	 * Gets type of a media file.
	 * 
	 * 
	 */

	public String getMimeType() {
		return mimeType;
	}

	/**
	 * Sets type of a media file.
	 * 
	 * 
	 */

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	/**
	 * Gets title for a media file.
	 * 
	 * 
	 */

	public String getTitle() {
		return title;
	}

	/**
	 * Sets title of a media file.
	 * 
	 * 
	 */

	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Gets description for a media.
	 * 
	 */

	public String getDescription() {
		return description;
	}

	/**
	 * Sets description for a media.
	 * 
	 */

	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * Gets list of ImageInfo objects for images representing a media (ex. icon, poster). Where first ([0]) is icon image, and second ([1]) is poster image. 
	 */

	public List<ImageInfo> getImages() {
		return allImages;
	}

	/**
	 * Sets list of ImageInfo objects for images representing a media (ex. icon, poster). Where first ([0]) is icon image, and second ([1]) is poster image. 

	 * 
	 */
	
	public void setImages(List<ImageInfo> images) {
		this.allImages = images;
	}
	
	/**
	 * Gets duration of a media file.
	 * 
	 */

	public long getDuration() {
		return duration;
	}
	
	/**
	 * Sets duration of a media file.
	 * 
	 */

	public void setDuration(long duration) {
		this.duration = duration;
	}
	
	/**
	 * Gets URL address of a media file.
	 * 
	 */

	public String getUrl() {
		return url;
	}
	
	/**
	 * Sets URL address of a media file.
	 * 
	 */

	public void setUrl(String url) {
		this.url = url;
	}
	
	/**
	 * Stores ImageInfo objects.
	 * 
	 */

	public void addImages(ImageInfo... images) {

		List<ImageInfo> list = new ArrayList<ImageInfo>();
		for (int i = 0; i < images.length; i++) {
			list.add(images[i]);
		}

		this.setImages(list);

	}

}
