package com.ilchern.saasbilling.billing

import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.saasbilling.billing.infrastructure.messaging.kafka.BillingCommandListener
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
class BillingKafkaListenerIntegrationTest(
  @Autowired private val listener: BillingCommandListener,
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
        invoice_lines,
        invoices
      restart identity cascade
      """.trimIndent(),
    )
  }

  @Test
  fun `CreateInitialInvoice envelope creates invoice through inbox and handler`() {
    val subscriptionId = UUID.randomUUID()
    val organizationId = "org-listener-create-invoice"
    val messageId = UUID.randomUUID()
    val occurredAt = Instant.parse("2026-05-19T13:15:30Z")

    listener.onMessage(
      commandEnvelope(
        messageId = messageId,
        messageType = "CreateInitialInvoice",
        aggregateId = subscriptionId,
        occurredAt = occurredAt,
        payload = mapOf(
          "subscriptionId" to subscriptionId.toString(),
          "organizationId" to organizationId,
          "subscriptionPlan" to "PRO",
          "billingPeriod" to "MONTHLY",
          "seats" to 3,
          "paymentMethodToken" to "pm_listener_create",
        ),
      ),
    )

    assertThat(countRows("invoices")).isEqualTo(1)
    assertThat(countRows("invoice_lines")).isEqualTo(1)
    assertThat(inboxMessages("billing.create-initial-invoice", messageId)).isEqualTo(1)

    val invoiceId = onlyInvoiceId()
    val invoice = invoiceRow(invoiceId)
    assertThat(invoice["subscription_id"]).isEqualTo(subscriptionId)
    assertThat(invoice["status"]).isEqualTo("OPEN")
    assertThat(invoice["amount_minor"]).isEqualTo(2600L)

    val outbox = singleOutboxMessage(invoiceId)
    assertThat(outbox["type"]).isEqualTo("InvoiceCreatedEvent")
    val payload = objectMapper.readTree(outbox["payload"].toString())
    assertThat(payload["subscriptionId"].asText()).isEqualTo(subscriptionId.toString())
    assertThat(payload["organizationId"].asText()).isEqualTo(organizationId)
    assertThat(payload["amountMinor"].asLong()).isEqualTo(2600L)
  }

  @Test
  fun `MarkInvoicePaid envelope marks invoice paid through inbox and handler`() {
    val invoiceId = UUID.randomUUID()
    val messageId = UUID.randomUUID()
    val occurredAt = Instant.parse("2026-05-19T14:15:30Z")
    insertInvoice(invoiceId, UUID.randomUUID(), "org-listener-paid", status = "OPEN", amountMinor = 2600L)

    listener.onMessage(
      commandEnvelope(
        messageId = messageId,
        messageType = "MarkInvoicePaid",
        aggregateId = invoiceId,
        occurredAt = occurredAt,
        payload = mapOf(
          "invoiceId" to invoiceId.toString(),
          "amountMinor" to 2600L,
          "currency" to "USD",
        ),
      ),
    )

    assertThat(invoiceStatus(invoiceId)).isEqualTo("PAID")
    assertThat(inboxMessages("billing.mark-invoice-paid", messageId)).isEqualTo(1)
    val outbox = singleOutboxMessage(invoiceId)
    assertThat(outbox["type"]).isEqualTo("InvoicePaidEvent")
  }

  @Test
  fun `MarkInvoicePaymentPending envelope marks invoice payment pending through inbox and handler`() {
    val invoiceId = UUID.randomUUID()
    val messageId = UUID.randomUUID()
    val occurredAt = Instant.parse("2026-05-19T15:15:30Z")
    insertInvoice(invoiceId, UUID.randomUUID(), "org-listener-pending", status = "OPEN", amountMinor = 2600L)

    listener.onMessage(
      commandEnvelope(
        messageId = messageId,
        messageType = "MarkInvoicePaymentPending",
        aggregateId = invoiceId,
        occurredAt = occurredAt,
        payload = mapOf(
          "invoiceId" to invoiceId.toString(),
          "amountMinor" to 2600L,
          "currency" to "USD",
          "failureCode" to "card_declined",
          "failureMessage" to "Card was declined",
        ),
      ),
    )

    assertThat(invoiceStatus(invoiceId)).isEqualTo("PAYMENT_PENDING")
    assertThat(inboxMessages("billing.mark-invoice-payment-pending", messageId)).isEqualTo(1)
    val outbox = singleOutboxMessage(invoiceId)
    assertThat(outbox["type"]).isEqualTo("InvoicePaymentPendingEvent")
    val payload = objectMapper.readTree(outbox["payload"].toString())
    assertThat(payload["failureCode"].asText()).isEqualTo("card_declined")
    assertThat(payload["failureMessage"].asText()).isEqualTo("Card was declined")
  }

  @Test
  fun `duplicate message id with same consumer does not execute side effects twice`() {
    val invoiceId = UUID.randomUUID()
    val messageId = UUID.randomUUID()
    val record = commandEnvelope(
      messageId = messageId,
      messageType = "MarkInvoicePaid",
      aggregateId = invoiceId,
      occurredAt = Instant.parse("2026-05-19T16:15:30Z"),
      payload = mapOf(
        "invoiceId" to invoiceId.toString(),
        "amountMinor" to 2600L,
        "currency" to "USD",
      ),
    )
    insertInvoice(invoiceId, UUID.randomUUID(), "org-listener-duplicate", status = "OPEN", amountMinor = 2600L)

    listener.onMessage(record)
    listener.onMessage(record)

    assertThat(invoiceStatus(invoiceId)).isEqualTo("PAID")
    assertThat(inboxMessages("billing.mark-invoice-paid", messageId)).isEqualTo(1)
    assertThat(countRows("outbox_messages")).isEqualTo(1)
  }

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
      "aggregateType" to "invoice",
      "occurredAt" to occurredAt.toString(),
      "schemaVersion" to 1,
      "correlationId" to UUID.randomUUID().toString(),
      "causationId" to UUID.randomUUID().toString(),
    )

    return GenericData.Record(OUTBOX_ENVELOPE_SCHEMA).apply {
      put("id", messageId.toString())
      put("type", messageType)
      put("aggregatetype", "invoice")
      put("aggregateid", aggregateId.toString())
      put("payload", objectMapper.writeValueAsString(payload))
      put("headers", objectMapper.writeValueAsString(headers))
      put("timestamp", occurredAt.toString())
    }
  }

  private fun insertInvoice(
    invoiceId: UUID,
    subscriptionId: UUID,
    organizationId: String,
    status: String,
    amountMinor: Long,
  ) {
    jdbcTemplate.update(
      """
      insert into invoices (
        id,
        subscription_id,
        organization_id,
        invoice_type,
        status,
        subscription_plan,
        billing_period,
        seats,
        period_start,
        period_end,
        amount_minor,
        currency,
        payment_method_token,
        created_at
      )
      values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """.trimIndent(),
      invoiceId,
      subscriptionId,
      organizationId,
      "INITIAL",
      status,
      "PRO",
      "MONTHLY",
      3,
      Timestamp.from(Instant.parse("2026-05-19T10:00:00Z")),
      Timestamp.from(Instant.parse("2026-06-19T10:00:00Z")),
      amountMinor,
      "USD",
      "pm_billing_listener",
      Timestamp.from(Instant.parse("2026-05-19T09:00:00Z")),
    )
    jdbcTemplate.update(
      """
      insert into invoice_lines (id, invoice_id, description, quantity, amount_minor, currency)
      values (?, ?, ?, ?, ?, ?)
      """.trimIndent(),
      UUID.randomUUID(),
      invoiceId,
      "Initial subscription invoice",
      1,
      amountMinor,
      "USD",
    )
  }

  private fun onlyInvoiceId(): UUID =
    requireNotNull(jdbcTemplate.queryForObject("select id from invoices", UUID::class.java))

  private fun invoiceRow(invoiceId: UUID): Map<String, Any> =
    jdbcTemplate.queryForMap("select * from invoices where id = ?", invoiceId)

  private fun invoiceStatus(invoiceId: UUID): String =
    requireNotNull(
      jdbcTemplate.queryForObject(
        "select status from invoices where id = ?",
        String::class.java,
        invoiceId,
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

  private fun singleOutboxMessage(invoiceId: UUID): Map<String, Any> =
    jdbcTemplate.queryForMap(
      """
      select aggregatetype, aggregateid, type, payload
      from outbox_messages
      where aggregateid = ?
      """.trimIndent(),
      invoiceId.toString(),
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
    private val postgres = BillingListenerPostgreSQLContainer("postgres:16-alpine")
      .withDatabaseName("billing_service_db")
      .withUsername("billing_service_user")
      .withPassword("billing_service_password")

    @JvmStatic
    @DynamicPropertySource
    fun registerProperties(registry: DynamicPropertyRegistry) {
      registry.add("SPRING.DATASOURCE.URL", postgres::getJdbcUrl)
      registry.add("SPRING.DATASOURCE.USERNAME", postgres::getUsername)
      registry.add("SPRING.DATASOURCE.PASSWORD", postgres::getPassword)
      registry.add("SPRING.KAFKA.BOOTSTRAP_SERVERS") { "127.0.0.1:65535" }
      registry.add("SPRING.KAFKA.PROPERTIES.SCHEMA.REGISTRY.URL") { "mock://billing-listener-test" }
    }
  }
}

private class BillingListenerPostgreSQLContainer(
  imageName: String,
) : PostgreSQLContainer<BillingListenerPostgreSQLContainer>(imageName)
