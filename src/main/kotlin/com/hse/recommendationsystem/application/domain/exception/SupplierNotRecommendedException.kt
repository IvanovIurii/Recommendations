package com.hse.recommendationsystem.application.domain.exception

import java.util.UUID

class SupplierNotRecommendedException(supplierId: UUID, rfqId: UUID) :
    RuntimeException("Supplier $supplierId is not among recommendations for RFQ $rfqId")
