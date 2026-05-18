package com.ilchern.saasbilling.subscription

import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.saasbilling.subscription.application.command.ActivateSubscriptionCommand
import com.ilchern.saasbilling.subscription.application.command.SuspendSubscriptionCommand
import com.ilchern.saasbilling.subscription.application.handler.ActivateSubscriptionHandler
import com.ilchern.saasbilling.subscription.application.handler.SuspendSubscriptionHandler
import com.ilchern.saasbilling.subscription.domain.model.OrganizationId
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Testcontainers
@ActiveProfiles("local")
@TestPropertySource(properties = ["spring.kafka.listener.auto-startup=false"])
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SubscriptionHandlerIntegrationTest(
  @Autowired private val activateSubscriptionHandler: ActivateSubscriptionHandler,
  @Autowired private val suspendSubscriptionHandler: SuspendSubscriptionHandler,
  @Autowired private val jdbcTemplate: JdbcTemplate,
  @Autowired private val objectMapper: ObjectMapper,
) {

  @BeforeEach
  fun cleanDatabase() {
    jdbcTemplate.execute(
      """
      truncate table
        outbox_messages,
        inbox_messages,
        idempotency_keys,
        subscription_history,
        subscription_changes,
        subscriptions
      restart identity cascade
      """.trimIndent(),
    )
  }

  @Test
  fun `activate subscription moves pending subscription to active and writes outbox event`() {
    val subscriptionId = UUID.randomUUID()
    val organizationId = "org-handler-activate"
    val occurredAt = Instant.parse("2026-05-18T10:15:30Z")
    insertSubscription(
      subscriptionId = subscriptionId,
      organizationId = organizationId,
      status = "PENDING",
    )

    activateSubscriptionHandler.handle(
      ActivateSubscriptionCommand(
        subscriptionId = SubscriptionId(subscriptionId),
        organizationId = OrganizationId(organizationId),
        messageId = UUID.randomUUID(),
        correlationId = UUID.randomUUID(),
        causationId = UUID.randomUUID(),
        occurredAt = occurredAt,
      ),
    )

    assertThat(subscriptionStatus(subscriptionId)).isEqualTo("ACTIVE")
    assertThat(historyActions(subscriptionId)).containsExactly("SUBSCRIPTION_ACTIVATED")

    val outbox = singleOutboxMessage(subscriptionId)
    assertThat(outbox["aggregatetype"]).isEqualTo("subscription")
    assertThat(outbox["aggregateid"]).isEqualTo(subscriptionId.toString())
    assertThat(outbox["type"]).isEqualTo("SubscriptionActivatedEvent")

    val payload = objectMapper.readTree(outbox["payload"].toString())
    assertThat(payload["subscriptionId"].asText()).isEqualTo(subscriptionId.toString())
    assertThat(payload["organizationId"].asText()).isEqualTo(organizationId)
    assertThat(payload["activatedAt"].asText()).isEqualTo(occurredAt.toString())
  }

  @Test
  fun `activate subscription is idempotent for already active subscription`() {
    val subscriptionId = UUID.randomUUID()
    val organizationId = "org-handler-active"
    insertSubscription(
      subscriptionId = subscriptionId,
      organizationId = organizationId,
      status = "ACTIVE",
    )

    activateSubscriptionHandler.handle(
      ActivateSubscriptionCommand(
        subscriptionId = SubscriptionId(subscriptionId),
        organizationId = OrganizationId(organizationId),
        messageId = UUID.randomUUID(),
        correlationId = null,
        causationId = null,
        occurredAt = Instant.parse("2026-05-18T10:16:30Z"),
      ),
    )

    assertThat(subscriptionStatus(subscriptionId)).isEqualTo("ACTIVE")
    assertThat(countRows("subscription_history")).isEqualTo(0)
    assertThat(countRows("outbox_messages")).isEqualTo(0)
  }

  @Test
  fun `suspend subscription moves pending subscription to suspended and writes outbox event`() {
    val subscriptionId = UUID.randomUUID()
    val organizationId = "org-handler-suspend"
    val occurredAt = Instant.parse("2026-05-18T11:15:30Z")
    insertSubscription(
      subscriptionId = subscriptionId,
      organizationId = organizationId,
      status = "PENDING",
    )

    suspendSubscriptionHandler.handle(
      SuspendSubscriptionCommand(
        subscriptionId = SubscriptionId(subscriptionId),
        organizationId = OrganizationId(organizationId),
        messageId = UUID.randomUUID(),
        correlationId = UUID.randomUUID(),
        causationId = UUID.randomUUID(),
        occurredAt = occurredAt,
      ),
    )

    assertThat(subscriptionStatus(subscriptionId)).isEqualTo("SUSPENDED")
    assertThat(historyActions(subscriptionId)).containsExactly("SUBSCRIPTION_SUSPENDED")

    val outbox = singleOutboxMessage(subscriptionId)
    assertThat(outbox["aggregatetype"]).isEqualTo("subscription")
    assertThat(outbox["aggregateid"]).isEqualTo(subscriptionId.toString())
    assertThat(outbox["type"]).isEqualTo("SubscriptionSuspendedEvent")

    val payload = objectMapper.readTree(outbox["payload"].toString())
    assertThat(payload["subscriptionId"].asText()).isEqualTo(subscriptionId.toString())
    assertThat(payload["organizationId"].asText()).isEqualTo(organizationId)
    assertThat(payload["suspendedAt"].asText()).isEqualTo(occurredAt.toString())
  }

  @Test
  fun `suspend subscription is idempotent for already suspended subscription`() {
    val subscriptionId = UUID.randomUUID()
    val organizationId = "org-handler-suspended"
    insertSubscription(
      subscriptionId = subscriptionId,
      organizationId = organizationId,
      status = "SUSPENDED",
    )

    suspendSubscriptionHandler.handle(
      SuspendSubscriptionCommand(
        subscriptionId = SubscriptionId(subscriptionId),
        organizationId = OrganizationId(organizationId),
        messageId = UUID.randomUUID(),
        correlationId = null,
        causationId = null,
        occurredAt = Instant.parse("2026-05-18T11:16:30Z"),
      ),
    )

    assertThat(subscriptionStatus(subscriptionId)).isEqualTo("SUSPENDED")
    assertThat(countRows("subscription_history")).isEqualTo(0)
    assertThat(countRows("outbox_messages")).isEqualTo(0)
  }

  private fun insertSubscription(
    subscriptionId: UUID,
    organizationId: String,
    status: String,
  ) {
    jdbcTemplate.update(
      """
      insert into subscriptions (
        id,
        organization_id,
        created_at,
        status,
        subscription_plan,
        billing_period,
        seats,
        payment_method_token
      )
      values (?, ?, ?, ?, ?, ?, ?, ?)
      """.trimIndent(),
      subscriptionId,
      organizationId,
      Timestamp.from(Instant.parse("2026-05-18T09:00:00Z")),
      status,
      "PRO",
      "MONTHLY",
      5,
      "pm_test_handler",
    )
  }

  private fun subscriptionStatus(subscriptionId: UUID): String =
    requireNotNull(
      jdbcTemplate.queryForObject(
        "select status from subscriptions where id = ?",
        String::class.java,
        subscriptionId,
      ),
    )

  private fun historyActions(subscriptionId: UUID): List<String> =
    jdbcTemplate.queryForList(
      """
      select action
      from subscription_history
      where subscription_id = ?
      order by occurred_at, id
      """.trimIndent(),
      String::class.java,
      subscriptionId,
    )

  private fun singleOutboxMessage(subscriptionId: UUID): Map<String, Any> =
    jdbcTemplate.queryForMap(
      """
      select aggregatetype, aggregateid, type, payload
      from outbox_messages
      where aggregateid = ?
      """.trimIndent(),
      subscriptionId.toString(),
    )

  private fun countRows(table: String): Long =
    requireNotNull(jdbcTemplate.queryForObject("select count(*) from $table", Long::class.java))

  companion object {
    @Container
    @JvmStatic
    private val postgres = HandlerPostgreSQLContainer("postgres:16-alpine")
      .withDatabaseName("subscription_service_db")
      .withUsername("subscription_service_user")
      .withPassword("subscription_service_password")

    @JvmStatic
    @DynamicPropertySource
    fun registerProperties(registry: DynamicPropertyRegistry) {
      registry.add("SPRING.DATASOURCE.URL", postgres::getJdbcUrl)
      registry.add("SPRING.DATASOURCE.USERNAME", postgres::getUsername)
      registry.add("SPRING.DATASOURCE.PASSWORD", postgres::getPassword)
      registry.add("SPRING.KAFKA.BOOTSTRAP_SERVERS") { "127.0.0.1:65535" }
      registry.add("SPRING.KAFKA.PROPERTIES.SCHEMA.REGISTRY.URL") { "mock://subscription-handler-test" }
    }
  }
}

private class HandlerPostgreSQLContainer(imageName: String) : PostgreSQLContainer<HandlerPostgreSQLContainer>(imageName)
