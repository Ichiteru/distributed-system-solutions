plugins {
  id("base")
  id("org.springframework.boot") apply false
  id("org.jetbrains.kotlin.jvm") apply false
  id("org.jetbrains.kotlin.plugin.spring") apply false
  id("org.jetbrains.kotlin.plugin.jpa") apply false
}

group = "com.ilchern"
version = "1.0-SNAPSHOT"

allprojects {
  repositories {
    mavenCentral()
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
