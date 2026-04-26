package com.ilchern.saasbilling.subscription.application.port

import com.ilchern.saasbilling.subscription.domain.model.OrganizationId
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionId

interface IdempotencyKeyStore {

  fun findSubscriptionId(
    organizationId: OrganizationId,
    operation: String,
    idempotencyKey: String,
  ): SubscriptionId?

  fun save(
    organizationId: OrganizationId,
    operation: String,
    idempotencyKey: String,
    subscriptionId: SubscriptionId,
  )
}
