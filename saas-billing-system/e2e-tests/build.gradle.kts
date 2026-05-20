plugins {
  id("org.jetbrains.kotlin.jvm")
}

description = "SaaS Billing end-to-end tests"

kotlin {
  jvmToolchain(17)
}

dependencies {
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.assertj:assertj-core")
  testImplementation("org.awaitility:awaitility")
  testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")

  testRuntimeOnly("org.postgresql:postgresql")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
