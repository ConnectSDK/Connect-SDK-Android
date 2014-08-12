package com.connectsdk.core;

import java.util.ArrayList;
import java.util.List;

public class MediaInfo {

	public MediaInfo(String url, String mimeType, String title,
			String description) {
		super();
		this.url = url;
		this.mimeType = mimeType;
		this.title = title;
		this.description = description;
	}

	public MediaInfo(String url, String mimeType, String title,
			String description, List<ImageInfo> allImages) {
		this(url, mimeType, title, description);
		this.allImages = allImages;
	}

	private String url, mimeType, description, title;

	private List<ImageInfo> allImages;

	private long duration;

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

}
