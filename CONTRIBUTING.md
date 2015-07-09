#Contributing to Connect SDK

##General Questions

Please do not use GitHub issues for general questions about the SDK. Instead, use any of the following services to reach out to the development team.

- [@ConnectSDK](https://twitter.com/ConnectSDK)
- Stack Overflow: [Connect-SDK tag](https://stackoverflow.com/tags/connect-sdk) (or [TV tag](https://stackoverflow.com/tags/tv))
- [support@connectsdk.com](mailto:support@connectsdk.com)

##Versioning

We use [semantic versioning](http://semver.org/) in our tagged releases.

##Branching Strategy

We use the [successful git branching model](http://nvie.com/posts/a-successful-git-branching-model/), except without release branches, and the `develop` branch is named `dev`.

##Bug Reports & Feature Requests

We use GitHub's issues system for managing bug reports and some upcoming features. Just open an issue and a member of the team will set the appropriate assignee, label, & milestone.

###Crash Reports

If you experience a crash, please attach your symbolicated crash log or stack trace to an issue in GitHub.

##Pull Requests

If you would like to submit code, please fork the repository on GitHub and develop in a feature branch, created from the latest `dev` commit. We do not accept pull requests on the `master` branch, as we only merge QA'd and tagged code into the `master` branch.

###Tests

Please include unit tests for the new/changed functionality with your pull request. It will help to verify the code is working as designed and make sure there are no regressions in future releases.

###Use of third party libraries

Connect SDK includes some third party libraries. If you'd like to integrate a library with a pull request, make sure that library has an open source license (MIT, Apache 2.0, etc).

###Licensing

If you submit a pull request, you acknowledge that your code will be released to the public under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html). Make sure that you have the rights to the code you are submitting in the pull request.

##Testing Lab

In the development of Connect SDK, we have gathered a number of devices for testing purposes. If you are contributing to and/or integrating Connect SDK & would like something tested in our lab, you may contact [partners@connectsdk.com](mailto:partners@connectsdk.com) with your request.
