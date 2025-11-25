plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

}

android {
    namespace = "com.teamapex.I23_0011_I23_0646"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.teamapex.I23_0011_I23_0646"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.squareup.picasso:picasso:2.8")

    // OkHttp for networking
    implementation("com.squareup.okhttp3:okhttp:4.11.0")


    implementation("androidx.recyclerview:recyclerview:1.3.1")
// Material Design Components
    implementation("com.google.android.material:material:1.9.0")

    implementation("io.agora.rtc:full-sdk:4.2.6")

    implementation("com.github.permissions-dispatcher:permissionsdispatcher:4.9.2")



}
