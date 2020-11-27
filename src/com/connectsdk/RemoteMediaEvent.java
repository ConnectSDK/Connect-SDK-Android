package com.connectsdk;

import java.util.Map;

public class RemoteMediaEvent {
    private MediaEventType eventType;
    private Map<String, Object> properties;

    public RemoteMediaEvent(MediaEventType eventType, Map<String, Object> properties) {
        this.eventType = eventType;
        this.properties = properties;
    }

    public MediaEventType getEventType() {
        return eventType;
    }

    public long getLong(String propertyName) {
        if (properties != null && properties.containsKey(propertyName))
            return (long) properties.get(propertyName);
        return 0;
    }

    public String getString(String propertyName) {
        if (properties != null && properties.containsKey(propertyName))
          return (String) properties.get(propertyName);
        return null;
    }
}
