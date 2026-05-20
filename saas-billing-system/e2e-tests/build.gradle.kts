plugins {
  id("org.jetbrains.kotlin.jvm")
}

description = "SaaS Billing end-to-end tests"

val dockerExecutable = listOf(
  "/usr/local/bin/docker",
  "/opt/homebrew/bin/docker",
  "/usr/bin/docker",
).firstOrNull { file(it).exists() } ?: "docker"

val composeCoreInfraUp by tasks.registering(Exec::class) {
  group = "verification"
  description = "Starts core SaaS Billing infrastructure containers for e2e tests."
  workingDir = layout.projectDirectory.dir("..").asFile
  commandLine(
    dockerExecutable,
    "compose",
    "-f",
    "docker-compose.yml",
    "up",
    "-d",
    "subscription-postgres",
    "billing-postgres",
    "orchestrator-postgres",
    "payment-postgres",
    "zookeeper",
    "wiremock",
  )
}

val composeMessagingUp by tasks.registering(Exec::class) {
  group = "verification"
  description = "Starts Kafka, Schema Registry and Kafka Connect for e2e tests."
  dependsOn(composeCoreInfraUp)
  workingDir = layout.projectDirectory.dir("..").asFile
  commandLine(
    dockerExecutable,
    "compose",
    "-f",
    "docker-compose.yml",
    "up",
    "-d",
    "--build",
    "kafka",
    "kafka-init-topics",
    "schema-registry",
    "kafka-connect",
    "kafka-connect-init",
    "kafka-ui",
  )
}

val composeUp by tasks.registering(Exec::class) {
  group = "verification"
  description = "Starts the full SaaS Billing docker-compose environment for e2e tests."
  dependsOn(composeMessagingUp)
  workingDir = layout.projectDirectory.dir("..").asFile
  commandLine(
    dockerExecutable,
    "compose",
    "-f",
    "docker-compose.yml",
    "up",
    "-d",
    "--build",
    "subscription-service",
    "billing-service",
    "billing-orchestrator",
    "payment-service",
  )
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.assertj:assertj-core")
  testImplementation("org.awaitility:awaitility")
  testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  testImplementation("org.postgresql:postgresql")

  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

//tasks.test {
//  dependsOn(composeUp)
//  outputs.upToDateWhen { false }
//}
