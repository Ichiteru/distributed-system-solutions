package com.ilchern.saasbilling.billing.application.handler

import com.ilchern.saasbilling.billing.application.command.MarkInvoicePaymentPendingCommand
import com.ilchern.saasbilling.billing.application.port.OutboxMessageStore
import com.ilchern.saasbilling.billing.domain.model.Invoice
import com.ilchern.saasbilling.billing.domain.repository.InvoiceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MarkInvoicePaymentPendingHandler(
  private val invoiceRepository: InvoiceRepository,
  private val outboxMessageStore: OutboxMessageStore,
) {

  @Transactional
  fun handle(command: MarkInvoicePaymentPendingCommand): Invoice {
    val invoice = invoiceRepository.findById(command.invoiceId)
      ?: error("Invoice was not found: ${command.invoiceId.value}")

    require(invoice.amount == command.amount) {
      "Payment amount does not match invoice amount"
    }

    invoice.markPaymentPending(
      paymentPendingAt = command.occurredAt,
      failureCode = command.failureCode,
      failureMessage = command.failureMessage,
    )
    val savedInvoice = invoiceRepository.save(invoice)
    outboxMessageStore.append(invoice.pullDomainEvents())
    return savedInvoice
  }
}
