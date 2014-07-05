/*
 * Device
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Copyright (c) 2011 stonker.lee@gmail.com https://code.google.com/p/android-dlna/
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

package com.connectsdk.core.upnp;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.connectsdk.core.upnp.service.Service;
import com.connectsdk.core.upnp.ssdp.SSDP;

public class Device {
    public static final String TAG = "device";
    public static final String TAG_DEVICE_TYPE = "deviceType";
    public static final String TAG_FRIENDLY_NAME = "friendlyName";
    public static final String TAG_MANUFACTURER = "manufacturer";
    public static final String TAG_MANUFACTURER_URL = "manufacturerURL";
    public static final String TAG_MODEL_DESCRIPTION = "modelDescription";
    public static final String TAG_MODEL_NAME = "modelName";
    public static final String TAG_MODEL_NUMBER = "modelNumber";
    public static final String TAG_MODEL_URL = "modelURL";
    public static final String TAG_SERIAL_NUMBER = "serialNumber";
    public static final String TAG_UDN = "UDN";
    public static final String TAG_UPC = "UPC";
    public static final String TAG_ICON_LIST = "iconList";
    public static final String TAG_SERVICE_LIST = "serviceList";
    
    public static final String HEADER_SERVER = "Server";
	
    /* Required. UPnP device type. */
    public String deviceType;
    /* Required. Short description for end user. */
    public String friendlyName;
    /* Required. Manufacturer's name. */
    public String manufacturer;
    /* Optional. Web site for manufacturer. */
    public String manufacturerURL;
    /* Recommended. Long description for end user. */
    public String modelDescription;
    /* Required. Model name. */
    public String modelName;
    /* Recommended. Model number. */
    public String modelNumber;
    /* Optional. Web site for model. */
    public String modelURL;
    /* Recommended. Serial number. */
    public String serialNumber;
    /* Required. Unique Device Name. */
    public String UDN;
    /* Optional. Universal Product Code. */
    public String UPC;
    /* Required. */
    List<Icon> iconList = new ArrayList<Icon>();
    public String locationXML;
    /* Optional. */
    public List<Service> serviceList = new ArrayList<Service>();
    public String searchTarget;
    public String applicationURL;

    public String baseURL;
    public String ipAddress;
    public int port;
    public String UUID;
    
    public Map<String, List<String>> headers;
	    
	public Device(String url, String searchTarget) throws IOException {
    	URL urlObject = new URL(url);

    	if (urlObject.getPort() == -1) {
    		baseURL = String.format("%s://%s", urlObject.getProtocol(), urlObject.getHost());
    	} else {
    		baseURL = String.format("%s://%s:%d", urlObject.getProtocol(), urlObject.getHost(), urlObject.getPort());
    	}
    	ipAddress = urlObject.getHost();
    	port = urlObject.getPort();
    	this.searchTarget = searchTarget;
    	UUID = null;

    	if ( searchTarget.equalsIgnoreCase("urn:dial-multiscreen-org:service:dial:1") )
    		applicationURL = getApplicationURL(url);
    }
	    
    public static Device createInstanceFromXML(String url, String searchTarget) {
    	Device newDevice = null;
    	try {
    		newDevice = new Device(url, searchTarget);
    	} catch(IOException e) {
    		return null;
    	}
    	
    	final Device device = newDevice;
        
        DefaultHandler dh = new DefaultHandler() {
            String currentValue = null;
            Icon currentIcon;
            Service currentService;
            
            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                if (currentValue == null) {
                    currentValue = new String(ch, start, length);
                } else {
                    // append to existing string (needed for parsing character entities)
                    currentValue += new String(ch, start, length);
                }
            }
            
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                if (Icon.TAG.equals(qName)) {
                    currentIcon = new Icon();
                } else if (Service.TAG.equals(qName)) {
                    currentService = new Service();
                    currentService.baseURL = device.baseURL;
                }
                currentValue = null;
            }
            
            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
