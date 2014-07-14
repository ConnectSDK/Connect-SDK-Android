#Connect SDK Android
Connect SDK is an open source framework that unifies device discovery and connectivity by providing one set of methods that work across multiple television platforms and protocols.

For more information, visit our [website](http://www.connectsdk.com/).

* [General information about Connect SDK](http://www.connectsdk.com/discover/)
* [Platform documentation & FAQs](http://www.connectsdk.com/docs/android/)
* [API documentation](http://www.connectsdk.com/apis/android/)

##Dependencies
This project has the following dependencies.
* [Java-WebSocket library](https://github.com/TooTallNate/Java-WebSocket)
* [Android Support v7 Libraries](https://developer.android.com/tools/support-library/setup.html)
  - appcompat
  - mediarouter
* [Google Play Services](http://developer.android.com/google/play-services/setup.html)

##Including Connect SDK in your app

1. Setup up your dependencies, listed above
2. Clone Connect-SDK-Android project (or download & unzip)
3. Open Eclipse
4. Click File > Import
5. Select `Existing Android Code Into Workspace` and click Next
6. Browse to the Connect-SDK-Android project folder and click Open
7. Click Finish
8. Right-click the Connect-SDK-Android project and select Properties
9. In the Library pane of the Android tab, add the following library references
   - android-support-v7-appcompat
   - android-support-v7-mediarouter
   - google-play-services_lib
10. **You must update these libraries to API 10 in their manifest.**
11. Click OK
12. Right-click your project and select Properties
13. In the Library pane of the Android tab, add the Connect-SDK-Android project
14. Set up your manifest file as per the instructions below

###Permissions to include in manifest
* Required for SSDP & Chromecast/Zeroconf discovery
 - `android.permission.INTERNET`
 - `android.permission.CHANGE_WIFI_MULTICAST_STATE`
* Required for interacting with devices
 - `android.permission.ACCESS_NETWORK_STATE`
 - `android.permission.ACCESS_WIFI_STATE`
* Required for storing device pairing information
 - `android.permission.WRITE_EXTERNAL_STORAGE`

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
<uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

###Metadata for application tag
This metadata tag is necessary to enable Chromecast support.

```xml
<application ... >
    ...
    
    <meta-data
        android:name="com.google.android.gms.version"
        android:value="@integer/google_play_services_version" />
        
</application>
```

###Proguard configuration
Add the following line to your proguard configuration file (otherwise `DiscoveryManager` won't be able to set any `DiscoveryProvider`).

```
-keep class com.connectsdk.**       { * ; }
```

##Contact
* Twitter [@ConnectSDK](https://www.twitter.com/ConnectSDK)
* Ask a question with the "tv" tag on [Stack Overflow](http://stackoverflow.com/tags/tv)
* General Inquiries info@connectsdk.com
* Developer Support support@connectsdk.com
* Partnerships partners@connectsdk.com

##Credits
Connect SDK for Android makes use of the following open-source projects.

* [Java-WebSocket](https://github.com/TooTallNate/Java-WebSocket) (MIT)
* [JmDNS](http://jmdns.sourceforge.net) (Apache License, Version 2.0)
* [Android-DLNA](https://code.google.com/p/android-dlna/) (Apache License, Version 2.0)

##License
Copyright (c) 2013-2014 LG Electronics.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

> http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
