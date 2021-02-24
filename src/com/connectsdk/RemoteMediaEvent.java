package com.connectsdk;

import java.util.Map;

public class RemoteMediaEvent {
    private String eventType;
    private Map<String, Object> properties;

    public RemoteMediaEvent(String eventType, Map<String, Object> properties) {
        this.eventType = eventType;
        this.properties = properties;
    }

    public String getEventType() {
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

    public Map<String, Object> getProperties() {
        return properties;
    }
}
