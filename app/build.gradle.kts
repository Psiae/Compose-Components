plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.flammky.compose_components.android"
    compileSdk = 33

    defaultConfig {
        applicationId = "dev.flammky.compose_components.android"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
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
    implementation(project(":android"))

    /* androidx.activity */
    dependencies {
        val v = "1.6.1"
        implementation("androidx.activity:activity-compose:$v")
    }

    /* androidx.compose */
    dependencies {

        // Core UI
        val vcui = "1.3.3"
        api("androidx.compose.ui:ui:$vcui")
        api("androidx.compose.ui:ui-tooling-preview:$vcui")
        api("androidx.compose.ui:ui-util:$vcui")

        // Core UI - Debug
        debugApi("androidx.compose.ui:ui-tooling:$vcui")

        // Foundation
        val vf = "1.3.1"
        api("androidx.compose.foundation:foundation:$vf")

        // Material
        val vMaterial = "1.3.1"
        api("androidx.compose.material:material:$vMaterial")

        // Material3
        val vMaterial3 = "1.0.1"
        api("androidx.compose.material3:material3:$vMaterial3")
    }

    /* androidx.navigation */
    dependencies {
        // compose
        val vCompose = "2.6.0-alpha04"
        implementation("androidx.navigation:navigation-compose:$vCompose")
    }

    @Suppress("SpellCheckingInspection")
    /* google.accompanist */
    dependencies  {
        val v = "0.28.0"

        // PlaceHolder
        implementation("com.google.accompanist:accompanist-placeholder:$v")

        // Swipe-refresh
        implementation("com.google.accompanist:accompanist-swiperefresh:$v")

        // SysUI
        implementation("com.google.accompanist:accompanist-systemuicontroller:$v")
    }

    dependencies {
        // Collections.Immutable
        val vCollection_Immutable = "0.3.5"
        implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:$vCollection_Immutable")
    }

    implementation("com.google.android.material:material:1.7.0")
}