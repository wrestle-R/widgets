plugins { id("com.android.application") }

android {
    namespace = "com.widgets.gallery"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.widgets.gallery"
        minSdk = 28
        targetSdk = 36
        versionCode = 3
        versionName = "0.0.3"
    }
    sourceSets["main"].assets.directories.add("../../shared")
}
