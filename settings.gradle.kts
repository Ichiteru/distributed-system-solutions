rootProject.name = "distributed-system-solutions"

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }

  val kotlinVersion: String by settings
  val springBootVersion: String by settings

  plugins {
    id("org.springframework.boot") version springBootVersion
    id("org.jetbrains.kotlin.jvm") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.jpa") version kotlinVersion
  }
}

include(":platform-dependencies")
include(":reactive-chat-service")
