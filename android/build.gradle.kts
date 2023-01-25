plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.flammky.compose_components.android"
    compileSdk = 32

    defaultConfig {
        minSdk = 21
        targetSdk = 32
    }
    buildTypes {
    }
    buildFeatures {
        compose = true
        composeOptions.kotlinCompilerExtensionVersion = "1.4.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    /* androidx.compose */
    dependencies androidxComposeDep@ {
        val vui = "1.3.3"

        // Core UI
        implementation("androidx.compose.ui:ui:$vui")
        implementation("androidx.compose.ui:ui-tooling-preview:$vui")
        implementation("androidx.compose.ui:ui-util:$vui")

        // Debug
        debugImplementation("androidx.compose.ui:ui-tooling:$vui")

        val vf = "1.3.1"
        // Foundation
        implementation("androidx.compose.foundation:foundation:$vf")
    }
}