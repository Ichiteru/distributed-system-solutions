package com.ilchern.saasbilling.billing.application.port

import com.ilchern.saasbilling.billing.domain.event.BillingDomainEvent

interface OutboxMessageStore {

  fun append(events: List<BillingDomainEvent>)
}
