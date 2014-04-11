package com.connectsdk.core;

/** Normalized reference object for information about a TVs program. */
public class ProgramInfo {
	// @cond INTERNAL
	private String id;
	private String name;

	private ChannelInfo channelInfo;
	
	private Object rawData;
	// @endcond 

	/** Gets the ID of the program on the first screen device. Format is different depending on the platform. */
	public String getId() {
		return id;
	}

	/** Sets the ID of the program on the first screen device. Format is different depending on the platform. */
	public void setId(String id) {
		this.id = id;
	}

	/** Gets the user-friendly name of the program (ex. Sesame Street, Cosmos, Game of Thrones, etc). */
	public String getName() {
		return name;
	}

	/** Sets the user-friendly name of the program (ex. Sesame Street, Cosmos, Game of Thrones, etc). */
	public void setName(String name) {
		this.name = name;
	}

	/** Gets the reference to the ChannelInfo object that this program is associated with */
	public ChannelInfo getChannelInfo() {
		return channelInfo;
	}

	/** Sets the reference to the ChannelInfo object that this program is associated with */
	public void setChannelInfo(ChannelInfo channelInfo) {
		this.channelInfo = channelInfo;
	}

	/** Gets the raw data from the first screen device about the program. In most cases, this is an NSDictionary. */
	public Object getRawData() {
		return rawData;
	}

	/** Sets the raw data from the first screen device about the program. In most cases, this is an NSDictionary. */
	public void setRawData(Object rawData) {
		this.rawData = rawData;
	}
	
	/**
	 * Compares two ProgramInfo objects.
	 *
	 * @param programInfo ProgramInfo object to compare.
	 *
	 * @return true if both ProgramInfo id & name values are equal
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof ProgramInfo) {
			ProgramInfo pi = (ProgramInfo) o;
			return pi.id.equals(pi.id) &&
					pi.name.equals(pi.name);
		}
		return super.equals(o);
	}
}
