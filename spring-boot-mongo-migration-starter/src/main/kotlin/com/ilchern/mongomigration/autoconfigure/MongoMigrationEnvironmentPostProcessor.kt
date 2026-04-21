package com.ilchern.mongomigration.autoconfigure

import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.Ordered
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

class MongoMigrationEnvironmentPostProcessor : EnvironmentPostProcessor, Ordered {

  override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
    val scanPackage = environment.getProperty("mongo.migration.scan-package")
    if (!scanPackage.isNullOrBlank() && environment.getProperty("mongock.migration-scan-package").isNullOrBlank()) {
      environment.propertySources.addFirst(
        MapPropertySource(
          "mongoMigrationMongockProperties",
          mapOf("mongock.migration-scan-package" to scanPackage),
        ),
      )
    }
  }

  override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE
}
