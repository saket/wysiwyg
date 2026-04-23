# releaseAndroidTest is minified because sample's androidTest targets the release build type.
# AndroidX Test pulls in error_prone_annotations, which reference javax.lang.model compiler APIs
# that are not present on Android and are not needed at runtime for the test APK.
-dontwarn javax.lang.model.element.Modifier
