package com.ilchern.saasbilling.billing

import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.saasbilling.billing.application.command.CreateInitialInvoiceCommand
import com.ilchern.saasbilling.billing.application.command.MarkInvoicePaidCommand
import com.ilchern.saasbilling.billing.application.command.MarkInvoicePaymentPendingCommand
import com.ilchern.saasbilling.billing.application.handler.CreateInitialInvoiceHandler
import com.ilchern.saasbilling.billing.application.handler.MarkInvoicePaidHandler
import com.ilchern.saasbilling.billing.application.handler.MarkInvoicePaymentPendingHandler
import com.ilchern.saasbilling.billing.domain.model.BillingPeriod
import com.ilchern.saasbilling.billing.domain.model.InvoiceId
import com.ilchern.saasbilling.billing.domain.model.Money
import com.ilchern.saasbilling.billing.domain.model.OrganizationId
import com.ilchern.saasbilling.billing.domain.model.PaymentMethodToken
import com.ilchern.saasbilling.billing.domain.model.SubscriptionId
import com.ilchern.saasbilling.billing.domain.model.SubscriptionPlan
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
class BillingHandlerIntegrationTest(
  @Autowired private val createInitialInvoiceHandler: CreateInitialInvoiceHandler,
  @Autowired private val markInvoicePaidHandler: MarkInvoicePaidHandler,
  @Autowired private val markInvoicePaymentPendingHandler: MarkInvoicePaymentPendingHandler,
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
  fun `create initial invoice persists invoice lines and outbox event`() {
    val subscriptionId = UUID.randomUUID()
    val organizationId = "org-billing-create"
    val occurredAt = Instant.parse("2026-05-19T10:15:30Z")

    val invoice = createInitialInvoiceHandler.handle(
      CreateInitialInvoiceCommand(
        subscriptionId = SubscriptionId(subscriptionId),
        organizationId = OrganizationId(organizationId),
        subscriptionPlan = SubscriptionPlan.PRO,
        billingPeriod = BillingPeriod.MONTHLY,
        seats = 3,
        paymentMethodToken = PaymentMethodToken("pm_billing_create"),
        messageId = UUID.randomUUID(),
        correlationId = UUID.randomUUID(),
        causationId = UUID.randomUUID(),
        occurredAt = occurredAt,
      ),
    )

    val invoiceRow = invoiceRow(invoice.id.value)
    assertThat(invoiceRow["subscription_id"]).isEqualTo(subscriptionId)
    assertThat(invoiceRow["organization_id"]).isEqualTo(organizationId)
    assertThat(invoiceRow["invoice_type"]).isEqualTo("INITIAL")
    assertThat(invoiceRow["status"]).isEqualTo("OPEN")
    assertThat(invoiceRow["subscription_plan"]).isEqualTo("PRO")
    assertThat(invoiceRow["billing_period"]).isEqualTo("MONTHLY")
    assertThat(invoiceRow["seats"]).isEqualTo(3)
    assertThat(invoiceRow["amount_minor"]).isEqualTo(2600L)
    assertThat(invoiceRow["currency"]).isEqualTo("USD")
    assertThat(invoiceRow["payment_method_token"]).isEqualTo("pm_billing_create")
    assertThat(countRows("invoice_lines")).isEqualTo(1)

    val outbox = singleOutboxMessage(invoice.id.value)
    assertThat(outbox["aggregatetype"]).isEqualTo("invoice")
    assertThat(outbox["aggregateid"]).isEqualTo(invoice.id.value.toString())
    assertThat(outbox["type"]).isEqualTo("InvoiceCreatedEvent")

    val payload = objectMapper.readTree(outbox["payload"].toString())
    assertThat(payload["invoiceId"].asText()).isEqualTo(invoice.id.value.toString())
    assertThat(payload["subscriptionId"].asText()).isEqualTo(subscriptionId.toString())
    assertThat(payload["organizationId"].asText()).isEqualTo(organizationId)
    assertThat(payload["status"].asText()).isEqualTo("OPEN")
    assertThat(payload["plan"].asText()).isEqualTo("PRO")
    assertThat(payload["billingPeriod"].asText()).isEqualTo("MONTHLY")
    assertThat(payload["seats"].asInt()).isEqualTo(3)
    assertThat(payload["amountMinor"].asLong()).isEqualTo(2600L)
    assertThat(payload["currency"].asText()).isEqualTo("USD")
    assertThat(payload["paymentMethodToken"].asText()).isEqualTo("pm_billing_create")
  }

  @Test
  fun `create initial invoice is idempotent for same subscription period`() {
    val subscriptionId = UUID.randomUUID()
    val occurredAt = Instant.parse("2026-05-19T10:15:30Z")
    val command = CreateInitialInvoiceCommand(
      subscriptionId = SubscriptionId(subscriptionId),
      organizationId = OrganizationId("org-billing-duplicate"),
      subscriptionPlan = SubscriptionPlan.BASIC,
      billingPeriod = BillingPeriod.MONTHLY,
      seats = 2,
      paymentMethodToken = PaymentMethodToken("pm_billing_duplicate"),
      messageId = UUID.randomUUID(),
      correlationId = null,
      causationId = null,
      occurredAt = occurredAt,
    )

    val first = createInitialInvoiceHandler.handle(command)
    val second = createInitialInvoiceHandler.handle(command.copy(messageId = UUID.randomUUID()))

    assertThat(second.id).isEqualTo(first.id)
    assertThat(countRows("invoices")).isEqualTo(1)
    assertThat(countRows("invoice_lines")).isEqualTo(1)
    assertThat(countRows("outbox_messages")).isEqualTo(1)
  }

  @Test
  fun `mark invoice paid updates status and writes outbox event`() {
    val invoiceId = UUID.randomUUID()
    val subscriptionId = UUID.randomUUID()
    val organizationId = "org-billing-paid"
    val paidAt = Instant.parse("2026-05-19T11:15:30Z")
    insertInvoice(invoiceId, subscriptionId, organizationId, status = "OPEN", amountMinor = 2600L)

    markInvoicePaidHandler.handle(
      MarkInvoicePaidCommand(
        invoiceId = InvoiceId(invoiceId),
        amount = Money(amountMinor = 2600L, currency = "USD"),
        messageId = UUID.randomUUID(),
        correlationId = UUID.randomUUID(),
        causationId = UUID.randomUUID(),
        occurredAt = paidAt,
      ),
    )

    assertThat(invoiceStatus(invoiceId)).isEqualTo("PAID")
    val outbox = singleOutboxMessage(invoiceId)
    assertThat(outbox["type"]).isEqualTo("InvoicePaidEvent")

    val payload = objectMapper.readTree(outbox["payload"].toString())
    assertThat(payload["invoiceId"].asText()).isEqualTo(invoiceId.toString())
    assertThat(payload["subscriptionId"].asText()).isEqualTo(subscriptionId.toString())
    assertThat(payload["organizationId"].asText()).isEqualTo(organizationId)
    assertThat(payload["amountMinor"].asLong()).isEqualTo(2600L)
    assertThat(payload["currency"].asText()).isEqualTo("USD")
    assertThat(payload["paidAt"].asText()).isEqualTo(paidAt.toString())
  }

  @Test
  fun `mark invoice payment pending updates status and writes outbox event`() {
    val invoiceId = UUID.randomUUID()
    val subscriptionId = UUID.randomUUID()
    val organizationId = "org-billing-pending"
    val pendingAt = Instant.parse("2026-05-19T12:15:30Z")
    insertInvoice(invoiceId, subscriptionId, organizationId, status = "OPEN", amountMinor = 2600L)

    markInvoicePaymentPendingHandler.handle(
      MarkInvoicePaymentPendingCommand(
        invoiceId = InvoiceId(invoiceId),
        amount = Money(amountMinor = 2600L, currency = "USD"),
        failureCode = "card_declined",
        failureMessage = "Card was declined",
        messageId = UUID.randomUUID(),
        correlationId = UUID.randomUUID(),
        causationId = UUID.randomUUID(),
        occurredAt = pendingAt,
      ),
    )

    assertThat(invoiceStatus(invoiceId)).isEqualTo("PAYMENT_PENDING")
    val outbox = singleOutboxMessage(invoiceId)
    assertThat(outbox["type"]).isEqualTo("InvoicePaymentPendingEvent")

    val payload = objectMapper.readTree(outbox["payload"].toString())
    assertThat(payload["invoiceId"].asText()).isEqualTo(invoiceId.toString())
    assertThat(payload["subscriptionId"].asText()).isEqualTo(subscriptionId.toString())
    assertThat(payload["organizationId"].asText()).isEqualTo(organizationId)
    assertThat(payload["amountMinor"].asLong()).isEqualTo(2600L)
    assertThat(payload["currency"].asText()).isEqualTo("USD")
    assertThat(payload["paymentPendingAt"].asText()).isEqualTo(pendingAt.toString())
    assertThat(payload["failureCode"].asText()).isEqualTo("card_declined")
    assertThat(payload["failureMessage"].asText()).isEqualTo("Card was declined")
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
      "pm_billing_seed",
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
    @Container
    @JvmStatic
    private val postgres = BillingHandlerPostgreSQLContainer("postgres:16-alpine")
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
      registry.add("SPRING.KAFKA.PROPERTIES.SCHEMA.REGISTRY.URL") { "mock://billing-handler-test" }
    }
  }
}

private class BillingHandlerPostgreSQLContainer(
  imageName: String,
) : PostgreSQLContainer<BillingHandlerPostgreSQLContainer>(imageName)
