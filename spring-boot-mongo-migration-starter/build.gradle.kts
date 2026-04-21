plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.kotlin.plugin.spring")
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  api("io.mongock:mongock-springboot-v3")
  api("io.mongock:mongodb-springdata-v4-driver")
  api("org.mongodb:mongodb-driver-sync")
  api("org.springframework.data:spring-data-mongodb")

  implementation("org.springframework.boot:spring-boot-autoconfigure")
}
