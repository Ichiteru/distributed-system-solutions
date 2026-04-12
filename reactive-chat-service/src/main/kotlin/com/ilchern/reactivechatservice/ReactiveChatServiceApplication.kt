package com.ilchern.reactivechatservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ReactiveChatServiceApplication

fun main(args: Array<String>) {
  runApplication<ReactiveChatServiceApplication>(*args)
}
