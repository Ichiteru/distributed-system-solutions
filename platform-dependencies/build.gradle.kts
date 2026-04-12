plugins {
  id("java-platform")
}

javaPlatform {
  allowDependencies()
}

val springBootVersion: String by project
val bucket4jVersion: String by project

dependencies {
  api(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
  constraints {
    api("com.bucket4j:bucket4j_jdk17-core:$bucket4jVersion")
    api("com.bucket4j:bucket4j_jdk17-lettuce:$bucket4jVersion")
  }
}
