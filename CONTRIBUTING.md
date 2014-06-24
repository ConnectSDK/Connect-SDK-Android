#Contributing to Connect SDK

##General Questions

Please do not use GitHub issues for general questions about the SDK. Instead, use any of the following services to reach out to the development team.

- [@ConnectSDK](https://twitter.com/ConnectSDK)
- [Stack Overflow TV Tag](http://www.stackoverflow.com/tags/tv)
- [support@connectsdk.com](mailto:support@connectsdk.com)

##Versioning

We use [semantic versioning](http://semver.org/) in our tagged releases.

##Branching Strategy

- master
 + latest stable, QA'd, tagged release of the SDK
 + assume that this is safe for production use
- sdk_MAJOR.MINOR
 + stable branch working towards the next major/minor/patch release
 + safe for checking out new features, but do not use in any production apps
- sdk_MAJOR.MINOR-dev
 + unstable development branch working towards the next major/minor/patch release
 + may not compile/run without errors
 + for development only
 + submit pull requests against this branch

##Bug Reports & Feature Requests

We use GitHub's issues system for managing bug reports and some upcoming features. Just open an issue and a member of the team will set the appropriate assignee, label, & milestone.

###Crash Reports

If you experience a crash, please attach your symbolicated crash log or stack trace to an issue in GitHub.

##Pull Requests

If you would like to submit code, please fork the repository on GitHub and develop on the latest sdk-X.Y-dev branch. We do not accept pull requests on the master branch, as we only merge QA'd & tagged code into the master branch. See the description of our branching strategy above.

###Use of third party libraries

Connect SDK does include some third party libraries, but we try to avoid using them. If you'd like to integrate a library with a pull request, make sure that library has an open source license (MIT, Apache 2.0, etc).

###Licensing

If you submit a pull request, you acknowledge that your code will be released to the public under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html). Make sure that you have the rights to the code you are submitting in the pull request.

##Testing Lab

In the development of Connect SDK, we have gathered a number of devices for testing purposes. If you are contributing to and/or integrating Connect SDK & would like something tested in our lab, you may contact [partners@connectsdk.com](mailto:partners@connectsdk.com) with your request.
