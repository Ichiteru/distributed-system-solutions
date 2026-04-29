plugins {
  id("java-library")
  id("com.github.davidmc24.gradle.plugin.avro")
}

description = "SaaS Billing shared Kafka contracts"

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

dependencies {
  api("org.apache.avro:avro:1.11.4")
}
