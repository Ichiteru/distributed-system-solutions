package com.ilchern.saasbilling.payment.application.handler

import com.ilchern.saasbilling.payment.application.command.SubmitPaymentCommand
import com.ilchern.saasbilling.payment.application.port.PaymentProviderClient
import com.ilchern.saasbilling.payment.application.port.SubmitProviderPaymentRequest
import com.ilchern.saasbilling.payment.domain.model.PaymentAttempt
import com.ilchern.saasbilling.payment.domain.repository.PaymentAttemptRepository
import com.ilchern.saasbilling.payment.domain.repository.PaymentMethodRepository
import java.time.Clock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SubmitPaymentHandler(
  private val clock: Clock,
  private val paymentAttemptRepository: PaymentAttemptRepository,
  private val paymentMethodRepository: PaymentMethodRepository,
  private val paymentProviderClient: PaymentProviderClient,
) {

  @Transactional
  fun handle(command: SubmitPaymentCommand): PaymentAttempt {
    val latestAttempt = paymentAttemptRepository.findLatestByInvoiceId(command.invoiceId)
    latestAttempt?.let { existing ->
      if (!existing.canStartNextAttempt()) {
        return if (existing.isCreated()) {
          submit(existing)
        } else {
          existing
        }
      }
    }

    val now = clock.instant()
    paymentMethodRepository.saveReference(
      organizationId = command.organizationId,
      token = command.paymentMethodToken,
      observedAt = now,
    )

    val paymentAttempt = PaymentAttempt.create(
      invoiceId = command.invoiceId,
      subscriptionId = command.subscriptionId,
      organizationId = command.organizationId,
      attemptNumber = latestAttempt?.attemptNumber?.plus(1) ?: FIRST_ATTEMPT_NUMBER,
      amount = command.amount,
      paymentMethodToken = command.paymentMethodToken,
      createdAt = now,
    )

    return submit(paymentAttempt)
  }

  private fun submit(paymentAttempt: PaymentAttempt): PaymentAttempt {
    val providerResponse = paymentProviderClient.submitPayment(
      SubmitProviderPaymentRequest(
        invoiceId = paymentAttempt.invoiceId,
        subscriptionId = paymentAttempt.subscriptionId,
        organizationId = paymentAttempt.organizationId,
        amount = paymentAttempt.amount,
        paymentMethodToken = paymentAttempt.paymentMethodToken,
        idempotencyKey = "${paymentAttempt.invoiceId.value}:${paymentAttempt.attemptNumber}",
      ),
    )

    paymentAttempt.markSubmitted(
      providerPaymentReference = providerResponse.providerPaymentReference,
      providerStatus = providerResponse.providerStatus,
      submittedAt = clock.instant(),
    )
    return paymentAttemptRepository.save(paymentAttempt)
  }

  companion object {
    private const val FIRST_ATTEMPT_NUMBER = 1
  }
}
