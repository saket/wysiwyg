import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.test)
  alias(libs.plugins.kotlin.android)
}

android {
  namespace = "me.saket.wysiwyg.benchmarks"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    minSdk = 28
    targetSdk = 36
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    // Must match :sample's release signing so package-manager treats the test APK
    // and the target APK as a related, profileable pair.
    create("release") {
      keyAlias = "sample"
      keyPassword = "frappe-snivel-possible-downward"
      storeFile = file("../sample/release_keystore.jks")
      storePassword = "abstract-emperor-john-twill"
    }
  }

  buildTypes {
    // Tracks the sample app's release variant (non-debuggable, minified, profileable).
    create("release") {
      isDebuggable = false
      signingConfig = signingConfigs.getByName("release")
    }
  }

  // The macrobenchmark instrumentation runs in a separate process and drives the
  // sample app via UiAutomator + atrace.
  targetProjectPath = ":sample"
  @Suppress("UnstableApiUsage")
  experimentalProperties["android.experimental.self-instrumenting"] = true

  kotlin {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_17)
    }
  }

  java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
  }
}

dependencies {
  implementation(libs.androidx.test.ext.junit)
  implementation(libs.androidx.test.uiautomator)
  implementation(libs.androidx.benchmark.macro.junit4)
  implementation(libs.junit)
}
