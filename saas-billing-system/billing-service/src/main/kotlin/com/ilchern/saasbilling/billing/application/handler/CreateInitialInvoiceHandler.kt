package com.ilchern.saasbilling.billing.application.handler

import com.ilchern.saasbilling.billing.application.command.CreateInitialInvoiceCommand
import com.ilchern.saasbilling.billing.application.port.OutboxMessageStore
import com.ilchern.saasbilling.billing.application.service.InvoicePricingService
import com.ilchern.saasbilling.billing.domain.model.BillingPeriod
import com.ilchern.saasbilling.billing.domain.model.Invoice
import com.ilchern.saasbilling.billing.domain.model.InvoiceType
import com.ilchern.saasbilling.billing.domain.repository.InvoiceRepository
import java.time.Clock
import java.time.ZoneOffset
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateInitialInvoiceHandler(
  private val clock: Clock,
  private val invoiceRepository: InvoiceRepository,
  private val invoicePricingService: InvoicePricingService,
  private val outboxMessageStore: OutboxMessageStore,
) {

  @Transactional
  fun handle(command: CreateInitialInvoiceCommand): Invoice {
    val periodStart = command.occurredAt
    val periodEnd = when (command.billingPeriod) {
      BillingPeriod.MONTHLY -> periodStart.atZone(ZoneOffset.UTC).plusMonths(1).toInstant()
      BillingPeriod.YEARLY -> periodStart.atZone(ZoneOffset.UTC).plusYears(1).toInstant()
    }

    invoiceRepository.findBySubscriptionPeriod(
      subscriptionId = command.subscriptionId,
      invoiceType = InvoiceType.INITIAL,
      periodStart = periodStart,
      periodEnd = periodEnd,
    )?.let { return it }

    val invoice = Invoice.createInitial(
      subscriptionId = command.subscriptionId,
      organizationId = command.organizationId,
      subscriptionPlan = command.subscriptionPlan,
      billingPeriod = command.billingPeriod,
      seats = command.seats,
      periodStart = periodStart,
      periodEnd = periodEnd,
      amount = invoicePricingService.calculate(
        subscriptionPlan = command.subscriptionPlan,
        billingPeriod = command.billingPeriod,
        seats = command.seats,
      ),
      paymentMethodToken = command.paymentMethodToken,
      createdAt = clock.instant(),
    )

    val savedInvoice = invoiceRepository.save(invoice)
    outboxMessageStore.append(invoice.pullDomainEvents())
    return savedInvoice
  }

}
