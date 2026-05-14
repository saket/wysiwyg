pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "wysiwyg-root"
include(
  ":sample",
  ":wysiwyg",
  ":wysiwyg-core",
  ":wysiwyg-parser-flexmark",
  ":benchmarks",
)
