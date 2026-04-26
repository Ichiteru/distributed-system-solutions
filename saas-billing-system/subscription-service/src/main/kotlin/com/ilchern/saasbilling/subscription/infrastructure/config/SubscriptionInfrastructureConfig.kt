package com.ilchern.saasbilling.subscription.infrastructure.config

import java.time.Clock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
class SubscriptionInfrastructureConfig {

  @Bean
  fun clock(): Clock = Clock.systemUTC()
}
