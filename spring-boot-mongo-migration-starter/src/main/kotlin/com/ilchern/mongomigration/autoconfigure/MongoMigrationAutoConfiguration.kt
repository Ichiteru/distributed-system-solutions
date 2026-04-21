package com.ilchern.mongomigration.autoconfigure

import io.mongock.runner.springboot.EnableMongock
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.core.MongoTemplate

@AutoConfiguration(after = [MongoDataAutoConfiguration::class])
@ConditionalOnClass(EnableMongock::class, MongoTemplate::class)
@ConditionalOnBean(MongoTemplate::class)
@EnableConfigurationProperties(MongoMigrationProperties::class)
@EnableMongock
class MongoMigrationAutoConfiguration {

  @Bean
  fun mongoMigrationScanPackageValidator(properties: MongoMigrationProperties): MongoMigrationScanPackageValidator {
    return MongoMigrationScanPackageValidator(properties)
  }
}
