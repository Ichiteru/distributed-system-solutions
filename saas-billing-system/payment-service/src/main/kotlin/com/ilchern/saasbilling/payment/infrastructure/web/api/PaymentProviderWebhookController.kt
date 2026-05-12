package com.ilchern.saasbilling.payment.infrastructure.web.api

import com.ilchern.saasbilling.payment.application.command.HandleProviderWebhookCommand
import com.ilchern.saasbilling.payment.application.handler.HandleProviderWebhookHandler
import com.ilchern.saasbilling.payment.domain.model.InvoiceId
import com.ilchern.saasbilling.payment.domain.model.Money
import com.ilchern.saasbilling.payment.domain.model.ProviderPaymentReference
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.time.Instant
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/webhooks/payment-provider")
class PaymentProviderWebhookController(
  private val handleProviderWebhookHandler: HandleProviderWebhookHandler,
) {

  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  fun handle(
    @Valid @RequestBody request: PaymentProviderWebhookRequest,
  ) {
    handleProviderWebhookHandler.handle(
      HandleProviderWebhookCommand(
        providerEventId = request.providerEventId,
        providerPaymentReference = ProviderPaymentReference(request.providerPaymentId),
        type = request.type,
        status = request.status,
        invoiceId = InvoiceId(UUID.fromString(request.invoiceId)),
        amount = Money(
          amountMinor = request.amountMinor,
          currency = request.currency,
        ),
        occurredAt = request.occurredAt,
        failureCode = request.failureCode,
        failureMessage = request.failureMessage,
      ),
    )
  }
}

data class PaymentProviderWebhookRequest(
  @field:NotBlank
  val providerEventId: String,

  @field:NotBlank
  val providerPaymentId: String,

  @field:NotBlank
  val type: String,

  @field:NotBlank
  val status: String,

  @field:NotBlank
  val invoiceId: String,

  @field:Positive
  val amountMinor: Long,

  @field:NotBlank
  val currency: String,

  val occurredAt: Instant,
  val failureCode: String? = null,
  val failureMessage: String? = null,
)
