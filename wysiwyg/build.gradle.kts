import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.vanniktech.maven.publish)
  alias(libs.plugins.paparazzi)
  alias(libs.plugins.poko)
}

android {
  namespace = "me.saket.wysiwyg"
  resourcePrefix = "wysiwyg_"

  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    minSdk = 23
    consumerProguardFiles("consumer-rules.pro")
  }

  buildFeatures {
    compose = true
    buildConfig = true
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
  api(libs.flexmark.java)
  api(libs.flexmark.ext.gfm.strikethrough)
  api(libs.extendedspans)
  api(libs.kotlinx.coroutines.core)

  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.poko.annotations)

  lintChecks(libs.composeLintChecks)

  testImplementation(libs.junit)
  testImplementation(libs.truth)
  testImplementation(libs.assertk)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.turbine)
  testImplementation(libs.androidx.compose.material3)
}
