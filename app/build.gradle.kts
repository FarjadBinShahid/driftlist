import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.farjad.letterline"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.farjad.letterline"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "0.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Room migration tests read the exported schemas.
    sourceSets.getByName("androidTest").assets.directories.add("$projectDir/schemas")

    // Release signing reads ../keystore.properties (gitignored). Without it, release builds come out unsigned.
    val keystoreProps = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
        Properties().apply { file.inputStream().use { load(it) } }
    }
    signingConfigs {
        if (keystoreProps != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Lets a debug/test build sit next to the installed release build.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)

    testImplementation(libs.junit)
    // Android's org.json is a stub on the JVM; the real one for unit tests of the JSON formats.
    testImplementation(libs.org.json)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.compose.ui.test.junit4.accessibility)
    debugImplementation(libs.compose.ui.test.manifest)

    constraints {
        // room-testing's schema reader needs 1.8+; this only lifts the version, it adds nothing.
        implementation(libs.kotlinx.serialization.json)
    }
}

// --- F1: no trackers, ever -------------------------------------------------------------------
// Letterline's promise is "nothing leaves your phone". This fails every build if a known analytics,
// advertising, crash-reporting or attribution library ever lands on the app's runtime classpath,
// directly or through another dependency.
val trackerPrefixes = listOf(
    "com.google.firebase", "com.google.android.gms:play-services-analytics", "com.google.android.gms:play-services-ads",
    "com.google.android.gms:play-services-measurement", "com.google.android.gms:play-services-appset",
    "com.crashlytics", "io.fabric", "io.sentry", "com.bugsnag", "com.newrelic", "com.instabug",
    "com.facebook.android", "com.facebook.sdk", "com.appsflyer", "com.adjust.sdk", "io.branch",
    "com.mixpanel", "com.amplitude", "com.segment", "com.onesignal", "com.flurry", "com.microsoft.appcenter",
    "com.yandex.android:mobmetricalib", "com.applovin", "com.unity3d.ads", "com.ironsource", "com.mopub",
)
val checkNoTrackers = tasks.register("checkNoTrackers") {
    group = "verification"
    description = "Fails if an analytics, ads or tracking library is on the release runtime classpath."
    val components = configurations.named("releaseRuntimeClasspath").map { cfg ->
        cfg.incoming.resolutionResult.allComponents.map { it.moduleVersion.toString() }
    }
    doLast {
        val all = components.get()
        val found = all.filter { id -> trackerPrefixes.any { id.startsWith(it) } }
        if (found.isNotEmpty()) throw GradleException("Tracker libraries on the classpath: $found")
        logger.lifecycle("checkNoTrackers: ${all.size} libraries checked, none are trackers")
    }
}
tasks.named("preBuild") { dependsOn(checkNoTrackers) }
