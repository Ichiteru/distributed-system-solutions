plugins {
  id("base")
  id("org.springframework.boot") apply false
  id("org.jetbrains.kotlin.jvm") apply false
  id("org.jetbrains.kotlin.plugin.spring") apply false
  id("org.jetbrains.kotlin.plugin.jpa") apply false
  id("com.github.davidmc24.gradle.plugin.avro") apply false
}

group = "com.ilchern"
version = "1.0-SNAPSHOT"

allprojects {
  repositories {
    mavenCentral()
    maven(url = "https://packages.confluent.io/maven/")
  }
}

subprojects {
  tasks.withType<Test>().configureEach {
    useJUnitPlatform()
  }

  pluginManager.withPlugin("java") {
    dependencies {
      add("implementation", platform(project(":platform-dependencies")))
      add("testImplementation", platform(project(":platform-dependencies")))
    }
  }
}
