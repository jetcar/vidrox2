plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ycngmn.notubetv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ycngmn.notubetv"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "0.0.3"

    }

    buildTypes {
        release {
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }


}

dependencies {
    implementation(libs.ktor.client.core)
    implementation (libs.ktor.ktor.client.okhttp)
    api(libs.compose.webview.multiplatform) {
        // Exclude desktop-only JCEF/jogamp dependencies that can't be resolved from Android repos
        exclude(group = "org.jogamp.gluegen", module = "gluegen-rt")
        exclude(group = "org.jogamp.jogl", module = "jogl-all")
        exclude(group = "dev.datlag.kcef", module = "kcef")
        exclude(group = "dev.datlag", module = "kcef")
        exclude(group = "dev.datlag", module = "jcef")
    }
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3.android)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}