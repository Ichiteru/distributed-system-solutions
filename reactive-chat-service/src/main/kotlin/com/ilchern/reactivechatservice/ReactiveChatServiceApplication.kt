package com.ilchern.reactivechatservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.core.annotation.Collation
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@SpringBootApplication
//@EnableReactiveMongoRepositories
class ReactiveChatServiceApplication

fun main(args: Array<String>) {
  runApplication<ReactiveChatServiceApplication>(*args)
}
