plugins { alias(libs.plugins.android.application) }

android {
    namespace = "com.example.nowbarprobe"
    compileSdk { version = release(36) { minorApiLevel = 1 } }
    defaultConfig {
        applicationId = "com.aistudio.quadrinheiro.nowbarprobe"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // Compile the exact same compatibility implementation used by the app.
    sourceSets.getByName("main").kotlin.srcDir("../../app/src/main/java/com/example/reading/samsung")
}
