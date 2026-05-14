import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.vanniktech.maven.publish)
}

android {
  namespace = "me.saket.wysiwyg.parser.flexmark"
  resourcePrefix = "wysiwyg_flexmark_"

  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
    consumerProguardFiles("consumer-rules.pro")
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
  api(projects.wysiwygCore)
  api(libs.kotlinx.coroutines.core)

  implementation(libs.flexmark.java)
  implementation(libs.flexmark.ext.gfm.strikethrough)
  implementation(libs.flexmark.ext.gfm.tasklist)
  implementation(libs.androidx.tracing.ktx)
}
