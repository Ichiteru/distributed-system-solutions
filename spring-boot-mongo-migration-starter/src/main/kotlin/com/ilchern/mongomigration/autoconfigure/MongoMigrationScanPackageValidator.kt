package com.ilchern.mongomigration.autoconfigure

import org.springframework.beans.factory.InitializingBean

class MongoMigrationScanPackageValidator(
  private val properties: MongoMigrationProperties,
) : InitializingBean {

  override fun afterPropertiesSet() {
    require(!properties.scanPackage.isNullOrBlank()) {
      "Property 'mongo.migration.scan-package' must be configured to enable Mongo migrations"
    }
  }
}
