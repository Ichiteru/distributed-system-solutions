package com.ilchern.mongomigration.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "mongo.migration")
data class MongoMigrationProperties(
  val scanPackage: String? = null,
)
