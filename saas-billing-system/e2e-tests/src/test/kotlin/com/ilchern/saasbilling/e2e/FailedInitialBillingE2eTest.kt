package com.ilchern.saasbilling.e2e

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class FailedInitialBillingE2eTest {

  @Test
  fun `failed initial billing suspends subscription and completes saga`() {
    val subscriptionId = E2eTestSupport.createSubscription(paymentMethodToken = "pm_fail")

    E2eTestSupport.awaitSubscriptionStatus(subscriptionId, "SUSPENDED")
    E2eTestSupport.awaitInitialInvoiceStatus(subscriptionId, "PAYMENT_PENDING")
    E2eTestSupport.awaitFirstPaymentAttemptStatus(subscriptionId, "FAILED")
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
