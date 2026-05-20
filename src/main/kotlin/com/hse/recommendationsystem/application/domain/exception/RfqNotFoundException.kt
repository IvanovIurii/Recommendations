package com.hse.recommendationsystem.application.domain.exception

import java.util.UUID

class RfqNotFoundException(rfqId: UUID) : RuntimeException("RFQ not found: $rfqId")
