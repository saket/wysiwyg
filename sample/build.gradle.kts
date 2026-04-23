import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "me.saket.wysiwyg.sample"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "me.saket.wysiwyg.sample"
    minSdk = 31
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"
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
}
