package com.ilchern.saasbilling.subscription.infrastructure.persistence.repository

import com.ilchern.saasbilling.subscription.domain.model.Subscription
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionId
import com.ilchern.saasbilling.subscription.domain.repository.SubscriptionRepository
import com.ilchern.saasbilling.subscription.infrastructure.persistence.mapper.SubscriptionPersistenceMapper
import org.springframework.stereotype.Repository

@Repository
class JpaSubscriptionRepositoryAdapter(
  private val subscriptionJpaRepository: SubscriptionJpaRepository,
  private val subscriptionPersistenceMapper: SubscriptionPersistenceMapper,
) : SubscriptionRepository {

  override fun save(subscription: Subscription): Subscription {
    val entity = subscriptionJpaRepository.findDetailedById(subscription.id.value)
      ?.also { entity -> subscriptionPersistenceMapper.copyToEntity(subscription, entity) }
      ?: subscriptionPersistenceMapper.newEntity(subscription)

    val savedEntity = subscriptionJpaRepository.save(entity)
    return subscriptionPersistenceMapper.toDomain(savedEntity)
  }

  override fun findById(subscriptionId: SubscriptionId): Subscription? =
    subscriptionJpaRepository.findDetailedById(subscriptionId.value)
      ?.let(subscriptionPersistenceMapper::toDomain)
}
