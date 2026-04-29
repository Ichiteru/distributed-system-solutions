package com.ilchern.saasbilling.subscription.infrastructure.persistence.mapper

import com.ilchern.saasbilling.subscription.domain.model.BillingPeriod
import com.ilchern.saasbilling.subscription.domain.model.OrganizationId
import com.ilchern.saasbilling.subscription.domain.model.PaymentMethodToken
import com.ilchern.saasbilling.subscription.domain.model.Subscription
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionChange
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionHistoryEntry
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionId
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionPlan
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionStatus
import com.ilchern.saasbilling.subscription.infrastructure.persistence.entity.SubscriptionChangeEntity
import com.ilchern.saasbilling.subscription.infrastructure.persistence.entity.SubscriptionEntity
import com.ilchern.saasbilling.subscription.infrastructure.persistence.entity.SubscriptionHistoryEntryEntity
import org.springframework.stereotype.Component

@Component
class SubscriptionPersistenceMapper {

  fun toDomain(entity: SubscriptionEntity): Subscription =
    Subscription.copy(
      id = SubscriptionId(entity.id),
      organizationId = OrganizationId(entity.organizationId),
      createdAt = entity.createdAt,
      status = SubscriptionStatus.valueOf(entity.status),
      subscriptionPlan = SubscriptionPlan.valueOf(entity.subscriptionPlan),
      billingPeriod = BillingPeriod.valueOf(entity.billingPeriod),
      seats = entity.seats,
      paymentMethodToken = PaymentMethodToken(entity.paymentMethodToken),
      pendingSubscriptionChange = entity.pendingSubscriptionChange?.let(::toDomainChange),
      historyEntries = entity.historyEntries
        .sortedWith(compareBy<SubscriptionHistoryEntryEntity> { it.occurredAt }.thenBy { it.id })
        .map(::toDomainHistoryEntry),
    )

  fun copyToEntity(source: Subscription, target: SubscriptionEntity) {
    target.id = source.id.value
    target.organizationId = source.organizationId.value
    target.createdAt = source.createdAt
    target.status = source.status().name
    target.subscriptionPlan = source.subscriptionPlan().name
    target.billingPeriod = source.billingPeriod().name
    target.seats = source.seats()
    target.paymentMethodToken = source.paymentMethodToken().value

    val pendingChange = source.pendingSubscriptionChange()
    target.pendingSubscriptionChange = pendingChange?.let { toEntity(it, target) }

    target.historyEntries.clear()
    target.historyEntries += source.historyEntries().map { toEntity(it, target) }
  }

  fun newEntity(source: Subscription): SubscriptionEntity =
    SubscriptionEntity(
      id = source.id.value,
      organizationId = source.organizationId.value,
      createdAt = source.createdAt,
      status = source.status().name,
      subscriptionPlan = source.subscriptionPlan().name,
      billingPeriod = source.billingPeriod().name,
      seats = source.seats(),
      paymentMethodToken = source.paymentMethodToken().value,
    ).also { entity ->
      copyToEntity(source, entity)
    }

  private fun toDomainChange(entity: SubscriptionChangeEntity): SubscriptionChange =
    SubscriptionChange(
      id = entity.id,
      requestedAt = entity.requestedAt,
      newPlan = SubscriptionPlan.valueOf(entity.newPlan),
      newSeats = entity.newSeats,
    )

  private fun toDomainHistoryEntry(entity: SubscriptionHistoryEntryEntity): SubscriptionHistoryEntry =
    SubscriptionHistoryEntry(
      id = entity.id,
      action = entity.action,
      occurredAt = entity.occurredAt,
      details = entity.details.toMap(),
    )

  private fun toEntity(source: SubscriptionChange, subscription: SubscriptionEntity): SubscriptionChangeEntity =
    SubscriptionChangeEntity(
      id = source.id,
      subscription = subscription,
      requestedAt = source.requestedAt,
      newPlan = source.newPlan.name,
      newSeats = source.newSeats,
    )

  private fun toEntity(
    source: SubscriptionHistoryEntry,
    subscription: SubscriptionEntity,
  ): SubscriptionHistoryEntryEntity =
    SubscriptionHistoryEntryEntity(
      id = source.id,
      subscription = subscription,
      action = source.action,
      occurredAt = source.occurredAt,
      details = source.details.toMutableMap(),
    )
}
