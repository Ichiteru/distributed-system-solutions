package com.ilchern.saasbilling.subscription

import com.fasterxml.jackson.databind.ObjectMapper
import com.ilchern.saasbilling.subscription.domain.model.BillingPeriod
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionPlan
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionStatus
import com.ilchern.saasbilling.subscription.infrastructure.web.dto.CreateSubscriptionRequest
import com.ilchern.saasbilling.subscription.infrastructure.web.dto.SubscriptionResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@ActiveProfiles("local")
@TestPropertySource(properties = ["spring.kafka.listener.auto-startup=false"])
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SubscriptionRestIntegrationTest(
  @Autowired private val restTemplate: TestRestTemplate,
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
  fun `create subscription stores pending subscription idempotency key and outbox event`() {
    val organizationId = "org-rest-create"
    val request = CreateSubscriptionRequest(
      plan = SubscriptionPlan.PRO,
      billingPeriod = BillingPeriod.MONTHLY,
      seats = 3,
      paymentMethodToken = "pm_test_create",
    )

    val response = postSubscription(
      organizationId = organizationId,
      idempotencyKey = "subscription-create-1",
      request = request,
    )

    assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    val body = requireNotNull(response.body)
    assertThat(body.organizationId).isEqualTo(organizationId)
    assertThat(body.status).isEqualTo(SubscriptionStatus.PENDING)
    assertThat(body.plan).isEqualTo(SubscriptionPlan.PRO)
    assertThat(body.billingPeriod).isEqualTo(BillingPeriod.MONTHLY)
    assertThat(body.seats).isEqualTo(3)

    val subscription = jdbcTemplate.queryForMap(
      """
      select organization_id, status, subscription_plan, billing_period, seats, payment_method_token
      from subscriptions
      where id = ?
      """.trimIndent(),
      body.id,
    )
    assertThat(subscription["organization_id"]).isEqualTo(organizationId)
    assertThat(subscription["status"]).isEqualTo("PENDING")
    assertThat(subscription["subscription_plan"]).isEqualTo("PRO")
    assertThat(subscription["billing_period"]).isEqualTo("MONTHLY")
    assertThat(subscription["seats"]).isEqualTo(3)
    assertThat(subscription["payment_method_token"]).isEqualTo("pm_test_create")

    assertThat(countRows("idempotency_keys")).isEqualTo(1)

    val outbox = jdbcTemplate.queryForMap(
      """
      select aggregatetype, aggregateid, type, payload
      from outbox_messages
      where aggregateid = ?
      """.trimIndent(),
      body.id.toString(),
    )
    assertThat(outbox["aggregatetype"]).isEqualTo("subscription")
    assertThat(outbox["aggregateid"]).isEqualTo(body.id.toString())
    assertThat(outbox["type"]).isEqualTo("SubscriptionCreatedEvent")

    val payload = objectMapper.readTree(outbox["payload"].toString())
    assertThat(payload["subscriptionId"].asText()).isEqualTo(body.id.toString())
    assertThat(payload["organizationId"].asText()).isEqualTo(organizationId)
    assertThat(payload["subscriptionPlan"].asText()).isEqualTo("PRO")
    assertThat(payload["billingPeriod"].asText()).isEqualTo("MONTHLY")
    assertThat(payload["seats"].asInt()).isEqualTo(3)
    assertThat(payload["paymentMethodToken"].asText()).isEqualTo("pm_test_create")
  }

  @Test
  fun `create subscription is idempotent for same organization and idempotency key`() {
    val organizationId = "org-rest-idempotent"
    val idempotencyKey = "subscription-create-idempotent-1"
    val request = CreateSubscriptionRequest(
      plan = SubscriptionPlan.ENTERPRISE,
      billingPeriod = BillingPeriod.YEARLY,
      seats = 8,
      paymentMethodToken = "pm_test_idempotent",
    )

    val firstResponse = postSubscription(
      organizationId = organizationId,
      idempotencyKey = idempotencyKey,
      request = request,
    )
    val secondResponse = postSubscription(
      organizationId = organizationId,
      idempotencyKey = idempotencyKey,
      request = request,
    )

    assertThat(firstResponse.statusCode).isEqualTo(HttpStatus.CREATED)
    assertThat(secondResponse.statusCode).isEqualTo(HttpStatus.CREATED)
    val firstBody = requireNotNull(firstResponse.body)
    val secondBody = requireNotNull(secondResponse.body)
    assertThat(secondBody.id).isEqualTo(firstBody.id)

    assertThat(
      requireNotNull(
        jdbcTemplate.queryForObject(
          "select count(*) from subscriptions where organization_id = ?",
          Long::class.java,
          organizationId,
        ),
      ),
    ).isEqualTo(1)
    assertThat(
      requireNotNull(
        jdbcTemplate.queryForObject(
          """
          select count(*)
          from idempotency_keys
          where organization_id = ?
            and operation = ?
            and idempotency_key = ?
          """.trimIndent(),
          Long::class.java,
          organizationId,
          "create-subscription",
          idempotencyKey,
        ),
      ),
    ).isEqualTo(1)
    assertThat(
      requireNotNull(
        jdbcTemplate.queryForObject(
          """
          select count(*)
          from outbox_messages
          where aggregateid = ?
            and type = ?
          """.trimIndent(),
          Long::class.java,
          firstBody.id.toString(),
          "SubscriptionCreatedEvent",
        ),
      ),
    ).isEqualTo(1)
  }

  private fun postSubscription(
    organizationId: String,
    idempotencyKey: String,
    request: CreateSubscriptionRequest,
  ) =
    restTemplate.postForEntity(
      "/api/v1/subscriptions",
      HttpEntity(request, headers(organizationId, idempotencyKey)),
      SubscriptionResponse::class.java,
    )

  private fun headers(
    organizationId: String,
    idempotencyKey: String,
  ): HttpHeaders =
    HttpHeaders().apply {
      contentType = MediaType.APPLICATION_JSON
      set("X-Organization-Id", organizationId)
      set("Idempotency-Key", idempotencyKey)
    }

  private fun countRows(table: String): Long =
    requireNotNull(jdbcTemplate.queryForObject("select count(*) from $table", Long::class.java))

  companion object {
    @Container
    @JvmStatic
    private val postgres = KPostgreSQLContainer("postgres:16-alpine")
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
      registry.add("SPRING.KAFKA.PROPERTIES.SCHEMA.REGISTRY.URL") { "mock://subscription-service-test" }
    }
  }
}

private class KPostgreSQLContainer(imageName: String) : PostgreSQLContainer<KPostgreSQLContainer>(imageName)
