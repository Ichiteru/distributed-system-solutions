package com.ilchern.saasbilling.subscription

import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.saasbilling.subscription.infrastructure.messaging.kafka.SubscriptionCommandListener
import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
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
class SubscriptionKafkaListenerIntegrationTest(
  @Autowired private val listener: SubscriptionCommandListener,
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
  fun `ActivateSubscription envelope activates subscription through inbox and handler`() {
    val subscriptionId = UUID.randomUUID()
    val organizationId = "org-listener-activate"
    val messageId = UUID.randomUUID()
    val occurredAt = Instant.parse("2026-05-18T12:15:30Z")
    insertSubscription(subscriptionId, organizationId, "PENDING")

    listener.onMessage(
      commandEnvelope(
        messageId = messageId,
        messageType = "ActivateSubscription",
        subscriptionId = subscriptionId,
        organizationId = organizationId,
        occurredAt = occurredAt,
      ),
    )

    assertThat(subscriptionStatus(subscriptionId)).isEqualTo("ACTIVE")
    assertThat(inboxMessages("subscription.activate-subscription", messageId)).isEqualTo(1)
    assertThat(historyActions(subscriptionId)).containsExactly("SUBSCRIPTION_ACTIVATED")

    val outbox = singleOutboxMessage(subscriptionId)
    assertThat(outbox["type"]).isEqualTo("SubscriptionActivatedEvent")
    val payload = objectMapper.readTree(outbox["payload"].toString())
    assertThat(payload["subscriptionId"].asText()).isEqualTo(subscriptionId.toString())
    assertThat(payload["organizationId"].asText()).isEqualTo(organizationId)
    assertThat(payload["activatedAt"].asText()).isEqualTo(occurredAt.toString())
  }

  @Test
  fun `SuspendSubscription envelope suspends subscription through inbox and handler`() {
    val subscriptionId = UUID.randomUUID()
    val organizationId = "org-listener-suspend"
    val messageId = UUID.randomUUID()
    val occurredAt = Instant.parse("2026-05-18T13:15:30Z")
    insertSubscription(subscriptionId, organizationId, "PENDING")

    listener.onMessage(
      commandEnvelope(
        messageId = messageId,
        messageType = "SuspendSubscription",
        subscriptionId = subscriptionId,
        organizationId = organizationId,
        occurredAt = occurredAt,
      ),
    )

    assertThat(subscriptionStatus(subscriptionId)).isEqualTo("SUSPENDED")
    assertThat(inboxMessages("subscription.suspend-subscription", messageId)).isEqualTo(1)
    assertThat(historyActions(subscriptionId)).containsExactly("SUBSCRIPTION_SUSPENDED")

    val outbox = singleOutboxMessage(subscriptionId)
    assertThat(outbox["type"]).isEqualTo("SubscriptionSuspendedEvent")
    val payload = objectMapper.readTree(outbox["payload"].toString())
    assertThat(payload["subscriptionId"].asText()).isEqualTo(subscriptionId.toString())
    assertThat(payload["organizationId"].asText()).isEqualTo(organizationId)
    assertThat(payload["suspendedAt"].asText()).isEqualTo(occurredAt.toString())
  }

  @Test
  fun `duplicate message id with same consumer does not execute side effects twice`() {
    val subscriptionId = UUID.randomUUID()
    val organizationId = "org-listener-duplicate"
    val messageId = UUID.randomUUID()
    val record = commandEnvelope(
      messageId = messageId,
      messageType = "ActivateSubscription",
      subscriptionId = subscriptionId,
      organizationId = organizationId,
      occurredAt = Instant.parse("2026-05-18T14:15:30Z"),
    )
    insertSubscription(subscriptionId, organizationId, "PENDING")

    listener.onMessage(record)
    listener.onMessage(record)

    assertThat(subscriptionStatus(subscriptionId)).isEqualTo("ACTIVE")
    assertThat(inboxMessages("subscription.activate-subscription", messageId)).isEqualTo(1)
    assertThat(countRows("subscription_history")).isEqualTo(1)
    assertThat(countRows("outbox_messages")).isEqualTo(1)
  }

  private fun commandEnvelope(
    messageId: UUID,
    messageType: String,
    subscriptionId: UUID,
    organizationId: String,
    occurredAt: Instant,
  ): GenericRecord {
    val headers = mapOf(
      "messageId" to messageId.toString(),
      "messageType" to messageType,
      "aggregateId" to subscriptionId.toString(),
      "aggregateType" to "subscription",
      "occurredAt" to occurredAt.toString(),
      "schemaVersion" to 1,
      "correlationId" to UUID.randomUUID().toString(),
      "causationId" to UUID.randomUUID().toString(),
    )
    val payload = mapOf(
      "subscriptionId" to subscriptionId.toString(),
      "organizationId" to organizationId,
    )

    return GenericData.Record(OUTBOX_ENVELOPE_SCHEMA).apply {
      put("id", messageId.toString())
      put("type", messageType)
      put("aggregatetype", "subscription")
      put("aggregateid", subscriptionId.toString())
      put("payload", objectMapper.writeValueAsString(payload))
      put("headers", objectMapper.writeValueAsString(headers))
      put("timestamp", occurredAt.toString())
    }
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
      "pm_test_listener",
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

  private fun inboxMessages(
    consumer: String,
    messageId: UUID,
  ): Long =
    requireNotNull(
      jdbcTemplate.queryForObject(
        "select count(*) from inbox_messages where consumer = ? and message_id = ?",
        Long::class.java,
        consumer,
        messageId,
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
    private val OUTBOX_ENVELOPE_SCHEMA: Schema = SchemaBuilder.record("OutboxEnvelope")
      .fields()
      .requiredString("id")
      .requiredString("type")
      .requiredString("aggregatetype")
      .requiredString("aggregateid")
      .requiredString("payload")
      .requiredString("headers")
      .requiredString("timestamp")
      .endRecord()

    @Container
    @JvmStatic
    private val postgres = ListenerPostgreSQLContainer("postgres:16-alpine")
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
      registry.add("SPRING.KAFKA.PROPERTIES.SCHEMA.REGISTRY.URL") { "mock://subscription-listener-test" }
    }
  }
}

private class ListenerPostgreSQLContainer(imageName: String) : PostgreSQLContainer<ListenerPostgreSQLContainer>(imageName)
