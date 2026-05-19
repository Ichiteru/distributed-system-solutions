package com.ilchern.saasbilling.orchestrator

import org.apache.avro.generic.GenericRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.DirectFieldAccessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DefaultErrorHandler
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class OrchestratorDltConfigurationIntegrationTest(
  @Autowired private val listenerContainerFactory: ConcurrentKafkaListenerContainerFactory<String, GenericRecord>,
  @Autowired private val errorHandler: DefaultErrorHandler,
  @Autowired private val kafkaTemplate: KafkaTemplate<String, GenericRecord>,
  @Autowired private val jdbcTemplate: JdbcTemplate,
  @Value("\${orchestrator.kafka.topics.subscription-events-dlt}") private val subscriptionEventsDltTopicName: String,
  @Value("\${orchestrator.kafka.topics.billing-events-dlt}") private val billingEventsDltTopicName: String,
  @Value("\${orchestrator.kafka.topics.payment-events-dlt}") private val paymentEventsDltTopicName: String,
  @Value("\${orchestrator.kafka.dlt.retry.backoff-ms}") private val dltRetryBackoffMs: Long,
  @Value("\${orchestrator.kafka.dlt.retry.max-attempts}") private val dltRetryMaxAttempts: Long,
) {

  @Test
  fun `orchestrator event listener factory is wired with DLT error handler`() {
    val factoryErrorHandler = DirectFieldAccessor(listenerContainerFactory)
      .getPropertyValue("commonErrorHandler")

    assertThat(subscriptionEventsDltTopicName).isEqualTo("subscription.events.dlt")
    assertThat(billingEventsDltTopicName).isEqualTo("billing.events.dlt")
    assertThat(paymentEventsDltTopicName).isEqualTo("payment.events.dlt")
    assertThat(dltRetryBackoffMs).isEqualTo(1_000L)
    assertThat(dltRetryMaxAttempts).isEqualTo(3L)
    assertThat(errorHandler).isNotNull
    assertThat(kafkaTemplate).isNotNull
    assertThat(factoryErrorHandler).isSameAs(errorHandler)
  }

  @Test
  fun `spring context starts without kafka listener auto startup`() {
    assertThat(requireNotNull(jdbcTemplate.queryForObject("select 1", Int::class.java))).isEqualTo(1)
  }

  companion object {
    @Container
    @JvmStatic
    private val postgres = OrchestratorDltPostgreSQLContainer("postgres:16-alpine")
      .withDatabaseName("orchestrator_service_db")
      .withUsername("orchestrator_service_user")
      .withPassword("orchestrator_service_password")

    @JvmStatic
    @DynamicPropertySource
    fun registerProperties(registry: DynamicPropertyRegistry) {
      registry.add("SPRING.DATASOURCE.URL", postgres::getJdbcUrl)
      registry.add("SPRING.DATASOURCE.USERNAME", postgres::getUsername)
      registry.add("SPRING.DATASOURCE.PASSWORD", postgres::getPassword)
      registry.add("SPRING.KAFKA.BOOTSTRAP_SERVERS") { "127.0.0.1:65535" }
      registry.add("SPRING.KAFKA.PROPERTIES.SCHEMA.REGISTRY.URL") { "mock://orchestrator-dlt-test" }
    }
  }
}

private class OrchestratorDltPostgreSQLContainer(
  imageName: String,
) : PostgreSQLContainer<OrchestratorDltPostgreSQLContainer>(imageName)
