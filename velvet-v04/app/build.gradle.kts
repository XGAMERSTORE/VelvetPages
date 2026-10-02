plugins { id("com.android.application") }

android {
    namespace = "com.wildbond.velvetcompanion.v04"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.wildbond.velvetcompanion.v04"
        minSdk = 24
        targetSdk = 36
        versionCode = 6
        versionName = "0.4.2"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }
}
