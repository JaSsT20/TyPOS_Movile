plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.typdevstudio.typos_movil"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.typdevstudio.typos_movil"
        minSdk = 24
        targetSdk = 35
        versionCode = 23
        versionName = "1.1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }

        // Licencias en la nube: clave pública ECDSA P-256 (Base64 de SubjectPublicKeyInfo) con la que se verifican
        // los tokens. TODO: es una clave de PRUEBA; reemplazarla por la de producción antes de publicar un release.
        buildConfigField(
            "String",
            "CLAVE_PUBLICA_LICENCIAS",
            "\"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE0/PmdlivtoC2d6SDoUSP6PjmAonBAbFsf/GtQ5XN6xdfij1uy068sHd9XzVdsJQ8ACsMeJm8Kl6hS1UH8UFvlg==\""
        )

        // Contacto que aparece en los avisos de vencimiento de la licencia (5 días antes y el mismo día).
        buildConfigField("String", "CONTACTO_LICENCIA_NOMBRE", "\"T&P DevStudio\"")
        buildConfigField("String", "CONTACTO_LICENCIA_TELEFONO", "\"+1 (849) 359-4927\"")
    }

    buildTypes {
        debug {
            // API de licencias local de pruebas. Se cambia sin tocar código con -PlicenciasUrlApiDebug=...
            // (o en gradle.properties). La IP debe estar permitida en src/debug/res/xml/network_security_config.xml.
            val urlApiDebug = providers.gradleProperty("licenciasUrlApiDebug").getOrElse("http://192.168.1.2:5109/api/v1")
            buildConfigField("String", "URL_API_LICENCIAS", "\"$urlApiDebug\"")
        }
        release {
            // API de licencias de producción (IIS, solo HTTPS).
            buildConfigField("String", "URL_API_LICENCIAS", "\"https://atpm-prod.notfoundgroup.com/api/v1\"")
            isMinifyEnabled = true
            isShrinkResources = true
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Base de datos local SQLite con Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Cámara y Escáner de Códigos de Barras
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)

    // Validación de la licencia en segundo plano
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}