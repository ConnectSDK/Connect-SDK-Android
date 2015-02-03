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
* [Java-WebSocket library](https://github.com/TooTallNate/Java-WebSocket)

##Including Connect SDK in your app with Android Studio
Edit your project's build.gradle to add this in the "dependencies" section
```groovy
dependencies {
    //...
    compile 'com.connectsdk:connect-sdk-android:1.4.+'
}
```
##Including Connect SDK in your app with Android Studio from sources
1. Open your terminal and execute these commands
    - cd your_project_folder
    - git clone https://github.com/ConnectSDK/Connect-SDK-Android.git
    - cd Connect-SDK-Android
    - git submodule init
    - git submodule update

2. On the root of your project directory create/modify the settings.gradle file. It should contain something like the following:
    ```groovy
    include ':app', ':Connect-SDK-Android'
    ```

3. Edit your project's build.gradle to add this in the "dependencies" section:
    ```groovy
    dependencies {
        //...
        compile project(':Connect-SDK-Android')
    }
    ```

4. Sync project with gradle files
5. Add permissions to your manifest

##Including Connect SDK in your app with Eclipse

1. Clone repository (or download & unzip)
2. Set up the submodules by running the following commands in Terminal
   - `git submodule init`
   - `git submodule update`
3. Open Eclipse
4. Click File > Import
5. Select `Existing Android Code Into Workspace` and click `Next`
6. Browse to the `Connect-SDK-Android` project folder and click `Open`
7. Check all projects and click `Finish`
8. Follow the setup instructions for each of the service submodules
   - [Connect-SDK-Android-Google-Cast](https://github.com/ConnectSDK/Connect-SDK-Android-Google-Cast)
9. Right-click the `Connect-SDK-Android-Core` project and select `Properties`, in the `Library` pane of the `Android` tab add
   - Connect-SDK-Android
10. Right-click the `Connect-SDK-Android-Google-Cast` project and select `Properties`, in the `Library` pane of the `Android` tab add following libraries
   - Connect-SDK-Android-Core
   - android-support-v7-appcompat
   - android-support-v7-mediarouter
   - google-play-services_lib
11. **IN YOUR PROJECT** select `Properties`, in the `Library` pane of the `Android` tab add following libraries
   - Connect-SDK-Android-Core
   - Connect-SDK-Android-Google-Cast
12. Set up your manifest file as per the instructions below

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

##Migrating from 1.3 to 1.4 release

1. Open terminal and go to your local Connect-SDK-Android repo
2. Pull the latest updates by running command `git pull` in Terminal
3. Set up the submodules by running the following commands in Terminal
   - `git submodule init`
   - `git submodule update`
4. Open Eclipse
5. Click `File > Import`
6. Select `Existing Android Code Into Workspace` and click `Next`
7. Browse to the `Connect-SDK-Android/core` folder and click `Open` to import core submodule
8. Click `Finish`
9. Do the steps 5-8 for Connect-SDK-Android-Google-Cast which is located in `Connect-SDK-Android/modules/google_cast` folder
10. Right click on `Connect-SDK-Android` project and select `Properties`, in the `Library` pane of the `Android` tab
   - remove all libraries references
11. Right-click the `Connect-SDK-Android-Core` project and select `Properties`, in the `Library` pane of the `Android` tab add
   - Connect-SDK-Android
12. Right-click the `Connect-SDK-Android-Google-Cast` project and select `Properties`, in the `Library` pane of the `Android` tab add following libraries
   - Connect-SDK-Android-Core
   - android-support-v7-appcompat
   - android-support-v7-mediarouter
   - google-play-services_lib
13. **IN YOUR PROJECT** select `Properties`, in the Library pane of the Android tab 
   - remove Connect-SDK-Android
   - add Connect-SDK-Android-Core
   - add Connect-SDK-Android-Google-Cast.

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
