package com.ilchern.saasbilling.subscription.infrastructure.messaging.outbox

import com.ilchern.saasbilling.subscription.infrastructure.messaging.kafka.SubscriptionEventPublisher
import java.time.Clock
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

@Component
class JpaOutboxPublisherJob(
  private val outboxMessageJpaRepository: OutboxMessageJpaRepository,
  private val subscriptionEventPublisher: SubscriptionEventPublisher,
  private val transactionTemplate: TransactionTemplate,
  private val clock: Clock,
  @Value("\${subscription.outbox.publisher.batch-size:100}")
  private val batchSize: Int,
) : OutboxPublisherJob {

  @Scheduled(fixedDelayString = "\${subscription.outbox.publisher.fixed-delay-ms:5000}")
  override fun publishUnpublishedMessages() {
    val batch = outboxMessageJpaRepository.findAllByPublishedFalseOrderByOccurredAtAscIdAsc(
      pageable = PageRequest.of(0, batchSize),
    )

    batch.forEach { message ->
      subscriptionEventPublisher.publish(message)
      markPublished(message.id)
    }
  }

  private fun markPublished(messageId: UUID) {
    transactionTemplate.executeWithoutResult {
      val message = outboxMessageJpaRepository.findById(messageId)
        .orElseThrow { IllegalStateException("Outbox message $messageId not found") }

      if (!message.published) {
        message.markPublished(clock.instant())
        outboxMessageJpaRepository.save(message)
      }
    }
  }
}
