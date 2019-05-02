![Screenshot](https://raw.githubusercontent.com/opacapp/opacclient/master/img/github_banner.png)

Web Opac App [![Build Status](https://travis-ci.org/opacapp/opacclient.svg?branch=master)](https://travis-ci.org/opacapp/opacclient)
============

Android client for public libraries. See [opacapp.net](http://opacapp.net) for details.

<a href="https://f-droid.org/repository/browse/?fdid=de.geeksfactory.opacclient" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="100"/></a>
<a href="https://play.google.com/store/apps/details?id=de.geeksfactory.opacclient" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="100"/></a>

Features
--------
* Search the catalogue
* Make reservations
* Bookmarks
* Account view
* Extending lending periods
* Notification for return dates
* Search by ISBN using barcodes
* Much more cool stuff

Not all of the features are available for every library.

Support
-------
Supports web catalogues of different library system vendors, see our [website](http://de.opacapp.net/kompatibilitaet/) or [wiki](https://github.com/raphaelm/opacclient/wiki/Supported-library-types) for details.

Java Library (`libopac`)
------------------------
The underlying code that the app uses to connect to the different supported OPAC software systems and parse 
data from them is available as a separate library called `libopac`. It can also be used in ordinary Java 
projects because it does not depend on Android APIs. More information about the library can be found in 
[its own README file](https://github.com/opacapp/opacclient/blob/master/opacclient/libopac/README.md).

Library configuration files
---------------------------
The JSON configuration files for each library previously located in the `opacclient/opacapp/src/main/assets/bibs` directory were recently removed from the repository and can be downloaded from our server. For more information about this, see [here](https://github.com/opacapp/opacclient/blob/master/opacclient/opacapp/LIBRARY_DATA.md).

Contributing
------------
We'd like to invite you to contribute to our project, e.g. by improving the user interface or adding support
for new libraries. Additionally, you could help to extend our test suite to increase this number: [![Coverage Status](https://coveralls.io/repos/github/opacapp/opacclient/badge.svg?branch=master)](https://coveralls.io/github/opacapp/opacclient?branch=master) :wink:.

Checking out the project with Android Studio should be fairly straightforward, but we
also have a little bit of information in the [project wiki](https://github.com/opacapp/opacclient/wiki).
Contributions are best submitted via GitHub pull requests.

If you get stuck anywhere in the process, please do not hesitate to create a draft pull request and ask us there!

Please note that we have a [Code of Conduct](https://github.com/opacapp/opacclient/blob/master/CODE_OF_CONDUCT.md)
in place that applies to all project-related communication.

License
-------
This code is released under the terms of the [MIT License](http://opensource.org/licenses/mit-license.php)

It contains several libraries:

* jsoup, (c) Jonathan Hedley and contributors, MIT License
* ACRA, (c) Kevin Gaudin and contributors, Apache License
* cwac-endless and cwac-adapter, (c) CommonsWare, LLC and Google, Inc., Apache License
* Android Support library, (c) Google Inc., Apache License
* NineOldAndroids, (c) Jake Wharton, Apache License
* android-flowlayout, (c) Artem Votincev, Apache License
* rv-joiner, (c) Evgeny Egorov, Apache License

Authors
-------
* [Raphael Michel](https://github.com/raphaelm) <mail@raphaelmichel.de>
* [Johan von Forstner](https://github.com/johan12345)

Many thanks go to our contributors, including:

* [Rüdiger Wurth](https://github.com/ruediger-w)
* [Simon Legner](https://github.com/simon04)
* [YKassim](https://github.com/YKassim)
* [AdamLazarus](https://github.com/AdamLazarus)
* [Sulekha Alse](https://github.com/YSulekha)
* [Caroline Ho](https://github.com/carolineh101)
* [Ceasar Jimenez](https://github.com/ceasarj)
* [Nico Puhlmann](https://github.com/NPuhlmann)
* [JustyAnOther](https://github.com/JustyAnOther)
