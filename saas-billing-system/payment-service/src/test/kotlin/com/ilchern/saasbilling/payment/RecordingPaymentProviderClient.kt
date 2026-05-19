package com.ilchern.saasbilling.payment

import com.ilchern.saasbilling.payment.application.port.PaymentProviderClient
import com.ilchern.saasbilling.payment.application.port.SubmitProviderPaymentRequest
import com.ilchern.saasbilling.payment.application.port.SubmitProviderPaymentResponse
import com.ilchern.saasbilling.payment.domain.model.ProviderPaymentReference

class RecordingPaymentProviderClient : PaymentProviderClient {
  private val submittedRequests = mutableListOf<SubmitProviderPaymentRequest>()
  private var nextProviderStatus = "accepted"

  override fun submitPayment(request: SubmitProviderPaymentRequest): SubmitProviderPaymentResponse {
    submittedRequests += request
    return SubmitProviderPaymentResponse(
      providerPaymentReference = ProviderPaymentReference("provider-${request.idempotencyKey}"),
      providerStatus = nextProviderStatus,
    )
  }

  fun reset() {
    submittedRequests.clear()
    nextProviderStatus = "accepted"
  }

  fun submittedRequests(): List<SubmitProviderPaymentRequest> = submittedRequests.toList()
}
