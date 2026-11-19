# Android releases

Set `versionName` to the next semantic version in `app/build.gradle.kts` and increase `versionCode` for each new Android release. Build from the commit that will be tagged. Use the same Widgets release key for every update and keep a protected backup of the keystore and its password.

From this directory, with Android SDK Build Tools 36.0.0 installed and `ANDROID_HOME` set:

```bash
./gradlew assembleRelease
tools="$ANDROID_HOME/build-tools/36.0.0"
build="app/build/outputs/apk/release"
"$tools/zipalign" -f -p 4 "$build/app-release-unsigned.apk" "$build/app-release-aligned.apk"
"$tools/apksigner" sign --ks .signing/widgets.p12 --ks-key-alias widgets \
  --ks-pass file:.signing/password --out "$build/widgets.apk" "$build/app-release-aligned.apk"
"$tools/apksigner" verify --verbose --print-certs "$build/widgets.apk"
(cd "$build" && sha256sum widgets.apk > widgets.apk.sha256)
```

Push the release commit and its matching `vX.Y.Z` tag. Create a draft GitHub release for the tag with the install notes, `widgets.apk`, and `widgets.apk.sha256`. Check the APK version, signing certificate, and checksum before publishing. Keep the asset name `widgets.apk` so the site can link to the latest stable release.
