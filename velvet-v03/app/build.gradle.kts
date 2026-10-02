plugins { id("com.android.application") }

android {
    namespace = "com.wildbond.velvetcompanion.v03"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.wildbond.velvetcompanion.v03"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "0.3.0"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }
}
