package com.ilchern.saasbilling.subscription.infrastructure.web.api

import com.ilchern.saasbilling.subscription.application.command.CancelSubscriptionAtPeriodEndCommand
import com.ilchern.saasbilling.subscription.application.command.CreateSubscriptionCommand
import com.ilchern.saasbilling.subscription.application.command.ScheduleSubscriptionChangeCommand
import com.ilchern.saasbilling.subscription.application.handler.CancelSubscriptionAtPeriodEndHandler
import com.ilchern.saasbilling.subscription.application.handler.CreateSubscriptionHandler
import com.ilchern.saasbilling.subscription.application.handler.ScheduleSubscriptionChangeHandler
import com.ilchern.saasbilling.subscription.domain.model.OrganizationId
import com.ilchern.saasbilling.subscription.domain.model.PaymentMethodToken
import com.ilchern.saasbilling.subscription.domain.model.SubscriptionId
import com.ilchern.saasbilling.subscription.infrastructure.web.dto.CreateSubscriptionRequest
import com.ilchern.saasbilling.subscription.infrastructure.web.dto.CancelSubscriptionAtPeriodEndRequest
import com.ilchern.saasbilling.subscription.infrastructure.web.dto.ScheduleSubscriptionChangeRequest
import com.ilchern.saasbilling.subscription.infrastructure.web.dto.SubscriptionResponse
import com.ilchern.saasbilling.subscription.infrastructure.web.mapper.toResponse
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/subscriptions")
class SubscriptionController(
  private val createSubscriptionHandler: CreateSubscriptionHandler,
  private val scheduleSubscriptionChangeHandler: ScheduleSubscriptionChangeHandler,
  private val cancelSubscriptionAtPeriodEndHandler: CancelSubscriptionAtPeriodEndHandler,
) {

  @PostMapping
  fun create(
    @RequestHeader("X-Organization-Id") organizationId: String,
    @RequestHeader("Idempotency-Key") idempotencyKey: String,
    @Valid @RequestBody request: CreateSubscriptionRequest,
  ): ResponseEntity<SubscriptionResponse> {
    val subscription = createSubscriptionHandler.handle(
      CreateSubscriptionCommand(
        organizationId = OrganizationId(organizationId),
        idempotencyKey = idempotencyKey,
        plan = requireNotNull(request.plan),
        billingPeriod = requireNotNull(request.billingPeriod),
        seats = request.seats,
        paymentMethodToken = PaymentMethodToken(request.paymentMethodToken),
      ),
    )
    return ResponseEntity.status(HttpStatus.CREATED).body(subscription.toResponse())
  }

  @PostMapping("/{subscriptionId}/changes")
  fun scheduleChange(
    @PathVariable subscriptionId: UUID,
    @Valid @RequestBody request: ScheduleSubscriptionChangeRequest,
  ): ResponseEntity<SubscriptionResponse> {
    val subscription = scheduleSubscriptionChangeHandler.handle(
      ScheduleSubscriptionChangeCommand(
        subscriptionId = SubscriptionId(subscriptionId),
        newSeats = request.newSeats,
        newPlan = requireNotNull(request.newPlan),
      ),
    )
    return ResponseEntity.ok(subscription.toResponse())
  }

  @PostMapping("/{subscriptionId}/cancel-at-period-end")
  fun cancelAtPeriodEnd(
    @PathVariable subscriptionId: UUID,
    @RequestBody(required = false) request: CancelSubscriptionAtPeriodEndRequest?,
  ): ResponseEntity<SubscriptionResponse> {
    val subscription = cancelSubscriptionAtPeriodEndHandler.handle(
      CancelSubscriptionAtPeriodEndCommand(
        subscriptionId = SubscriptionId(subscriptionId),
      ),
    )
    return ResponseEntity.ok(subscription.toResponse())
  }
}
