package com.ilchern.saasbilling.payment.infrastructure.provider.wiremock

import com.ilchern.saasbilling.payment.application.port.PaymentProviderClient
import com.ilchern.saasbilling.payment.application.port.SubmitProviderPaymentRequest
import com.ilchern.saasbilling.payment.application.port.SubmitProviderPaymentResponse
import com.ilchern.saasbilling.payment.domain.model.ProviderPaymentReference
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class WireMockPaymentProviderClient(
  paymentProviderProperties: PaymentProviderProperties,
) : PaymentProviderClient {

  private val restClient = RestClient.builder()
    .baseUrl(paymentProviderProperties.baseUrl)
    .requestFactory(SimpleClientHttpRequestFactory())
    .build()

  override fun submitPayment(request: SubmitProviderPaymentRequest): SubmitProviderPaymentResponse {
    val response = restClient.post()
      .uri("/payments")
      .header(IDEMPOTENCY_KEY_HEADER, request.idempotencyKey)
      .body(
        WireMockSubmitPaymentRequest(
          invoiceId = request.invoiceId.value.toString(),
          subscriptionId = request.subscriptionId.value.toString(),
          organizationId = request.organizationId.value,
          amountMinor = request.amount.amountMinor,
          currency = request.amount.currency,
          paymentMethodToken = request.paymentMethodToken.value,
        ),
      )
      .retrieve()
      .body(WireMockSubmitPaymentResponse::class.java)
      ?: error("Payment provider returned empty response")

    return SubmitProviderPaymentResponse(
      providerPaymentReference = ProviderPaymentReference(response.providerPaymentId),
      providerStatus = response.status,
    )
  }

  data class WireMockSubmitPaymentRequest(
    val invoiceId: String,
    val subscriptionId: String,
    val organizationId: String,
    val amountMinor: Long,
    val currency: String,
    val paymentMethodToken: String,
  )

  data class WireMockSubmitPaymentResponse(
    val providerPaymentId: String,
    val status: String,
  )

  companion object {
    private const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"
  }
}
