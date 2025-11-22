plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false

    // --- LÍNEA AÑADIDA (EL PLUGIN QUE FALTABA) ---
    alias(libs.plugins.google.services) apply false
}