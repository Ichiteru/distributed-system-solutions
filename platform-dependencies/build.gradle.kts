plugins {
  id("java-platform")
}

javaPlatform {
  allowDependencies()
}

val springBootVersion: String by project

dependencies {
  api(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
  api(platform("io.mongock:mongock-bom:5.5.1"))

}
