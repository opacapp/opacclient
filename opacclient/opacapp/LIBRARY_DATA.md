Web Opac App - libary configuration files
=========================================

The JSON configuration files for each of the 1000+ libraries were previously located in the `src/main/assets/bibs` directory. We recently removed them from the GitHub repository.

Instead, we now maintain the files on our own server and download them to this directory using a custom Gradle task. This allows us to update them independently from the app using the `LibraryConfigUpdateService` implemented in [#444](https://github.com/opacapp/opacclient/pull/444).

Downloading the configuration files using Gradle
------------------------------------------------
Simply run the following command:
```
gradlew downloadJson
```
Or execute it from Android Studio's Gradle tool window.

This creates the JSON config files in the `src/main/assets/bibs` directory and a `src/main/assets/last_library_config_update.txt` file containing the date and time when you last  updated the configuration files.

Contributing
------------

If you want to test a new library or edit the configuration of an existing one, you can simply do that change in the `src/main/assets/bibs` directory and run the app. Just be sure not to run the `downloadJson` Gradle task afterwards because it will revert all your changes.

When you are done testing your change, please contact us at [info@opacapp.net](mailto:info@opacapp.net) or via a GitHub issue so that we can update the file on the server to push it to all app users.

Accessing the API directly
--------------------------

You can also directly access the configuration files using the API endpoint at

```
https://info.opacapp.net/androidconfigs/
```

*(At a file size of more than 500 KB, this is quite a large JSON file and might take some time to be downloaded and/or parsed.)*

The format of the returned data matches the format of the local configuration files, with the only exception being an additional `"_id"` field containing the library's ID (which is used as the name of the local file).

You can also download only the files that were modified since a specific point in time:

```
https://info.opacapp.net/androidconfigs/?modified_since=2016-08-01T16:30Z
```

or only the files that are compatible with a specific app version (specified using its version code):

```
https://info.opacapp.net/androidconfigs/?app_version=157
```

Rules for using the API
-----------------------

The library configuration files available through the API are subject to the same [license](https://github.com/opacapp/opacclient/blob/master/LICENSE) as the app itself.

While there are no special security measures in place to restrict access to the API, please contact us at [info@opacapp.net](mailto:info@opacapp.net) if you would like to use it in a way that is not directly related to the development of the app and/or is expected to cause a considerable amount of traffic on our server, e.g. through frequent polling.