import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
}

// ===============================================================
//  BLOQUE DE LECTURA DE PROPIEDADES (MOVIDO FUERA DEL BLOQUE ANDROID)
// ===============================================================
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    // Usamos use{} para asegurar que el flujo se cierre
    localPropertiesFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
}
// Función de extensión para leer la propiedad sin tener que escribir todo el código
fun getLocalProperty(key: String): String = localProperties.getProperty(key) ?: ""
// ===============================================================

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.example.geotrack"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.geotrack"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ===============================================================
        //  USAMOS LA FUNCIÓN PARA OBTENER LA PROPIEDAD
        // ===============================================================
        manifestPlaceholders["MAPS_API_KEY"] = getLocalProperty("MAPS_API_KEY")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
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
    // --- Dependencias de Firebase ---
    implementation(platform("com.google.firebase:firebase-bom:33.1.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // --- Dependencias de Room ---
    implementation(libs.room.runtime)
    implementation(libs.gson) // GSON debe ir antes del ksp para que lo lea bien
    ksp(libs.room.compiler)

    // --- Dependencia de GPS ---
    implementation(libs.play.services.location)

    // --- Dependencia de Google Maps ---
    implementation(libs.play.services.maps)

    // --- Dependencia de WorkManager ---
    implementation(libs.work.runtime)

    // --- Dependencias para el Dashboard de Admin ---
    implementation(libs.firebase.ui.firestore)
    implementation(libs.glide)

    // --- Dependencia para 'ListenableFuture' ---
    implementation(libs.guava)

    // --- Dependencias Básicas ---
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}