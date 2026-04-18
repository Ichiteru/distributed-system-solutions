package com.ilchern.reactivechatservice.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

@Configuration
class WorkerConfig(
  @Value("\${chat.session-worker-pool-size:0}")
  private val configuredWorkerPoolSize: Int,
) {

  @Bean
  fun sessionWorkers() : Array<Scheduler> {
    val poolSize = configuredWorkerPoolSize
      .takeIf { it > 0 }
      ?: Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

      return Array(poolSize) { index -> Schedulers.newSingle("session-outbound-$index") }
  }
}