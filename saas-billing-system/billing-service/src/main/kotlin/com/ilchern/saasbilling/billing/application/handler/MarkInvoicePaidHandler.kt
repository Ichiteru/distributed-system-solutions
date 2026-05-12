package com.ilchern.saasbilling.billing.application.handler

import com.ilchern.saasbilling.billing.application.command.MarkInvoicePaidCommand
import com.ilchern.saasbilling.billing.application.port.OutboxMessageStore
import com.ilchern.saasbilling.billing.domain.model.Invoice
import com.ilchern.saasbilling.billing.domain.repository.InvoiceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MarkInvoicePaidHandler(
  private val invoiceRepository: InvoiceRepository,
  private val outboxMessageStore: OutboxMessageStore,
) {

  @Transactional
  fun handle(command: MarkInvoicePaidCommand): Invoice {
    val invoice = invoiceRepository.findById(command.invoiceId)
      ?: error("Invoice was not found: ${command.invoiceId.value}")

    require(invoice.amount == command.amount) {
      "Payment amount does not match invoice amount"
    }

    invoice.markPaid(command.occurredAt)
    val savedInvoice = invoiceRepository.save(invoice)
    outboxMessageStore.append(invoice.pullDomainEvents())
    return savedInvoice
  }
}
