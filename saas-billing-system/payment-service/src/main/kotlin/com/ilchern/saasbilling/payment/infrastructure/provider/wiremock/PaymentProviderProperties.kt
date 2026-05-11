package com.ilchern.saasbilling.payment.infrastructure.provider.wiremock

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "payment.provider")
data class PaymentProviderProperties(
  val baseUrl: String,
)
