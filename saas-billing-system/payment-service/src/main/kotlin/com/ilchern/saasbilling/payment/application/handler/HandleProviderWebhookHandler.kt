package com.ilchern.saasbilling.payment.application.handler

import com.ilchern.saasbilling.payment.application.command.HandleProviderWebhookCommand
import com.ilchern.saasbilling.payment.application.port.OutboxMessageStore
import com.ilchern.saasbilling.payment.application.port.ProviderWebhookEvent
import com.ilchern.saasbilling.payment.application.port.ProviderWebhookEventStore
import com.ilchern.saasbilling.payment.domain.model.PaymentAttempt
import com.ilchern.saasbilling.payment.domain.repository.PaymentAttemptRepository
import java.time.Clock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class HandleProviderWebhookHandler(
  private val clock: Clock,
  private val paymentAttemptRepository: PaymentAttemptRepository,
  private val providerWebhookEventStore: ProviderWebhookEventStore,
  private val outboxMessageStore: OutboxMessageStore,
) {

  @Transactional
  fun handle(command: HandleProviderWebhookCommand): PaymentAttempt? {
    val saved = providerWebhookEventStore.saveIfAbsent(
      ProviderWebhookEvent(
        providerEventId = command.providerEventId,
        providerPaymentId = command.providerPaymentReference.value,
        type = command.type,
        status = command.status,
        receivedAt = clock.instant(),
      ),
    )
    if (!saved) {
      return paymentAttemptRepository.findByProviderPaymentReference(command.providerPaymentReference)
    }

    val paymentAttempt = paymentAttemptRepository.findByProviderPaymentReference(command.providerPaymentReference)
      ?: error("Payment attempt was not found for provider payment ${command.providerPaymentReference.value}")

    require(paymentAttempt.invoiceId == command.invoiceId) {
      "Webhook invoiceId ${command.invoiceId.value} does not match payment attempt invoiceId ${paymentAttempt.invoiceId.value}"
    }
    require(paymentAttempt.amount == command.amount) {
      "Webhook amount does not match payment attempt amount"
    }

    when (command.status) {
      PROVIDER_STATUS_SUCCEEDED -> {
        paymentAttempt.markSucceeded(
          providerStatus = command.status,
          completedAt = command.occurredAt,
        )
      }
      PROVIDER_STATUS_FAILED -> {
        paymentAttempt.markFailed(
          providerStatus = command.status,
          failureCode = command.failureCode,
          failureMessage = command.failureMessage,
          completedAt = command.occurredAt,
        )
      }
      else -> error("Unsupported provider payment status: ${command.status}")
    }

    val savedAttempt = paymentAttemptRepository.save(paymentAttempt)
    outboxMessageStore.append(paymentAttempt.pullDomainEvents())
    return savedAttempt
  }

  companion object {
    private const val PROVIDER_STATUS_SUCCEEDED = "succeeded"
    private const val PROVIDER_STATUS_FAILED = "failed"
  }
}
