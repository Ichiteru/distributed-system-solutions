package com.ilchern.saasbilling.payment.application.port

import com.ilchern.saasbilling.payment.domain.event.PaymentDomainEvent

interface OutboxMessageStore {
  fun append(events: List<PaymentDomainEvent>)
}
