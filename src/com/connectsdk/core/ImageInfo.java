package com.connectsdk.core;


public class ImageInfo {


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

	private enum ImageType {
		Thumb, Video_Poster, Album_Art, Unknown;
	}

	private String url;
	private ImageType type;
	private int width;
	private int height;

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
	

