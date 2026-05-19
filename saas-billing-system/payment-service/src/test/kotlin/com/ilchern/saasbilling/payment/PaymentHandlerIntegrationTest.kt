package com.ilchern.saasbilling.payment

import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.saasbilling.payment.application.command.HandleProviderWebhookCommand
import com.ilchern.saasbilling.payment.application.command.SubmitPaymentCommand
import com.ilchern.saasbilling.payment.application.handler.HandleProviderWebhookHandler
import com.ilchern.saasbilling.payment.application.handler.SubmitPaymentHandler
import com.ilchern.saasbilling.payment.domain.model.InvoiceId
import com.ilchern.saasbilling.payment.domain.model.Money
import com.ilchern.saasbilling.payment.domain.model.OrganizationId
import com.ilchern.saasbilling.payment.domain.model.PaymentMethodToken
import com.ilchern.saasbilling.payment.domain.model.ProviderPaymentReference
import com.ilchern.saasbilling.payment.domain.model.SubscriptionId
import org.assertj.core.api.Assertions.assertThat
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
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Testcontainers
@ActiveProfiles("local")
@Import(PaymentIntegrationTestConfig::class)
@TestPropertySource(properties = ["spring.kafka.listener.auto-startup=false"])
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PaymentHandlerIntegrationTest(
  @Autowired private val submitPaymentHandler: SubmitPaymentHandler,
  @Autowired private val handleProviderWebhookHandler: HandleProviderWebhookHandler,
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
  fun `submit payment stores payment method creates submitted attempt and calls provider`() {
    val invoiceId = UUID.randomUUID()
    val subscriptionId = UUID.randomUUID()
    val organizationId = "org-payment-submit"

    val attempt = submitPaymentHandler.handle(
      SubmitPaymentCommand(
        invoiceId = InvoiceId(invoiceId),
        subscriptionId = SubscriptionId(subscriptionId),
        organizationId = OrganizationId(organizationId),
        amount = Money(amountMinor = 2600L, currency = "USD"),
        paymentMethodToken = PaymentMethodToken("pm_submit"),
        messageId = UUID.randomUUID(),
        correlationId = UUID.randomUUID(),
        causationId = UUID.randomUUID(),
        occurredAt = Instant.parse("2026-05-19T10:15:30Z"),
      ),
    )

    assertThat(attempt.status().name).isEqualTo("SUBMITTED")
    assertThat(countRows("payment_methods")).isEqualTo(1)
    assertThat(countRows("payment_attempts")).isEqualTo(1)
    val row = paymentAttemptRow(attempt.id.value)
    assertThat(row["invoice_id"]).isEqualTo(invoiceId)
    assertThat(row["subscription_id"]).isEqualTo(subscriptionId)
    assertThat(row["organization_id"]).isEqualTo(organizationId)
    assertThat(row["attempt_number"]).isEqualTo(1)
    assertThat(row["status"]).isEqualTo("SUBMITTED")
    assertThat(row["provider_payment_id"]).isEqualTo("provider-$invoiceId:1")
    assertThat(row["provider_status"]).isEqualTo("accepted")

    assertThat(providerClient.submittedRequests()).hasSize(1)
    val request = providerClient.submittedRequests().single()
    assertThat(request.idempotencyKey).isEqualTo("$invoiceId:1")
    assertThat(request.paymentMethodToken.value).isEqualTo("pm_submit")
  }

  @Test
  fun `duplicate submit payment for submitted attempt does not call provider twice`() {
    val invoiceId = UUID.randomUUID()
    val subscriptionId = UUID.randomUUID()
    val organizationId = "org-payment-duplicate-submit"
    val command = SubmitPaymentCommand(
      invoiceId = InvoiceId(invoiceId),
      subscriptionId = SubscriptionId(subscriptionId),
      organizationId = OrganizationId(organizationId),
      amount = Money(amountMinor = 2600L, currency = "USD"),
      paymentMethodToken = PaymentMethodToken("pm_duplicate_submit"),
      messageId = UUID.randomUUID(),
      correlationId = null,
      causationId = null,
      occurredAt = Instant.parse("2026-05-19T10:15:30Z"),
    )

    val first = submitPaymentHandler.handle(command)
    val second = submitPaymentHandler.handle(command.copy(messageId = UUID.randomUUID()))

    assertThat(second.id).isEqualTo(first.id)
    assertThat(countRows("payment_attempts")).isEqualTo(1)
    assertThat(providerClient.submittedRequests()).hasSize(1)
  }

  @Test
  fun `existing created attempt is submitted using same provider idempotency key`() {
    val invoiceId = UUID.randomUUID()
    val subscriptionId = UUID.randomUUID()
    val attemptId = UUID.randomUUID()
    insertPaymentAttempt(
      attemptId = attemptId,
      invoiceId = invoiceId,
      subscriptionId = subscriptionId,
      organizationId = "org-payment-created",
      status = "CREATED",
      providerPaymentId = null,
      providerStatus = null,
    )

    val attempt = submitPaymentHandler.handle(
      SubmitPaymentCommand(
        invoiceId = InvoiceId(invoiceId),
        subscriptionId = SubscriptionId(subscriptionId),
        organizationId = OrganizationId("org-payment-created"),
        amount = Money(amountMinor = 2600L, currency = "USD"),
        paymentMethodToken = PaymentMethodToken("pm_created"),
        messageId = UUID.randomUUID(),
        correlationId = null,
        causationId = null,
        occurredAt = Instant.parse("2026-05-19T10:15:30Z"),
      ),
    )

    assertThat(attempt.id.value).isEqualTo(attemptId)
    assertThat(paymentAttemptRow(attemptId)["status"]).isEqualTo("SUBMITTED")
    assertThat(providerClient.submittedRequests()).hasSize(1)
    assertThat(providerClient.submittedRequests().single().idempotencyKey).isEqualTo("$invoiceId:1")
  }

  @Test
  fun `successful provider webhook marks attempt succeeded and writes outbox event`() {
    val invoiceId = UUID.randomUUID()
    val subscriptionId = UUID.randomUUID()
    val attemptId = UUID.randomUUID()
    val completedAt = Instant.parse("2026-05-19T11:15:30Z")
    insertPaymentAttempt(
      attemptId = attemptId,
      invoiceId = invoiceId,
      subscriptionId = subscriptionId,
      organizationId = "org-payment-webhook-success",
      status = "SUBMITTED",
      providerPaymentId = "provider-success",
      providerStatus = "accepted",
    )

    handleProviderWebhookHandler.handle(
      HandleProviderWebhookCommand(
        providerEventId = "evt-success-1",
        providerPaymentReference = ProviderPaymentReference("provider-success"),
        type = "payment_succeeded",
        status = "succeeded",
        invoiceId = InvoiceId(invoiceId),
        amount = Money(amountMinor = 2600L, currency = "USD"),
        occurredAt = completedAt,
        failureCode = null,
        failureMessage = null,
      ),
    )

    val row = paymentAttemptRow(attemptId)
    assertThat(row["status"]).isEqualTo("SUCCEEDED")
    assertThat(row["provider_status"]).isEqualTo("succeeded")
    assertThat(countRows("provider_webhook_events")).isEqualTo(1)

    val outbox = singleOutboxMessage(invoiceId)
    assertThat(outbox["aggregatetype"]).isEqualTo("payment")
    assertThat(outbox["aggregateid"]).isEqualTo(invoiceId.toString())
    assertThat(outbox["type"]).isEqualTo("PaymentSucceededEvent")
    val payload = objectMapper.readTree(outbox["payload"].toString())
    assertThat(payload["paymentAttemptId"].asText()).isEqualTo(attemptId.toString())
    assertThat(payload["invoiceId"].asText()).isEqualTo(invoiceId.toString())
    assertThat(payload["subscriptionId"].asText()).isEqualTo(subscriptionId.toString())
    assertThat(payload["providerPaymentId"].asText()).isEqualTo("provider-success")
    assertThat(payload["attemptNumber"].asInt()).isEqualTo(1)
    assertThat(payload["occurredAt"].asText()).isEqualTo(completedAt.toString())
  }

  @Test
  fun `failed provider webhook marks attempt failed and writes outbox event`() {
    val invoiceId = UUID.randomUUID()
    val attemptId = UUID.randomUUID()
    val completedAt = Instant.parse("2026-05-19T12:15:30Z")
    insertPaymentAttempt(
      attemptId = attemptId,
      invoiceId = invoiceId,
      subscriptionId = UUID.randomUUID(),
      organizationId = "org-payment-webhook-failed",
      status = "SUBMITTED",
      providerPaymentId = "provider-failed",
      providerStatus = "accepted",
    )

    handleProviderWebhookHandler.handle(
      HandleProviderWebhookCommand(
        providerEventId = "evt-failed-1",
        providerPaymentReference = ProviderPaymentReference("provider-failed"),
        type = "payment_failed",
        status = "failed",
        invoiceId = InvoiceId(invoiceId),
        amount = Money(amountMinor = 2600L, currency = "USD"),
        occurredAt = completedAt,
        failureCode = "card_declined",
        failureMessage = "Card was declined",
      ),
    )

    val row = paymentAttemptRow(attemptId)
    assertThat(row["status"]).isEqualTo("FAILED")
    assertThat(row["provider_status"]).isEqualTo("failed")
    assertThat(row["failure_code"]).isEqualTo("card_declined")
    assertThat(row["failure_message"]).isEqualTo("Card was declined")

    val payload = objectMapper.readTree(singleOutboxMessage(invoiceId)["payload"].toString())
    assertThat(payload["failureCode"].asText()).isEqualTo("card_declined")
    assertThat(payload["failureMessage"].asText()).isEqualTo("Card was declined")
  }

  @Test
  fun `duplicate provider event does not duplicate outbox event`() {
    val invoiceId = UUID.randomUUID()
    insertPaymentAttempt(
      attemptId = UUID.randomUUID(),
      invoiceId = invoiceId,
      subscriptionId = UUID.randomUUID(),
      organizationId = "org-payment-webhook-duplicate",
      status = "SUBMITTED",
      providerPaymentId = "provider-duplicate",
      providerStatus = "accepted",
    )
    val command = HandleProviderWebhookCommand(
      providerEventId = "evt-duplicate-1",
      providerPaymentReference = ProviderPaymentReference("provider-duplicate"),
      type = "payment_succeeded",
      status = "succeeded",
      invoiceId = InvoiceId(invoiceId),
      amount = Money(amountMinor = 2600L, currency = "USD"),
      occurredAt = Instant.parse("2026-05-19T13:15:30Z"),
      failureCode = null,
      failureMessage = null,
    )

    handleProviderWebhookHandler.handle(command)
    handleProviderWebhookHandler.handle(command)

    assertThat(countRows("provider_webhook_events")).isEqualTo(1)
    assertThat(countRows("outbox_messages")).isEqualTo(1)
  }

  private fun insertPaymentAttempt(
    attemptId: UUID,
    invoiceId: UUID,
    subscriptionId: UUID,
    organizationId: String,
    status: String,
    providerPaymentId: String?,
    providerStatus: String?,
  ) {
    jdbcTemplate.update(
      """
      insert into payment_attempts (
        id,
        invoice_id,
        subscription_id,
        organization_id,
        attempt_number,
        amount_minor,
        currency,
        payment_method_token,
        status,
        provider_payment_id,
        provider_status,
        created_at,
        submitted_at
      )
      values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """.trimIndent(),
      attemptId,
      invoiceId,
      subscriptionId,
      organizationId,
      1,
      2600L,
      "USD",
      "pm_seed",
      status,
      providerPaymentId,
      providerStatus,
      Timestamp.from(Instant.parse("2026-05-19T09:00:00Z")),
      if (providerPaymentId == null) null else Timestamp.from(Instant.parse("2026-05-19T09:01:00Z")),
    )
  }

  private fun paymentAttemptRow(attemptId: UUID): Map<String, Any> =
    jdbcTemplate.queryForMap("select * from payment_attempts where id = ?", attemptId)

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
    private val postgres = PaymentHandlerPostgreSQLContainer("postgres:16-alpine")
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
      registry.add("SPRING.KAFKA.PROPERTIES.SCHEMA.REGISTRY.URL") { "mock://payment-handler-test" }
      registry.add("PAYMENT.PROVIDER.BASE_URL") { "http://127.0.0.1:65535" }
    }
  }
}

private class PaymentHandlerPostgreSQLContainer(
  imageName: String,
) : PostgreSQLContainer<PaymentHandlerPostgreSQLContainer>(imageName)
