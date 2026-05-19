package com.ilchern.saasbilling.payment

import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.saasbilling.payment.infrastructure.web.api.PaymentProviderWebhookRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentWebhookRestIntegrationTest(
  @Autowired private val restTemplate: TestRestTemplate,
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
  fun `successful provider webhook marks attempt succeeded and writes outbox event`() {
    val invoiceId = UUID.randomUUID()
    val subscriptionId = UUID.randomUUID()
    val attemptId = UUID.randomUUID()
    val occurredAt = Instant.parse("2026-05-19T14:15:30Z")
    insertSubmittedAttempt(
      attemptId = attemptId,
      invoiceId = invoiceId,
      subscriptionId = subscriptionId,
      organizationId = "org-webhook-success",
      providerPaymentId = "provider-webhook-success",
    )

    val response = restTemplate.postForEntity(
      "/webhooks/payment-provider",
      webhookRequest(
        providerEventId = "evt-webhook-success-1",
        providerPaymentId = "provider-webhook-success",
        status = "succeeded",
        invoiceId = invoiceId,
        occurredAt = occurredAt,
      ),
      Void::class.java,
    )

    assertThat(response.statusCode).isEqualTo(HttpStatus.ACCEPTED)
    assertThat(paymentAttemptRow(attemptId)["status"]).isEqualTo("SUCCEEDED")
    assertThat(countRows("provider_webhook_events")).isEqualTo(1)
    val outbox = singleOutboxMessage(invoiceId)
    assertThat(outbox["type"]).isEqualTo("PaymentSucceededEvent")
    val payload = objectMapper.readTree(outbox["payload"].toString())
    assertThat(payload["paymentAttemptId"].asText()).isEqualTo(attemptId.toString())
    assertThat(payload["invoiceId"].asText()).isEqualTo(invoiceId.toString())
    assertThat(payload["subscriptionId"].asText()).isEqualTo(subscriptionId.toString())
    assertThat(payload["providerPaymentId"].asText()).isEqualTo("provider-webhook-success")
  }

  @Test
  fun `failed provider webhook marks attempt failed and writes outbox event`() {
    val invoiceId = UUID.randomUUID()
    val attemptId = UUID.randomUUID()
    insertSubmittedAttempt(
      attemptId = attemptId,
      invoiceId = invoiceId,
      subscriptionId = UUID.randomUUID(),
      organizationId = "org-webhook-failed",
      providerPaymentId = "provider-webhook-failed",
    )

    val response = restTemplate.postForEntity(
      "/webhooks/payment-provider",
      webhookRequest(
        providerEventId = "evt-webhook-failed-1",
        providerPaymentId = "provider-webhook-failed",
        status = "failed",
        invoiceId = invoiceId,
        occurredAt = Instant.parse("2026-05-19T15:15:30Z"),
        failureCode = "card_declined",
        failureMessage = "Card was declined",
      ),
      Void::class.java,
    )

    assertThat(response.statusCode).isEqualTo(HttpStatus.ACCEPTED)
    val row = paymentAttemptRow(attemptId)
    assertThat(row["status"]).isEqualTo("FAILED")
    assertThat(row["failure_code"]).isEqualTo("card_declined")
    assertThat(row["failure_message"]).isEqualTo("Card was declined")
    val payload = objectMapper.readTree(singleOutboxMessage(invoiceId)["payload"].toString())
    assertThat(payload["failureCode"].asText()).isEqualTo("card_declined")
    assertThat(payload["failureMessage"].asText()).isEqualTo("Card was declined")
  }

  @Test
  fun `duplicate provider webhook does not duplicate outcome event`() {
    val invoiceId = UUID.randomUUID()
    insertSubmittedAttempt(
      attemptId = UUID.randomUUID(),
      invoiceId = invoiceId,
      subscriptionId = UUID.randomUUID(),
      organizationId = "org-webhook-duplicate",
      providerPaymentId = "provider-webhook-duplicate",
    )
    val request = webhookRequest(
      providerEventId = "evt-webhook-duplicate-1",
      providerPaymentId = "provider-webhook-duplicate",
      status = "succeeded",
      invoiceId = invoiceId,
      occurredAt = Instant.parse("2026-05-19T16:15:30Z"),
    )

    val first = restTemplate.postForEntity("/webhooks/payment-provider", request, Void::class.java)
    val second = restTemplate.postForEntity("/webhooks/payment-provider", request, Void::class.java)

    assertThat(first.statusCode).isEqualTo(HttpStatus.ACCEPTED)
    assertThat(second.statusCode).isEqualTo(HttpStatus.ACCEPTED)
    assertThat(countRows("provider_webhook_events")).isEqualTo(1)
    assertThat(countRows("outbox_messages")).isEqualTo(1)
  }

  @Test
  fun `provider webhook with mismatched amount fails without changing attempt or writing outbox`() {
    val invoiceId = UUID.randomUUID()
    val attemptId = UUID.randomUUID()
    insertSubmittedAttempt(
      attemptId = attemptId,
      invoiceId = invoiceId,
      subscriptionId = UUID.randomUUID(),
      organizationId = "org-webhook-mismatch",
      providerPaymentId = "provider-webhook-mismatch",
    )

    val response = restTemplate.postForEntity(
      "/webhooks/payment-provider",
      webhookRequest(
        providerEventId = "evt-webhook-mismatch-1",
        providerPaymentId = "provider-webhook-mismatch",
        status = "succeeded",
        invoiceId = invoiceId,
        amountMinor = 9999L,
        occurredAt = Instant.parse("2026-05-19T17:15:30Z"),
      ),
      String::class.java,
    )

    assertThat(response.statusCode.is5xxServerError).isTrue()
    assertThat(paymentAttemptRow(attemptId)["status"]).isEqualTo("SUBMITTED")
    assertThat(countRows("outbox_messages")).isEqualTo(0)
  }

  private fun webhookRequest(
    providerEventId: String,
    providerPaymentId: String,
    status: String,
    invoiceId: UUID,
    occurredAt: Instant,
    amountMinor: Long = 2600L,
    failureCode: String? = null,
    failureMessage: String? = null,
  ): PaymentProviderWebhookRequest =
    PaymentProviderWebhookRequest(
      providerEventId = providerEventId,
      providerPaymentId = providerPaymentId,
      type = if (status == "succeeded") "payment_succeeded" else "payment_failed",
      status = status,
      invoiceId = invoiceId.toString(),
      amountMinor = amountMinor,
      currency = "USD",
      occurredAt = occurredAt,
      failureCode = failureCode,
      failureMessage = failureMessage,
    )

  private fun insertSubmittedAttempt(
    attemptId: UUID,
    invoiceId: UUID,
    subscriptionId: UUID,
    organizationId: String,
    providerPaymentId: String,
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
      "pm_webhook",
      "SUBMITTED",
      providerPaymentId,
      "accepted",
      Timestamp.from(Instant.parse("2026-05-19T09:00:00Z")),
      Timestamp.from(Instant.parse("2026-05-19T09:01:00Z")),
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
    private val postgres = PaymentWebhookPostgreSQLContainer("postgres:16-alpine")
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
      registry.add("SPRING.KAFKA.PROPERTIES.SCHEMA.REGISTRY.URL") { "mock://payment-webhook-test" }
      registry.add("PAYMENT.PROVIDER.BASE_URL") { "http://127.0.0.1:65535" }
    }
  }
}

private class PaymentWebhookPostgreSQLContainer(
  imageName: String,
) : PostgreSQLContainer<PaymentWebhookPostgreSQLContainer>(imageName)
