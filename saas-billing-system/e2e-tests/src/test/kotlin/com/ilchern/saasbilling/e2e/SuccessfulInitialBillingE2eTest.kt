package com.ilchern.saasbilling.e2e

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SuccessfulInitialBillingE2eTest {

  @Test
  fun `successful initial billing activates subscription and completes saga`() {
    val subscriptionId = E2eTestSupport.createSubscription(paymentMethodToken = "pm_success")

    E2eTestSupport.awaitSubscriptionStatus(subscriptionId, "ACTIVE")
    E2eTestSupport.awaitInitialInvoiceStatus(subscriptionId, "PAID")
    E2eTestSupport.awaitFirstPaymentAttemptStatus(subscriptionId, "SUCCEEDED")
    E2eTestSupport.awaitInitialBillingSagaCompleted(subscriptionId)
  }

  companion object {
    @JvmStatic
    @BeforeAll
    fun waitForEnvironment() {
      E2eTestSupport.waitForEnvironment()
    }
  }
}
