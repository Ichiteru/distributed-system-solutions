rootProject.name = "distributed-system-solutions"

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }

  val kotlinVersion: String by settings

  plugins {
    id("org.jetbrains.kotlin.jvm") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.jpa") version kotlinVersion
  }
}

include(":platform-dependencies")
