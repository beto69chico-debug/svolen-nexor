plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Versión: el número de build de GitHub Actions hace que cada APK sea "más nuevo" que el anterior.
val ciBuild = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "com.tecnobasics.svolennexor"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tecnobasics.svolennexor"
        minSdk = 26
        targetSdk = 35
        versionCode = ciBuild
        versionName = "1.0.$ciBuild"
    }

    signingConfigs {
        // Firma fija: necesaria para que las actualizaciones se instalen encima sin desinstalar.
        create("release") {
            storeFile = file("signing/svolen.jks")
            storePassword = "svolen-tecnobasics"
            keyAlias = "svolen"
            keyPassword = "svolen-tecnobasics"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    testImplementation("junit:junit:4.13.2")
    // org.json real para probar el guardado fuera del teléfono
    testImplementation("org.json:json:20240303")
}
