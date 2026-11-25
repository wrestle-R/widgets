plugins { id("com.android.application") }

android {
    namespace = "com.widgets.gallery"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.widgets.gallery"
        minSdk = 28
        targetSdk = 36
        versionCode = 5
        versionName = "0.0.5"
    }
    sourceSets["main"].assets.directories.add("../../shared")
}
