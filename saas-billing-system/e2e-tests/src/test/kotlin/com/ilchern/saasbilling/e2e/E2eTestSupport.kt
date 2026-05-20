package com.ilchern.saasbilling.e2e

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.sql.DriverManager
import java.time.Duration
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await

object E2eTestSupport {

  private val STARTUP_TIMEOUT: Duration = Duration.ofMinutes(5)
  private val FLOW_TIMEOUT: Duration = Duration.ofMinutes(3)
  private val POLL_INTERVAL: Duration = Duration.ofSeconds(2)

  private const val subscriptionServiceUrl = "http://localhost:8082"
  private const val billingServiceUrl = "http://localhost:8084"
  private const val orchestratorServiceUrl = "http://localhost:8085"
  private const val paymentServiceUrl = "http://localhost:8086"
  private const val kafkaConnectUrl = "http://localhost:8083"

  private val httpClient: HttpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(2))
    .build()
  private val objectMapper = jacksonObjectMapper()

  fun waitForEnvironment() {
    waitForServiceHealth("subscription-service", subscriptionServiceUrl)
    waitForServiceHealth("billing-service", billingServiceUrl)
    waitForServiceHealth("billing-orchestrator", orchestratorServiceUrl)
    waitForServiceHealth("payment-service", paymentServiceUrl)
    waitForKafkaConnect()
  }

  fun createSubscription(paymentMethodToken: String): UUID {
    val organizationId = "org-e2e-${UUID.randomUUID()}"
    val idempotencyKey = "subscription-${UUID.randomUUID()}"

    val response = httpClient.send(
      HttpRequest.newBuilder(URI.create("$subscriptionServiceUrl/api/v1/subscriptions"))
        .header("Content-Type", "application/json")
        .header("X-Organization-Id", organizationId)
        .header("Idempotency-Key", idempotencyKey)
        .POST(
          HttpRequest.BodyPublishers.ofString(
            objectMapper.writeValueAsString(
              mapOf(
                "plan" to "PRO",
                "billingPeriod" to "MONTHLY",
                "seats" to 1,
                "paymentMethodToken" to paymentMethodToken,
              ),
            ),
          ),
        )
        .build(),
      HttpResponse.BodyHandlers.ofString(),
    )

    assertThat(response.statusCode())
      .describedAs("subscription creation response body: %s", response.body())
      .isEqualTo(201)

    return UUID.fromString(objectMapper.readTree(response.body()).required("id").asText())
  }

  fun awaitSubscriptionStatus(
    subscriptionId: UUID,
    expectedStatus: String,
  ) {
    await("subscription becomes $expectedStatus")
      .atMost(FLOW_TIMEOUT)
      .pollInterval(POLL_INTERVAL)
      .untilAsserted {
        assertThat(subscriptionStatus(subscriptionId))
          .describedAs("subscription status for subscription_id=%s", subscriptionId)
          .isEqualTo(expectedStatus)
      }
  }

  fun awaitInitialInvoiceStatus(
    subscriptionId: UUID,
    expectedStatus: String,
  ) {
    await("initial invoice becomes $expectedStatus")
      .atMost(FLOW_TIMEOUT)
      .pollInterval(POLL_INTERVAL)
      .untilAsserted {
        assertThat(initialInvoiceStatus(subscriptionId))
          .describedAs("initial invoice status for subscription_id=%s", subscriptionId)
          .isEqualTo(expectedStatus)
      }
  }

  fun awaitFirstPaymentAttemptStatus(
    subscriptionId: UUID,
    expectedStatus: String,
  ) {
    await("first payment attempt becomes $expectedStatus")
      .atMost(FLOW_TIMEOUT)
      .pollInterval(POLL_INTERVAL)
      .untilAsserted {
        assertThat(firstPaymentAttemptStatus(subscriptionId))
          .describedAs("first payment attempt status for subscription_id=%s", subscriptionId)
          .isEqualTo(expectedStatus)
      }
  }

  fun awaitInitialBillingSagaCompleted(subscriptionId: UUID) {
    await("initial billing saga completes")
      .atMost(FLOW_TIMEOUT)
      .pollInterval(POLL_INTERVAL)
      .untilAsserted {
        val saga = initialBillingSaga(subscriptionId)
        assertThat(saga?.status)
          .describedAs("initial billing saga status for subscription_id=%s", subscriptionId)
          .isEqualTo("COMPLETED")
        assertThat(saga?.completed)
          .describedAs("initial billing saga completed_at presence for subscription_id=%s", subscriptionId)
          .isTrue()
      }
  }

  private fun waitForServiceHealth(
    serviceName: String,
    baseUrl: String,
  ) {
    await("$serviceName actuator health is UP")
      .atMost(STARTUP_TIMEOUT)
      .pollInterval(POLL_INTERVAL)
      .untilAsserted {
        val response = getOrRetry("$baseUrl/actuator/health", "$serviceName health")
        assertThat(response.statusCode())
          .describedAs("%s health response body: %s", serviceName, response.body())
          .isEqualTo(200)
        assertThat(objectMapper.readTree(response.body()).required("status").asText())
          .describedAs("%s health status", serviceName)
          .isEqualTo("UP")
      }
  }

  private fun waitForKafkaConnect() {
    val connectorNames = listOf(
      "subscription-outbox-connector",
      "billing-outbox-connector",
      "payment-outbox-connector",
      "orchestrator-command-outbox-connector",
    )

    await("Kafka Connect connectors are RUNNING")
      .atMost(STARTUP_TIMEOUT)
      .pollInterval(POLL_INTERVAL)
      .untilAsserted {
        connectorNames.forEach { connectorName ->
          val response = getOrRetry(
            url = "$kafkaConnectUrl/connectors/$connectorName/status",
            description = "$connectorName status",
          )
          assertThat(response.statusCode())
            .describedAs("%s status response body: %s", connectorName, response.body())
            .isEqualTo(200)

          val status = objectMapper.readTree(response.body())
          assertThat(status.required("connector").required("state").asText())
            .describedAs("%s connector state", connectorName)
            .isEqualTo("RUNNING")
          assertThat(status.required("tasks").all { task -> task.required("state").asText() == "RUNNING" })
            .describedAs("%s task states: %s", connectorName, status.required("tasks"))
            .isTrue()
        }
      }
  }

  private fun subscriptionStatus(subscriptionId: UUID): String? =
    queryString(
      database = Database.SUBSCRIPTION,
      sql = "select status from subscriptions where id = ?",
      parameter = subscriptionId,
    )

  private fun initialInvoiceStatus(subscriptionId: UUID): String? =
    queryString(
      database = Database.BILLING,
      sql = "select status from invoices where subscription_id = ? and invoice_type = 'INITIAL'",
      parameter = subscriptionId,
    )

  private fun firstPaymentAttemptStatus(subscriptionId: UUID): String? =
    queryString(
      database = Database.PAYMENT,
      sql = "select status from payment_attempts where subscription_id = ? and attempt_number = 1",
      parameter = subscriptionId,
    )

  private fun initialBillingSaga(subscriptionId: UUID): SagaRow? =
    connection(Database.ORCHESTRATOR).use { connection ->
      connection.prepareStatement(
        """
        select status, completed_at is not null as completed
        from billing_sagas
        where saga_type = 'initial-subscription-billing'
          and business_key = ?
        """.trimIndent(),
      ).use { statement ->
        statement.setString(1, subscriptionId.toString())
        statement.executeQuery().use { resultSet ->
          if (!resultSet.next()) {
            null
          } else {
            SagaRow(
              status = resultSet.getString("status"),
              completed = resultSet.getBoolean("completed"),
            )
          }
        }
      }
    }

  private fun queryString(
    database: Database,
    sql: String,
    parameter: UUID,
  ): String? =
    connection(database).use { connection ->
      connection.prepareStatement(sql).use { statement ->
        statement.setObject(1, parameter)
        statement.executeQuery().use { resultSet ->
          if (resultSet.next()) resultSet.getString(1) else null
        }
      }
    }

  private fun connection(database: Database) =
    DriverManager.getConnection(database.url, database.username, database.password)

  private fun get(url: String): HttpResponse<String> =
    httpClient.send(
      HttpRequest.newBuilder(URI.create(url))
        .GET()
        .build(),
      HttpResponse.BodyHandlers.ofString(),
    )

  private fun getOrRetry(
    url: String,
    description: String,
  ): HttpResponse<String> =
    runCatching { get(url) }
      .getOrElse { error ->
        throw AssertionError("$description request to $url failed: ${error.message}", error)
      }

  private fun JsonNode.required(fieldName: String): JsonNode =
    requireNotNull(get(fieldName)) { "Missing JSON field '$fieldName' in $this" }

  private data class SagaRow(
    val status: String,
    val completed: Boolean,
  )

  private enum class Database(
    val url: String,
    val username: String,
    val password: String,
  ) {
    SUBSCRIPTION(
      url = "jdbc:postgresql://localhost:5432/subscription_service_db",
      username = "subscription_service_user",
      password = "subscription_service_password",
    ),
    BILLING(
      url = "jdbc:postgresql://localhost:5433/billing_service_db",
      username = "billing_service_user",
      password = "billing_service_password",
    ),
    ORCHESTRATOR(
      url = "jdbc:postgresql://localhost:5434/orchestrator_service_db",
      username = "orchestrator_service_user",
      password = "orchestrator_service_password",
    ),
    PAYMENT(
      url = "jdbc:postgresql://localhost:5435/payment_service_db",
      username = "payment_service_user",
      password = "payment_service_password",
    ),
  }
}
