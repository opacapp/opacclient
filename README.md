Web Opac App [![Build Status](https://travis-ci.org/opacapp/opacclient.svg?branch=master)](https://travis-ci.org/opacapp/opacclient)
============
Android client for public libraries. See [opacapp.net](http://opacapp.net) for details.

![Screenshot](http://opacapp.de/wp-content/themes/opacapp/img/020_menue.png)


Features
--------
* Search the catalogue
* Make reservations
* Bookmarks
* Account view
* extending lending periods
* Notification for return dates
* Search by ISBN using barcodes
* much more cool stuff

Not all of the features are availably for every library.

Support
-------
Supports web catalogues of different library system vendors, see our [website](http://de.opacapp.net/kompatibilitaet/) or [wiki](https://github.com/raphaelm/opacclient/wiki/Supported-library-types) for details.

Java Library (`libopac`)
------------------------
The underlying code that the app uses to connect to the different supported OPAC software systems and parse data from them is available as a separate library called `libopac`. It can also be used in ordinary Java projects because it does not depend on Android APIs. More information about the library can be found in [its own README file](https://github.com/opacapp/opacclient/blob/master/opacclient/libopac/README.md).

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
* Raphael Michel <mail@raphaelmichel.de>
* Johan von Forstner

Many thanks go to our contributors, including:

* Rüdiger Wurth
* Simon Legner

