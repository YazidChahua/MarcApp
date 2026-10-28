
plugins {
    alias(libs.plugins.android.application)

    // Compilador de Jetpack Compose
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
}

android {
    namespace = "com.marcapp.app"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.marcapp.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf(
                    "androidx.**",
                    "kotlin.**",
                    "kotlinx.**"
                )
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Habilitar Jetpack Compose
    buildFeatures {
        compose = true
    }
}

dependencies {

    // Dependencias originales
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Versiones de Jetpack Compose
    val composeBom = platform(
        "androidx.compose:compose-bom:2026.09.00"
    )

    implementation(composeBom)

    // Diseño visual
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.runtime:runtime-saveable")

    // Para mostrar las pantallas en Android Studio
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Para utilizar Compose dentro de MainActivity
    implementation("androidx.activity:activity-compose:1.13.0")

    // Pruebas
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
