package com.ilchern.saasbilling.payment.infrastructure.persistence.mapper

import com.ilchern.saasbilling.payment.domain.model.InvoiceId
import com.ilchern.saasbilling.payment.domain.model.Money
import com.ilchern.saasbilling.payment.domain.model.OrganizationId
import com.ilchern.saasbilling.payment.domain.model.PaymentAttempt
import com.ilchern.saasbilling.payment.domain.model.PaymentAttemptId
import com.ilchern.saasbilling.payment.domain.model.PaymentAttemptStatus
import com.ilchern.saasbilling.payment.domain.model.PaymentMethodToken
import com.ilchern.saasbilling.payment.domain.model.ProviderPaymentReference
import com.ilchern.saasbilling.payment.domain.model.SubscriptionId
import com.ilchern.saasbilling.payment.infrastructure.persistence.entity.PaymentAttemptEntity
import org.springframework.stereotype.Component

@Component
class PaymentAttemptPersistenceMapper {

  fun toDomain(entity: PaymentAttemptEntity): PaymentAttempt =
    PaymentAttempt.restore(
      id = PaymentAttemptId(entity.id),
      invoiceId = InvoiceId(entity.invoiceId),
      subscriptionId = SubscriptionId(entity.subscriptionId),
      organizationId = OrganizationId(entity.organizationId),
      attemptNumber = entity.attemptNumber,
      amount = Money(
        amountMinor = entity.amountMinor,
        currency = entity.currency,
      ),
      paymentMethodToken = PaymentMethodToken(entity.paymentMethodToken),
      status = PaymentAttemptStatus.valueOf(entity.status),
      providerPaymentReference = entity.providerPaymentId?.let(::ProviderPaymentReference),
      providerStatus = entity.providerStatus,
      createdAt = entity.createdAt,
      submittedAt = entity.submittedAt,
    )

  fun newEntity(source: PaymentAttempt): PaymentAttemptEntity =
    PaymentAttemptEntity(
      id = source.id.value,
      invoiceId = source.invoiceId.value,
      subscriptionId = source.subscriptionId.value,
      organizationId = source.organizationId.value,
      attemptNumber = source.attemptNumber,
      amountMinor = source.amount.amountMinor,
      currency = source.amount.currency,
      paymentMethodToken = source.paymentMethodToken.value,
      status = source.status().name,
      providerPaymentId = source.providerPaymentReference()?.value,
      providerStatus = source.providerStatus(),
      createdAt = source.createdAt,
      submittedAt = source.submittedAt(),
    )

  fun copyToEntity(
    source: PaymentAttempt,
    target: PaymentAttemptEntity,
  ) {
    target.invoiceId = source.invoiceId.value
    target.subscriptionId = source.subscriptionId.value
    target.organizationId = source.organizationId.value
    target.attemptNumber = source.attemptNumber
    target.amountMinor = source.amount.amountMinor
    target.currency = source.amount.currency
    target.paymentMethodToken = source.paymentMethodToken.value
    target.status = source.status().name
    target.providerPaymentId = source.providerPaymentReference()?.value
    target.providerStatus = source.providerStatus()
    target.createdAt = source.createdAt
    target.submittedAt = source.submittedAt()
  }
}
