package com.ilchern.saasbilling.payment

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class PaymentIntegrationTestConfig {

  @Bean
  @Primary
  fun recordingPaymentProviderClient(): RecordingPaymentProviderClient =
    RecordingPaymentProviderClient()
}
