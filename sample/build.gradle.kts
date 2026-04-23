import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "me.saket.wysiwyg.sample"
  compileSdk = libs.versions.compileSdk.get().toInt()
  testBuildType = "release"

  defaultConfig {
    applicationId = "me.saket.wysiwyg.sample"
    minSdk = 31
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      keyAlias = "sample"
      keyPassword = "frappe-snivel-possible-downward"
      storeFile = file("release_keystore.jks")
      storePassword = "abstract-emperor-john-twill"
    }
  }
  buildTypes {
    getByName("release") {
      isMinifyEnabled = true
      isDebuggable = false
      signingConfig = signingConfigs.getByName("debug")
      testProguardFiles("test-proguard-rules.pro")
    }
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  // Produce one APK per ABI rather than a universal one containing every .so.
  splits {
    abi {
      isEnable = true
      reset()
      include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
      isUniversalApk = false
    }
  }

  kotlin {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_17)
    }
  }

  java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
  }

  lint {
    abortOnError = true
  }
}

dependencies {
  implementation(project(":wysiwyg"))
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)

  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.ext.junit)
  // releaseAndroidTest is minified, and AndroidX Test's dependency graph expects these
  // annotations to be present even though they're normally irrelevant in non-minified test APKs.
  androidTestImplementation(libs.errorprone.annotations)
}