//            	System.out.println("[DEBUG] qName: " + qName + ", currentValue: " + currentValue);
            	/* Parse device-specific information */
                if (TAG_DEVICE_TYPE.equals(qName)) {
                    device.deviceType = currentValue;
                } else if (TAG_FRIENDLY_NAME.equals(qName)) {
                    device.friendlyName = currentValue;
                } else if (TAG_MANUFACTURER.equals(qName)) {
                    device.manufacturer = currentValue;
                } else if (TAG_MANUFACTURER_URL.equals(qName)) {
                    device.manufacturerURL = currentValue;
                } else if (TAG_MODEL_DESCRIPTION.equals(qName)) {
                    device.modelDescription = currentValue;
                } else if (TAG_MODEL_NAME.equals(qName)) {
                    device.modelName = currentValue;
                } else if (TAG_MODEL_NUMBER.equals(qName)) {
                    device.modelNumber = currentValue;
                } else if (TAG_MODEL_URL.equals(qName)) {
                    device.modelURL = currentValue;
                } else if (TAG_SERIAL_NUMBER.equals(qName)) {
                    device.serialNumber = currentValue;
                } else if (TAG_UDN.equals(qName)) {
                    device.UDN = currentValue;
                    
//                	device.UUID = Device.parseUUID(currentValue);
                } else if (TAG_UPC.equals(qName)) {
                    device.UPC = currentValue;
                }
                /* Parse icon-list information */
                else if (Icon.TAG_MIME_TYPE.equals(qName)) {
                    currentIcon.mimetype = currentValue;
                } else if (Icon.TAG_WIDTH.equals(qName)) {
                    currentIcon.width = currentValue;
                } else if (Icon.TAG_HEIGHT.equals(qName)) {
                    currentIcon.height = currentValue;
                } else if (Icon.TAG_DEPTH.equals(qName)) {
                    currentIcon.depth = currentValue;
                } else if (Icon.TAG_URL.equals(qName)) {
                    currentIcon.url = currentValue;
                } else if (Icon.TAG.equals(qName)) {
                    device.iconList.add(currentIcon);
                }
                /* Parse service-list information */
                else if (Service.TAG_SERVICE_TYPE.equals(qName)) {
                    currentService.serviceType = currentValue;
                } else if (Service.TAG_SERVICE_ID.equals(qName)) {
                    currentService.serviceId = currentValue;
                } else if (Service.TAG_SCPD_URL.equals(qName)) {
                    currentService.SCPDURL = currentValue;
                } else if (Service.TAG_CONTROL_URL.equals(qName)) {
                    currentService.controlURL = currentValue;
                } else if (Service.TAG_EVENTSUB_URL.equals(qName)) {
                    currentService.eventSubURL = currentValue;
                } else if (Service.TAG.equals(qName)) {
                    device.serviceList.add(currentService);
                }

                currentValue = null;
            }
        };
        
        SAXParserFactory factory = SAXParserFactory.newInstance();
        
        SAXParser parser;
        try {
        	URL mURL = new URL(url);
        	URLConnection urlConnection = mURL.openConnection();
        	InputStream in = new BufferedInputStream(urlConnection.getInputStream());
        	try {
            	Scanner s = new Scanner(in).useDelimiter("\\A");
            	device.locationXML = s.hasNext() ? s.next() : "";

            	parser = factory.newSAXParser();
            	parser.parse(new ByteArrayInputStream(device.locationXML.getBytes()), dh);
        	} finally {
        		in.close();
        	}
        	
        	device.headers = urlConnection.getHeaderFields();
        	
            return device;
        } catch (MalformedURLException e) {
        	e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
        	e.printStackTrace();
        }
        
        return null;
    }
    
    private String getApplicationURL(String url) {
    	HttpClient client = new DefaultHttpClient();

    	HttpGet get = new HttpGet(url);
    	
    	String applicationURL = null;

		try {
			HttpResponse response = client.execute(get);
			
			int code = response.getStatusLine().getStatusCode();
			
			if ( code == 200 ) {
				if ( response.getFirstHeader(SSDP.APPLICATION_URL) != null ) {
					applicationURL =  response.getFirstHeader(SSDP.APPLICATION_URL).getValue();

					if (!applicationURL.substring(applicationURL.length() - 1).equals("/")) {
						applicationURL = applicationURL.concat("/");
					}
				}
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}    	
    	return applicationURL;
    }
    
    protected static String parseUUID(String str) {
    	String uuidColon = "uuid:";
    	String colonColon = "::";
    	if (str == null)
    		return "";

    	int start = str.indexOf(uuidColon);
    	
    	if ( start != -1 ) {
    		start += uuidColon.length();
        	int end = str.indexOf(colonColon);
        	if ( end != -1 ) 
        		return str.substring(start, end);
        	else
        		return str.substring(start);
    	}
    	else {
    		return str;
    	}
    }
    
    @Override
    public String toString() {
        return friendlyName;
    }
    
	static class Icon {
	    static final String TAG = "icon";
	    static final String TAG_MIME_TYPE = "mimetype";
	    static final String TAG_WIDTH = "width";
	    static final String TAG_HEIGHT = "height";
	    static final String TAG_DEPTH = "depth";
	    static final String TAG_URL = "url";
	
	    /* Required. Icon's MIME type. */
	    String mimetype;
	    /* Required. Horizontal dimension of icon in pixels. */
	    String width;
	    /* Required. Vertical dimension of icon in pixels. */
	    String height;
	    /* Required. Number of color bits per pixel. */
	    String depth;
	    /* Required. Pointer to icon image. */
	    String url;
	}
}
