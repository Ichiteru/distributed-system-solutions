package com.ilchern.saasbilling.messaging.autoconfigure

import com.ilchern.saasbilling.messaging.inbox.InboxMessageProcessor
import com.ilchern.saasbilling.messaging.inbox.InboxMessageStore
import com.ilchern.saasbilling.messaging.jpa.inbox.InboxMessageJpaRepository
import com.ilchern.saasbilling.messaging.jpa.inbox.JpaInboxMessageStore
import com.ilchern.saasbilling.messaging.jpa.outbox.JpaTransactionalOutboxMessageStore
import com.ilchern.saasbilling.messaging.jpa.outbox.OutboxMessageJpaRepository
import com.ilchern.saasbilling.messaging.outbox.TransactionalOutboxMessageStore
import jakarta.persistence.EntityManager
import javax.sql.DataSource
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@AutoConfiguration(
  after = [HibernateJpaAutoConfiguration::class],
  before = [JpaRepositoriesAutoConfiguration::class],
)
@ConditionalOnClass(DataSource::class, EntityManager::class)
@ConditionalOnBean(DataSource::class)
@EntityScan(basePackages = ["com.ilchern.saasbilling"])
@EnableJpaRepositories(basePackages = ["com.ilchern.saasbilling"])
class MessagingJpaAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  fun inboxMessageStore(
    inboxMessageJpaRepository: InboxMessageJpaRepository,
  ): InboxMessageStore =
    JpaInboxMessageStore(inboxMessageJpaRepository)

  @Bean
  @ConditionalOnMissingBean
  fun inboxMessageProcessor(
    inboxMessageStore: InboxMessageStore,
  ): InboxMessageProcessor =
    InboxMessageProcessor(inboxMessageStore)

  @Bean
  @ConditionalOnMissingBean
  fun transactionalOutboxMessageStore(
    outboxMessageJpaRepository: OutboxMessageJpaRepository,
  ): TransactionalOutboxMessageStore =
    JpaTransactionalOutboxMessageStore(outboxMessageJpaRepository)
}
