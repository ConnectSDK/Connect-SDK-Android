# Connect SDK Android Changelog

## 1.4.3 -- 23 Mar 2015

- Reverted Roku 6.1 fix for playing video as Roku has fixed its media player
- Added proper encoding of special characters for metadata in DLNAService
- Added getPlayState implementation into AirPlayService
- Implemeted sending number key for NetcastTV
- Miscellaneous bug fixes

- [See commits between 1.4.2 and 1.4.3](https://github.com/ConnectSDK/Connect-SDK-Android/compare/1.4.2...1.4.3)

[View files at version 1.4.3](https://github.com/ConnectSDK/Connect-SDK-Android/tree/1.4.3)

## 1.4.2 -- 10 Feb 2015

- Fixed saving service configuration
- Fixed video beaming for Roku firmware 6.1
- Added playlist capabilities
- Significantly improved SSDP classes
- Improved usage of CPU for WebOS service
- Added support for Xbox one and Sonos speakers
- Added support for Android Studio 1.0
- [See commits between 1.4.1 and 1.4.2](https://github.com/ConnectSDK/Connect-SDK-Android/compare/1.4.1...1.4.2)

[View files at version 1.4.2](https://github.com/ConnectSDK/Connect-SDK-Android/tree/1.4.2)

## 1.4.1 -- 18 Dec 2014

- Fixed connection failure event
- Added new unit tests
- [See commits between 1.4.0 and 1.4.1](https://github.com/ConnectSDK/Connect-SDK-Android/compare/1.4.0...1.4.1)

[View files at version 1.4.1](https://github.com/ConnectSDK/Connect-SDK-Android/tree/1.4.1)

## 1.4.0 -- 3 Dec 2014

- Modularized project to allow easy exclusion of modules that have heavy and/or external dependencies
- Improved support for DLNA devices
  - DLNA volume control subscriptions
  - DLNA play state subscriptions
  - DLNA media info
- Unit tests for the discovery services providers
- Miscellaneous bug fixes
- [See commits between 1.3.2 and 1.4.0](https://github.com/ConnectSDK/Connect-SDK-Android/compare/1.3.2...1.4.0)

[View files at version 1.4.0](https://github.com/ConnectSDK/Connect-SDK-Android/tree/1.4.0)

## 1.3.2 -- 6 Aug 2014

- Added launchYouTube(String contentId, float startTime, AppLaunchListener listener) method to Launcher capability
- Decoupled Netcast and DLNA services
- Miscellaneous bug fixes
- [See commits between 1.3.1 and 1.3.2](https://github.com/ConnectSDK/Connect-SDK-Android/compare/1.3.1...1.3.2)

[View files at version 1.3.2](https://github.com/ConnectSDK/Connect-SDK-Android/tree/1.3.2)

## 1.3.1 -- 14 July 2014

- Significant performance fixes with regards to Zeroconf discovery
- Miscellaneous bug fixes
- [See commits between 1.3.0 and 1.3.1](https://github.com/ConnectSDK/Connect-SDK-Android/compare/1.3.0...1.3.1)

[View files at version 1.3.1](https://github.com/ConnectSDK/Connect-SDK-Android/tree/1.3.1)

## 1.3.0 -- 23 June 2014

- Added support for Apple TV
 + Supports media playback & control via HTTP requests
- Added ZeroconfDiscoveryProvider for discovery of devices over mDNS/Bonjour/Zeroconf
 + Only used for AirPlay devices currently, but can be used for discovery of other devices over Zeroconf
- Improved stability of web app capabilities on webOS
- Improved support for different versions of LG webOS
- Significant improvement in discovery due to change in Connectable Device Store
- Miscellaneous bug fixes
- Resolved numerous crashing bugs
- [See commits between 1.2.1 and 1.3.0](https://github.com/ConnectSDK/Connect-SDK-Android/compare/1.2.1...1.3.0)

[View files at version 1.3.0](https://github.com/ConnectSDK/Connect-SDK-Android/tree/1.3.0)

## 1.2.1 -- 14 May 2014

- Fixed numerous issues with Connectable Device Store
- Added ability to probe for app presence on Roku & DIAL
 + Capability will be added to device named "Launcher.X", where X is your DIAL/Roku app name
- Fixed some issues with launching apps via DIAL on non-LG devices
- Resolved numerous crashing bugs
- Miscellaneous bug fixes
- [See commits between 1.2.0 and 1.2.1](https://github.com/ConnectSDK/Connect-SDK-Android/compare/1.2.0...1.2.1)

[View files at version 1.2.1](https://github.com/ConnectSDK/Connect-SDK-Android/tree/1.2.1)

## 1.2.0 -- 17 Apr 2014

- Initial release, includes support for
 + Chromecast
 + DIAL
 + Roku
 + LG Smart TV with Netcast 3 & 4 (2012-13 models)
 + LG Smart TV with webOS (2014+ models)

[View files at version 1.2.0](https://github.com/ConnectSDK/Connect-SDK-Android/tree/1.2.0)
