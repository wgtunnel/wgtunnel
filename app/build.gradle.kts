import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.grgit)
    alias(libs.plugins.licensee)
}

android {
    namespace = Constants.APP_ID
    compileSdk = Constants.TARGET_SDK

    androidResources { generateLocaleConfig = true }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    ksp { arg("room.schemaLocation", "$projectDir/schemas") }

    // fix okhttp proguard issue
    packaging { resources { pickFirsts.add("okhttp3/internal/publicsuffix/publicsuffixes.gz") } }

    defaultConfig {
        applicationId = Constants.APP_ID
        minSdk = Constants.MIN_SDK
        targetSdk = Constants.TARGET_SDK
        versionCode = computeVersionCode()
        versionName = computeVersionName()

        sourceSets { getByName("debug").assets.srcDirs(files("$projectDir/schemas")) }

        val languagesArray = buildLanguagesArray(languageList())
        buildConfigField("String[]", "LANGUAGES", "new String[]{ $languagesArray }")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create(Constants.RELEASE) {
            storeFile = file(System.getenv("KEY_STORE_PATH") ?: "keystore/android_keystore.jks")
            storePassword =
                LocalProperties.get("SIGNING_STORE_PASSWORD")
                    ?: System.getenv("SIGNING_STORE_PASSWORD")
            keyAlias =
                LocalProperties.get("SIGNING_KEY_ALIAS") ?: System.getenv("SIGNING_KEY_ALIAS")
            keyPassword =
                LocalProperties.get("SIGNING_KEY_PASSWORD") ?: System.getenv("SIGNING_KEY_PASSWORD")
        }
    }

    buildTypes {
        packaging.jniLibs.keepDebugSymbols.addAll(
            listOf("libwg-go.so", "libwg-quick.so", "libwg.so")
        )

        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName(Constants.RELEASE)
            resValue("string", "provider", "\"${Constants.APP_NAME}.provider\"")
        }

        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "WG Tunnel Debug")
            isDebuggable = true
            resValue("string", "provider", "\"${Constants.APP_NAME}.provider.debug\"")
        }

        create(Constants.NIGHTLY) {
            initWith(buildTypes.getByName(Constants.RELEASE))
            applicationIdSuffix = ".nightly"
            resValue("string", "app_name", "WG Tunnel Nightly")
            resValue("string", "provider", "\"${Constants.APP_NAME}.provider.nightly\"")
        }
    }

    flavorDimensions.add("type")
    productFlavors {
        create("fdroid") {
            dimension = "type"
            buildConfigField("String", "FLAVOR", "\"fdroid\"")
        }
        create("google") {
            dimension = "type"
            buildConfigField("String", "FLAVOR", "\"google\"")
        }
        create("standalone") {
            dimension = "type"
            buildConfigField("String", "FLAVOR", "\"standalone\"")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
            freeCompilerArgs = listOf("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }

    licensee {
        allowedLicenses().forEach { allow(it) }
        allowedLicenseUrls().forEach { allowUrl(it) }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val outputFileName =
                    if (variant.flavorName == "fdroid" && variant.buildType.name == "release") {
                        "${Constants.APP_NAME}-fdroid-release-${variant.versionName}.apk"
                    } else {
                        "${Constants.APP_NAME}-${variant.flavorName}-v${variant.versionName}.apk"
                    }
                output.outputFileName = outputFileName
            }
    }
}

dependencies {
    implementation(project(":logcatter"))
    implementation(project(":networkmonitor"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.storage)

    implementation("androidx.navigation3:navigation3-runtime:1.0.0-alpha10")
    implementation("androidx.navigation3:navigation3-ui:1.0.0-alpha10")
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")

    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.manifest)

    implementation(libs.tunnel)
    implementation(libs.amneziawg.android)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.timber)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.drawablepainter)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.zxing.android.embedded)

    implementation(libs.material.icons.core)
    implementation(libs.material.icons.extended)

    implementation(libs.pin.lock.compose)

    implementation(libs.androidx.core)

    implementation(libs.androidx.core.splashscreen)

    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)

    implementation(libs.qrose)
    implementation(libs.semver4j)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.slf4j.android)
    implementation(libs.icmp4a)

    // shizuku
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    implementation(libs.reorderable)
    implementation(libs.roomdatabasebackup) {
        exclude(group = "org.reactivestreams", module = "reactive-streams")
    }

    // state management
    implementation(libs.orbit.compose)
    implementation(libs.orbit.viewmodel)
    implementation(libs.orbit.core)
}

tasks.register<Copy>("copyLicenseeJsonToAssets") {
    dependsOn("licensee")
    val outputAssets = layout.projectDirectory.dir("src/main/assets")
    from(layout.buildDirectory.file("reports/licensee/androidFdroidRelease/artifacts.json")) {
        rename("artifacts.json", "licenses.json")
    }
    into(outputAssets)
}

tasks.named("preBuild") { dependsOn("copyLicenseeJsonToAssets") }

// https://gist.github.com/obfusk/61046e09cee352ae6dd109911534b12e#fix-proposed-by-linsui-disable-baseline-profiles
tasks.whenTaskAdded {
    if (name.contains("ArtProfile")) {
        enabled = false
    }
}
