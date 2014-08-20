#Connect SDK Android
Connect SDK is an open source framework that connects your mobile apps with multiple TV platforms. Because most TV platforms support a variety of protocols, Connect SDK integrates and abstracts the discovery and connectivity between all supported protocols.

For more information, visit our [website](http://www.connectsdk.com/).

* [General information about Connect SDK](http://www.connectsdk.com/discover/)
* [Platform documentation & FAQs](http://www.connectsdk.com/docs/android/)
* [API documentation](http://www.connectsdk.com/apis/android/)

##Dependencies
This project has the following dependencies, some of which require manual setup. If you would like to use a version of the SDK which has no manual setup, consider using the [lite version](https://github.com/ConnectSDK/Connect-SDK-Android-Lite) of the SDK.

This project has the following dependencies.
* [Connect-SDK-Android-Core](https://github.com/ConnectSDK/Connect-SDK-Android-Core) submodule
* [Connect-SDK-Android-Google-Cast](https://github.com/ConnectSDK/Connect-SDK-Android-Google-Cast) submodule
  - Requires [GoogleCast.framework](https://developers.google.com/cast/docs/downloads)
* [Connect-SDK-Android-Samsung-MultiScreen](https://github.com/ConnectSDK/Connect-SDK-Android-Samsung-MultiScreen) submodule
  - Requires [SamsungMultiScreen.framework](http://multiscreen.samsung.com/downloads.html)
* [Java-WebSocket library](https://github.com/TooTallNate/Java-WebSocket)

##Including Connect SDK in your app

1. Clone repository (or download & unzip)
2. Set up the submodules by running the following commands in Terminal
   - `git submodule init`
   - `git submodule update`
3. Open Eclipse
4. Click File > Import
5. Select `Existing Android Code Into Workspace` and click Next
6. Browse to the Connect-SDK-Android project folder and click Open
7. Click Finish
8. Do the steps 4-7 for Connect-SDK-Android-Core which is located in `core` folder of the Connect-SDK-Android project
9. Do the steps 4-7 for Connect-SDK-Android-Google-Cast which is located in `modules/google_cast` folder of the Connect-SDK-Android project
10. Do the steps 4-7 for Connect-SDK-Android-Samsung-MultiScreen which is located in `modules/samsung_multiscreen` folder of the Connect-SDK-Android project
11. Follow the setup instructions for each of the service submodules
 - [Connect-SDK-Android-Google-Cast](https://github.com/ConnectSDK/Connect-SDK-Android-Google-Cast)
 - [Connect-SDK-Android-Samsung-MultiScreen](https://github.com/ConnectSDK/Connect-SDK-Android-Samsung-MultiScreen)
12. Right-click the Connect-SDK-Android-Core project and select Properties, in the Library pane of the Android tab, add Connect-SDK-Android
13. Right-click the Connect-SDK-Android-Google-Cast project and select Properties, in the Library pane of the Android tab, add Connect-SDK-Android-Core
14. Right-click the Connect-SDK-Android-Samsung-MultiScreen project and select Properties, in the Library pane of the Android tab, add Connect-SDK-Android-Core
15. In your project select Properties, in the Library pane of the Android tab, add Connect-SDK-Android-Core, Connect-SDK-Android-Google-Cast, and Connect-SDK-Android-Samsung-MultiScreen
15. Set up your manifest file as per the instructions below

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
