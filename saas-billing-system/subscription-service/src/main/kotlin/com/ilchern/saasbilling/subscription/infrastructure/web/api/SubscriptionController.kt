package com.ilchern.saasbilling.subscription.infrastructure.web.api

import com.ilchern.saasbilling.subscription.application.command.CreateSubscriptionCommand
import com.ilchern.saasbilling.subscription.application.handler.CreateSubscriptionHandler
import com.ilchern.saasbilling.subscription.domain.model.OrganizationId
import com.ilchern.saasbilling.subscription.domain.model.PaymentMethodToken
import com.ilchern.saasbilling.subscription.infrastructure.web.dto.CreateSubscriptionRequest
import com.ilchern.saasbilling.subscription.infrastructure.web.dto.SubscriptionResponse
import com.ilchern.saasbilling.subscription.infrastructure.web.mapper.toResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/subscriptions")
class SubscriptionController(
  private val createSubscriptionHandler: CreateSubscriptionHandler,
) {

  @PostMapping
  fun create(
    @RequestHeader("X-Organization-Id") organizationId: String,
//    @RequestHeader("X-User-Id") userId: String,
    @Valid @RequestBody request: CreateSubscriptionRequest,
  ): ResponseEntity<SubscriptionResponse> {
    val subscription = createSubscriptionHandler.handle(
      CreateSubscriptionCommand(
        organizationId = OrganizationId(organizationId),
//        requestedBy = UserId(userId),
        plan = requireNotNull(request.plan),
        billingPeriod = requireNotNull(request.billingPeriod),
        seats = request.seats,
        paymentMethodToken = PaymentMethodToken(request.paymentMethodToken),
      ),
    )
    return ResponseEntity.status(HttpStatus.CREATED).body(subscription.toResponse())
  }
}