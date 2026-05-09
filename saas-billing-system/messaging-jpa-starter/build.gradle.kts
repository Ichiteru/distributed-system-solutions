plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.kotlin.plugin.spring")
  id("org.jetbrains.kotlin.plugin.jpa")
}

description = "SaaS Billing messaging JPA starter"

kotlin {
  jvmToolchain(17)
}

dependencies {
  api("org.springframework.data:spring-data-jpa")

  implementation("org.springframework.boot:spring-boot-autoconfigure")
  implementation("org.hibernate.orm:hibernate-core")
}
