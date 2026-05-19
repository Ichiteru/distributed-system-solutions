package com.ilchern.saasbilling.payment

import com.ilchern.saasbilling.payment.infrastructure.messaging.kafka.PaymentCommandListener
import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

@Testcontainers
@ActiveProfiles("local")
@Import(PaymentIntegrationTestConfig::class)
@TestPropertySource(properties = ["spring.kafka.listener.auto-startup=false"])
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PaymentKafkaListenerIntegrationTest(
  @Autowired private val listener: PaymentCommandListener,
  @Autowired private val providerClient: RecordingPaymentProviderClient,
  @Autowired private val jdbcTemplate: JdbcTemplate,
  @Autowired private val objectMapper: ObjectMapper,
) {

  @BeforeEach
  fun cleanDatabase() {
    providerClient.reset()
    jdbcTemplate.execute(
      """
      truncate table
        outbox_messages,
        inbox_messages,
        provider_webhook_events,
        payment_attempts,
        payment_methods
      restart identity cascade
      """.trimIndent(),
    )
  }

  @Test
  fun `SubmitPayment envelope creates submitted attempt through inbox and handler`() {
    val invoiceId = UUID.randomUUID()
    val subscriptionId = UUID.randomUUID()
    val organizationId = "org-listener-payment-submit"
    val messageId = UUID.randomUUID()

    listener.onMessage(
      commandEnvelope(
        messageId = messageId,
        messageType = "SubmitPayment",
        aggregateId = invoiceId,
        occurredAt = Instant.parse("2026-05-19T18:15:30Z"),
        payload = submitPaymentPayload(
          invoiceId = invoiceId,
          subscriptionId = subscriptionId,
          organizationId = organizationId,
        ),
      ),
    )

    assertThat(countRows("payment_attempts")).isEqualTo(1)
    assertThat(countRows("payment_methods")).isEqualTo(1)
    assertThat(inboxMessages("payment.submit-payment", messageId)).isEqualTo(1)

    val row = onlyPaymentAttemptRow()
    assertThat(row["invoice_id"]).isEqualTo(invoiceId)
    assertThat(row["subscription_id"]).isEqualTo(subscriptionId)
    assertThat(row["organization_id"]).isEqualTo(organizationId)
    assertThat(row["status"]).isEqualTo("SUBMITTED")
    assertThat(row["provider_payment_id"]).isEqualTo("provider-$invoiceId:1")
    assertThat(providerClient.submittedRequests()).hasSize(1)
    assertThat(providerClient.submittedRequests().single().idempotencyKey).isEqualTo("$invoiceId:1")
  }

  @Test
  fun `duplicate message id with same consumer does not execute side effects twice`() {
    val invoiceId = UUID.randomUUID()
    val record = commandEnvelope(
      messageId = UUID.randomUUID(),
      messageType = "SubmitPayment",
      aggregateId = invoiceId,
      occurredAt = Instant.parse("2026-05-19T19:15:30Z"),
      payload = submitPaymentPayload(
        invoiceId = invoiceId,
        subscriptionId = UUID.randomUUID(),
        organizationId = "org-listener-payment-duplicate",
      ),
    )

    listener.onMessage(record)
    listener.onMessage(record)

    assertThat(countRows("payment_attempts")).isEqualTo(1)
    assertThat(countRows("payment_methods")).isEqualTo(1)
    assertThat(countRows("inbox_messages")).isEqualTo(1)
    assertThat(providerClient.submittedRequests()).hasSize(1)
  }

  @Test
  fun `unsupported command type fails clearly`() {
    val invoiceId = UUID.randomUUID()

    assertThatThrownBy {
      listener.onMessage(
        commandEnvelope(
          messageId = UUID.randomUUID(),
          messageType = "UnsupportedPaymentCommand",
          aggregateId = invoiceId,
          occurredAt = Instant.parse("2026-05-19T20:15:30Z"),
          payload = submitPaymentPayload(
            invoiceId = invoiceId,
            subscriptionId = UUID.randomUUID(),
            organizationId = "org-listener-payment-unsupported",
          ),
        ),
      )
    }.hasMessageContaining("Unsupported payment command type: UnsupportedPaymentCommand")

    assertThat(countRows("inbox_messages")).isEqualTo(0)
    assertThat(countRows("payment_attempts")).isEqualTo(0)
  }

  private fun submitPaymentPayload(
    invoiceId: UUID,
    subscriptionId: UUID,
    organizationId: String,
  ): Map<String, Any> =
    mapOf(
      "invoiceId" to invoiceId.toString(),
      "subscriptionId" to subscriptionId.toString(),
      "organizationId" to organizationId,
      "amountMinor" to 2600L,
      "currency" to "USD",
      "paymentMethodToken" to "pm_listener",
    )

  private fun commandEnvelope(
    messageId: UUID,
    messageType: String,
    aggregateId: UUID,
    occurredAt: Instant,
    payload: Map<String, Any>,
  ): GenericRecord {
    val headers = mapOf(
      "messageId" to messageId.toString(),
      "messageType" to messageType,
      "aggregateId" to aggregateId.toString(),
      "aggregateType" to "payment",
      "occurredAt" to occurredAt.toString(),
      "schemaVersion" to 1,
      "correlationId" to UUID.randomUUID().toString(),
      "causationId" to UUID.randomUUID().toString(),
    )

    return GenericData.Record(OUTBOX_ENVELOPE_SCHEMA).apply {
      put("id", messageId.toString())
      put("type", messageType)
      put("aggregatetype", "payment")
      put("aggregateid", aggregateId.toString())
      put("payload", objectMapper.writeValueAsString(payload))
      put("headers", objectMapper.writeValueAsString(headers))
      put("timestamp", occurredAt.toString())
    }
  }

  private fun onlyPaymentAttemptRow(): Map<String, Any> =
    jdbcTemplate.queryForMap("select * from payment_attempts")

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
    private val postgres = PaymentListenerPostgreSQLContainer("postgres:16-alpine")
      .withDatabaseName("payment_service_db")
      .withUsername("payment_service_user")
      .withPassword("payment_service_password")

    @JvmStatic
    @DynamicPropertySource
    fun registerProperties(registry: DynamicPropertyRegistry) {
      registry.add("SPRING.DATASOURCE.URL", postgres::getJdbcUrl)
      registry.add("SPRING.DATASOURCE.USERNAME", postgres::getUsername)
      registry.add("SPRING.DATASOURCE.PASSWORD", postgres::getPassword)
      registry.add("SPRING.KAFKA.BOOTSTRAP_SERVERS") { "127.0.0.1:65535" }
      registry.add("SPRING.KAFKA.PROPERTIES.SCHEMA.REGISTRY.URL") { "mock://payment-listener-test" }
      registry.add("PAYMENT.PROVIDER.BASE_URL") { "http://127.0.0.1:65535" }
    }
  }
}

private class PaymentListenerPostgreSQLContainer(
  imageName: String,
) : PostgreSQLContainer<PaymentListenerPostgreSQLContainer>(imageName)
