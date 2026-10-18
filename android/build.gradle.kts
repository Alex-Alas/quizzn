// Sin dependencias: solo el framework de Android (Activity, Service, WebView). AGP 9 compila Kotlin por sí solo.
plugins { id("com.android.application") version "9.1.0" }

android {
    namespace = "io.github.alexalas.quizzn"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.github.alexalas.quizzn"
        minSdk = 26 // TYPE_APPLICATION_OVERLAY
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // ponytail: firma con la llave debug de esta PC (las actualizaciones deben salir de aquí); para Play Store, llave propia
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
