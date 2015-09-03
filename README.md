#Connect SDK Android

[![Build Status](https://travis-ci.org/ConnectSDK/Connect-SDK-Android.svg)](https://travis-ci.org/ConnectSDK/Connect-SDK-Android)
[![Code Coverage](https://img.shields.io/codecov/c/github/ConnectSDK/Connect-SDK-Android/dev.svg)](https://codecov.io/github/ConnectSDK/Connect-SDK-Android)
[![Maven Central](http://img.shields.io/maven-central/v/com.connectsdk/connect-sdk-android.svg)](http://search.maven.org/#artifactdetails|com.connectsdk|connect-sdk-android|1.6.0|aar)
[![Apache License, 2.0](https://img.shields.io/github/license/ConnectSDK/Connect-SDK-Android.svg)](https://github.com/ConnectSDK/Connect-SDK-Android/blob/master/LICENSE)
[![Twitter](https://img.shields.io/badge/twitter-@ConnectSDK-blue.svg)](https://twitter.com/connectsdk)

Connect SDK is an open source framework that connects your mobile apps with multiple TV platforms. Because most TV platforms support a variety of protocols, Connect SDK integrates and abstracts the discovery and connectivity between all supported protocols.
This project can be built in Android Studio or directly with Gradle. Eclipse IDE is not supported since 1.5.0 version.

For more information, visit our [website](http://www.connectsdk.com/).

* [General information about Connect SDK](http://www.connectsdk.com/discover/)
* [Platform documentation & FAQs](http://www.connectsdk.com/docs/android/)
* [API documentation](http://www.connectsdk.com/apis/android/)

##Dependencies
This project has the following dependencies, some of which require manual setup. If you would like to use a version of the SDK which has no manual setup, consider using the [lite version](https://github.com/ConnectSDK/Connect-SDK-Android-Lite) of the SDK.

This project has the following dependencies.
* [Connect-SDK-Android-Core](https://github.com/ConnectSDK/Connect-SDK-Android-Core) submodule
  - Requires [Java-WebSocket library](https://github.com/TooTallNate/Java-WebSocket)
  - Requires [jmDNS library](https://github.com/openhab/jmdns)
* [Connect-SDK-Android-Google-Cast](https://github.com/ConnectSDK/Connect-SDK-Android-Google-Cast) submodule
  - Requires [GoogleCast.framework](https://developers.google.com/cast/docs/downloads)
* [Connect-SDK-Android-FireTV](https://github.com/ConnectSDK/Connect-SDK-Android-FireTV) submodule
  - Requires [AmazonFling.framework](https://developer.amazon.com/public/apis/experience/fling/docs/amazon-fling-sdk-download)

##Including Connect SDK in your app with Android Studio
Edit your project's build.gradle to add this in the "dependencies" section
```groovy
dependencies {
    //...
    compile 'com.connectsdk:connect-sdk-android:1.6.0'
}
```
This prebuilt library doesn't have Amazon Fling SDK support, because it’s not available on maven. You need to set the project up from sources
if you want to have Amazon Fling SDK support.

##Including Connect SDK in your app with Android Studio from sources
1. Open your terminal and execute these commands
    ```
    cd your_project_folder
    git clone https://github.com/ConnectSDK/Connect-SDK-Android.git
    cd Connect-SDK-Android
    git submodule update --init
    ```

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

4. Setup [FireTV submodule](https://github.com/ConnectSDK/Connect-SDK-Android-FireTV)
5. Sync project with gradle files
6. Add permissions to your manifest

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

###Tests
Connect SDK has unit tests for some parts of the code, and we are continuing to increase the test coverage.
These tests are based on third party libraries such as Robolectric, Mockito and PowerMock. You can easily run these tests with Gradle:
```
gradle test
```
Also the project has a target for generating test coverage report with Jacoco. Use this command for generating it.
```
gradle jacocoTestReport
```
The test coverage report will be in this folder `Connect-SDK-Android/build/reports/jacoco/jacocoTestReport/html`.

##Limitations/Caveats

###Subtitles

- DLNA service support `SRT` format only. Since there is no official specification for them, subtitles may not work on all DLNA-compatible devices. This feature has been tested and works on LG WebOS and Netcast TVs.
- FireTV service supports `WebVTT` format only. Subtitles on Fire TV are hidden by default. To display them, the user should manually pick one in the media player (click the "Options" button on the remote). The Fling SDK doesn't provide any way to make them appear remotely.
- Google Cast service supports `WebVTT` format only. Servers providing subtitles and media files should support CORS headers, otherwise they are not displayed. The simplest change is to send this HTTP response header for your subtitles: `Access-Control-Allow-Origin: *`. More information is here: [https://developers.google.com/cast/docs/android_sender#cors-requirements](https://developers.google.com/cast/docs/android_sender#cors-requirements).
- Netcast service support `SRT` format only. It uses DLNA and has the same restrictions as DLNA service.
- WebOS service supports `WebVTT` format only. The server providing subtitles should support CORS headers, similarly to Cast service's requirements.



##Contact
* Twitter [@ConnectSDK](https://www.twitter.com/ConnectSDK)
* Ask a question on Stack Overflow with the [Connect-SDK tag](https://stackoverflow.com/tags/connect-sdk) (or [TV tag](https://stackoverflow.com/tags/tv))
* General Inquiries info@connectsdk.com
* Developer Support support@connectsdk.com
* Partnerships partners@connectsdk.com

##Credits
Connect SDK for Android makes use of the following projects, some of which are open-source.

* [Amazon Fling SDK](https://developer.amazon.com/fling)
  - [Amazon Fling SDK Terms of Service](https://developer.amazon.com/public/support/pml.html)
* [Android-DLNA](https://code.google.com/p/android-dlna/) (Apache License, Version 2.0)
* [Google Cast SDK](https://developers.google.com/cast/)
  - [Google Cast SDK Additional Developer Terms of Service](https://developers.google.com/cast/docs/terms)
  - [Google APIs Terms of Service](https://developers.google.com/terms/)
* [Java-WebSocket](https://github.com/TooTallNate/Java-WebSocket) (MIT)
* [JmDNS](http://jmdns.sourceforge.net) (Apache License, Version 2.0)

These projects are used in tests:
* [Mockito](http://mockito.org/) (MIT)
* [Robolectric](http://robolectric.org) (MIT)
* [PowerMock](https://github.com/jayway/powermock) (Apache License, Version 2.0)
* [XMLUnit](http://www.xmlunit.org/) (Apache License, Version 2.0)

##License
Copyright (c) 2013-2015 LG Electronics.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

> http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
