import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.JavaVersion
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
  alias(libs.plugins.android.library)
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
    minSdk = libs.versions.minSdk.get().toInt()
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_17)
      freeCompilerArgs.add("-Xcontext-parameters")
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
  api(libs.kotlinx.coroutines.core)
  api(libs.androidx.compose.ui)
  api(libs.androidx.compose.foundation)

  implementation(libs.androidx.compose.material.ripple)
  implementation(libs.androidx.tracing.ktx)
  implementation(libs.poko.annotations)

  lintChecks(libs.composeLintChecks)

  testImplementation(libs.junit)
  testImplementation(libs.truth)
  testImplementation(libs.assertk)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.turbine)
  testImplementation(libs.touchrobot) {
    // TODO: Remove this when Wysiwyg no longer uses eygraber's Paparazzi fork.
    exclude(group = "app.cash.paparazzi", module = "paparazzi")
  }
}

tasks.withType<Test>().configureEach {
  // Paparazzi requires Java 21 or newer. Use the Gradle JVM, which can be Java 24.
  javaLauncher.set(
    javaToolchains.launcherFor {
      languageVersion.set(JavaLanguageVersion.of(JavaVersion.current().majorVersion))
    }
  )
}
