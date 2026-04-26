plugins {
  id("org.springframework.boot")
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.kotlin.plugin.spring")
  id("org.jetbrains.kotlin.plugin.jpa")
}

description = "SaaS Billing subscription-service"

kotlin {
  jvmToolchain(17)
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.kafka:spring-kafka")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
}
