libopac  [ ![Download](https://api.bintray.com/packages/opacapp/libs/libopac/images/download.svg) ](https://bintray.com/opacapp/libs/libopac/_latestVersion)
=======
libopac is a Java library that can connect to online catalogs (OPACs) of public libraries and access their search and account data features. It is used by our own [Android app](https://github.com/opacapp/opacclient).

Web catalogues of different library system vendors are supported, see our [website](http://de.opacapp.net/kompatibilitaet/) or [wiki](https://github.com/raphaelm/opacclient/wiki/Supported-library-types) for details.

Installation
------------
The library is available through [JCenter](https://bintray.com/opacapp/libs/libopac/). Snippets for Maven or Gradle build configuration files and JAR downloads are available there.

Usage
-----
The following resources should help you to get started on using libopac:

* [Basic sample project using Maven](https://github.com/opacapp/libopac-sample-mvn) 
* [Basic sample project using Gradle](https://github.com/opacapp/libopac-sample-gradle) 
* [Javadoc](https://en.opacapp.net/doc/) (not always up-to-date)
* [JSON configuration files](https://github.com/opacapp/opacclient/blob/master/opacclient/opacapp/LIBRARY_DATA.md) for more than 1000 public and university libraries

If you have any questions, you can write us an email at [info@opacapp.net](mailto:info@opacapp.net).

### Usage on Android

If you want to use libopac on Android, the setup is currently a little complicated because it uses the Apache HTTP client, an older version of which is included in Android and now deprecated. Please use [our own build.gradle file](https://github.com/opacapp/opacclient/blob/master/opacclient/opacapp/build.gradle) as a reference. In particular, you will need the following lines (maybe more):

```groovy
android {
    // ...

    packagingOptions {
        exclude 'META-INF/NOTICE'
        exclude 'META-INF/LICENSE'
    }

    useLibrary 'org.apache.http.legacy'
}
dependencies {
    compile('net.opacapp:libopac:+') { // you can insert the latest version here
        transitive = false
    }
    compile 'org.apache.httpcomponents:httpclient-android:4.3.5.1'
    // additional libopac dependencies
}
```

We are planning to replace the Apache HTTP client with [OkHttp](http://square.github.io/okhttp/) in the future, which will eliminate these problems.

License
-------
Being a part of the [Web Opac App](https://github.com/opacapp/opacclient) project, this code is also licensed under the terms of the [MIT License](http://opensource.org/licenses/mit-license.php).
