package com.ilchern.saasbilling.billing.application.service

import com.ilchern.saasbilling.billing.domain.model.BillingPeriod
import com.ilchern.saasbilling.billing.domain.model.Money
import com.ilchern.saasbilling.billing.domain.model.SubscriptionPlan
import org.springframework.stereotype.Service

@Service
class InvoicePricingService {

  fun calculate(
    subscriptionPlan: SubscriptionPlan,
    billingPeriod: BillingPeriod,
    seats: Int,
  ): Money {
    require(seats > 0) { "Seats must be positive" }

    val monthlyAmount = subscriptionPlan.basePriceMinor + subscriptionPlan.seatPriceMinor * seats
    val amount = when (billingPeriod) {
      BillingPeriod.MONTHLY -> monthlyAmount
      BillingPeriod.YEARLY -> monthlyAmount * MONTHS_IN_YEAR
    }
    return Money(amountMinor = amount, currency = USD)
  }

  companion object {
    private const val MONTHS_IN_YEAR = 12
    private const val USD = "USD"
  }
}
