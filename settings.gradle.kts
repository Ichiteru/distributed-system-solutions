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
    id("com.github.davidmc24.gradle.plugin.avro") version "1.7.1"
  }
}

include(":platform-dependencies")
include(":reactive-chat-service")
include(":spring-boot-mongo-migration-starter")

include("saas-billing-system")
include("saas-billing-system:billing-contracts")
include("saas-billing-system:e2e-tests")
include("saas-billing-system:billing-orchestrator")
include("saas-billing-system:billing-service")
include("saas-billing-system:messaging-jpa-starter")
include("saas-billing-system:payment-service")
include("saas-billing-system:subscription-service")
