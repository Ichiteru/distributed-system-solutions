package com.ilchern.saasbilling.billing.infrastructure.persistence.mapper

import com.ilchern.saasbilling.billing.domain.model.BillingPeriod
import com.ilchern.saasbilling.billing.domain.model.Invoice
import com.ilchern.saasbilling.billing.domain.model.InvoiceId
import com.ilchern.saasbilling.billing.domain.model.InvoiceLine
import com.ilchern.saasbilling.billing.domain.model.InvoiceStatus
import com.ilchern.saasbilling.billing.domain.model.InvoiceType
import com.ilchern.saasbilling.billing.domain.model.Money
import com.ilchern.saasbilling.billing.domain.model.OrganizationId
import com.ilchern.saasbilling.billing.domain.model.PaymentMethodToken
import com.ilchern.saasbilling.billing.domain.model.SubscriptionId
import com.ilchern.saasbilling.billing.domain.model.SubscriptionPlan
import com.ilchern.saasbilling.billing.infrastructure.persistence.entity.InvoiceEntity
import com.ilchern.saasbilling.billing.infrastructure.persistence.entity.InvoiceLineEntity
import org.springframework.stereotype.Component

@Component
class InvoicePersistenceMapper {

  fun toDomain(entity: InvoiceEntity): Invoice =
    Invoice.copy(
      id = InvoiceId(entity.id),
      subscriptionId = SubscriptionId(entity.subscriptionId),
      organizationId = OrganizationId(entity.organizationId),
      invoiceType = InvoiceType.valueOf(entity.invoiceType),
      status = InvoiceStatus.valueOf(entity.status),
      subscriptionPlan = SubscriptionPlan.valueOf(entity.subscriptionPlan),
      billingPeriod = BillingPeriod.valueOf(entity.billingPeriod),
      seats = entity.seats,
      periodStart = entity.periodStart,
      periodEnd = entity.periodEnd,
      amount = Money(
        amountMinor = entity.amountMinor,
        currency = entity.currency,
      ),
      paymentMethodToken = PaymentMethodToken(entity.paymentMethodToken),
      createdAt = entity.createdAt,
      lines = entity.lines.map(::toDomainLine),
    )

  fun copyToEntity(source: Invoice, target: InvoiceEntity) {
    target.id = source.id.value
    target.subscriptionId = source.subscriptionId.value
    target.organizationId = source.organizationId.value
    target.invoiceType = source.invoiceType.name
    target.status = source.status().name
    target.subscriptionPlan = source.subscriptionPlan.name
    target.billingPeriod = source.billingPeriod.name
    target.seats = source.seats
    target.periodStart = source.periodStart
    target.periodEnd = source.periodEnd
    target.amountMinor = source.amount.amountMinor
    target.currency = source.amount.currency
    target.paymentMethodToken = source.paymentMethodToken.value
    target.createdAt = source.createdAt

    target.lines.clear()
    target.lines += source.lines().map { toEntity(it, target) }
  }

  fun newEntity(source: Invoice): InvoiceEntity =
    InvoiceEntity(
      id = source.id.value,
      subscriptionId = source.subscriptionId.value,
      organizationId = source.organizationId.value,
      invoiceType = source.invoiceType.name,
      status = source.status().name,
      subscriptionPlan = source.subscriptionPlan.name,
      billingPeriod = source.billingPeriod.name,
      seats = source.seats,
      periodStart = source.periodStart,
      periodEnd = source.periodEnd,
      amountMinor = source.amount.amountMinor,
      currency = source.amount.currency,
      paymentMethodToken = source.paymentMethodToken.value,
      createdAt = source.createdAt,
    ).also { entity ->
      copyToEntity(source, entity)
    }

  private fun toDomainLine(entity: InvoiceLineEntity): InvoiceLine =
    InvoiceLine(
      id = entity.id,
      description = entity.description,
      quantity = entity.quantity,
      amount = Money(
        amountMinor = entity.amountMinor,
        currency = entity.currency,
      ),
    )

  private fun toEntity(
    source: InvoiceLine,
    invoice: InvoiceEntity,
  ): InvoiceLineEntity =
    InvoiceLineEntity(
      id = source.id,
      invoice = invoice,
      description = source.description,
      quantity = source.quantity,
      amountMinor = source.amount.amountMinor,
      currency = source.amount.currency,
    )
}
