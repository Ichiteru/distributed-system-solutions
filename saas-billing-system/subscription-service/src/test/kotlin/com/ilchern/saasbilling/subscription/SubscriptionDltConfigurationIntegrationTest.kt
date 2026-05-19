package com.ilchern.saasbilling.subscription

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
class SubscriptionDltConfigurationIntegrationTest(
  @Autowired private val listenerContainerFactory: ConcurrentKafkaListenerContainerFactory<String, GenericRecord>,
  @Autowired private val errorHandler: DefaultErrorHandler,
  @Autowired private val kafkaTemplate: KafkaTemplate<String, GenericRecord>,
  @Autowired private val jdbcTemplate: JdbcTemplate,
  @Value("\${subscription.kafka.topics.commands-dlt.name}") private val dltTopicName: String,
  @Value("\${subscription.kafka.dlt.retry.backoff-ms}") private val dltRetryBackoffMs: Long,
  @Value("\${subscription.kafka.dlt.retry.max-attempts}") private val dltRetryMaxAttempts: Long,
) {

  @Test
  fun `subscription commands listener factory is wired with DLT error handler`() {
    val factoryErrorHandler = DirectFieldAccessor(listenerContainerFactory)
      .getPropertyValue("commonErrorHandler")

    assertThat(dltTopicName).isEqualTo("subscription.commands.dlt")
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
    private val postgres = DltPostgreSQLContainer("postgres:16-alpine")
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
      registry.add("SPRING.KAFKA.PROPERTIES.SCHEMA.REGISTRY.URL") { "mock://subscription-dlt-test" }
    }
  }
}

private class DltPostgreSQLContainer(
  imageName: String,
) : PostgreSQLContainer<DltPostgreSQLContainer>(imageName)
