package com.ilchern.saasbilling.payment.infrastructure.persistence.mapper

import com.ilchern.saasbilling.payment.domain.model.OrganizationId
import com.ilchern.saasbilling.payment.domain.model.PaymentMethod
import com.ilchern.saasbilling.payment.domain.model.PaymentMethodId
import com.ilchern.saasbilling.payment.domain.model.PaymentMethodToken
import com.ilchern.saasbilling.payment.infrastructure.persistence.entity.PaymentMethodEntity
import org.springframework.stereotype.Component

@Component
class PaymentMethodPersistenceMapper {

  fun toDomain(entity: PaymentMethodEntity): PaymentMethod =
    PaymentMethod.restore(
      id = PaymentMethodId(entity.id),
      organizationId = OrganizationId(entity.organizationId),
      token = PaymentMethodToken(entity.token),
      createdAt = entity.createdAt,
      updatedAt = entity.updatedAt,
    )

  fun newEntity(source: PaymentMethod): PaymentMethodEntity =
    PaymentMethodEntity(
      id = source.id.value,
      organizationId = source.organizationId.value,
      token = source.token.value,
      createdAt = source.createdAt,
      updatedAt = source.updatedAt(),
    )

  fun copyToEntity(
    source: PaymentMethod,
    target: PaymentMethodEntity,
  ) {
    target.organizationId = source.organizationId.value
    target.token = source.token.value
    target.createdAt = source.createdAt
    target.updatedAt = source.updatedAt()
  }
}
