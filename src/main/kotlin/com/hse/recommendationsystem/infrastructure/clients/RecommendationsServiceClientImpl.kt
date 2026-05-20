package com.hse.recommendationsystem.infrastructure.clients

import com.fasterxml.jackson.databind.ObjectMapper
import com.hse.recommendationsystem.application.InferenceServiceClient
import com.hse.recommendationsystem.application.RecommendationsServiceClient
import com.hse.recommendationsystem.application.TppClient
import com.hse.recommendationsystem.application.TppSupplierCandidate
import com.hse.recommendationsystem.application.domain.model.MatchType
import com.hse.recommendationsystem.application.domain.model.SupplierRecommendationCandidate
import com.hse.recommendationsystem.application.domain.model.SupplierRecommendationsRequest
import com.hse.recommendationsystem.infrastructure.config.RecommendationsProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RecommendationsServiceClientImpl(
    private val tppClient: TppClient,
    private val inferenceServiceClient: InferenceServiceClient,
    private val recommendationsProperties: RecommendationsProperties,
    private val objectMapper: ObjectMapper,
) : RecommendationsServiceClient {

    override fun getRecommendations(request: SupplierRecommendationsRequest): List<SupplierRecommendationCandidate>? {
        logger.info("=== ONLINE PIPELINE START === rfqId={}", request.rfqId)

        val tppCandidates = tppClient.getCandidates(request)
        logger.info("Step 1 — TPP Recall: received {} candidates for rfqId={}", tppCandidates.size, request.rfqId)

        val allowedTypes = recommendationsProperties.allowedMatchTypes.mapNotNull { MatchType.fromDatabase(it) }.toSet()
        logger.info("Step 2 — Allowed match types for filtering: {}", allowedTypes)

        val scoredCandidates = tppCandidates.mapNotNull { supplier ->
            try {
                val prediction = inferenceServiceClient.predict(
                    rfqTitle = request.title ?: "",
                    rfqDescription = request.description ?: "",
                    deliveryLocation = request.deliveryLocation ?: "",
                    quantity = request.quantity ?: "",
                    rfqSupplierTypes = request.supplierTypes?.joinToString(", ") ?: "",
                    supplierName = supplier.name,
                    supplierCountry = supplier.country,
                    distributionArea = supplier.distributionArea,
                    supplierDescription = supplier.description,
                    supplierTypes = supplier.supplierTypes.joinToString(", "),
                    products = supplier.products.joinToString(", "),
                    productCategories = supplier.productCategories.joinToString(", "),
                    keywords = supplier.keywords.joinToString(", "),
                )
                logger.info(
                    "Step 3 — RLAB Inference: supplier='{}' → matchType={} (rfqId={})",
                    supplier.name,
                    prediction.matchType,
                    request.rfqId,
                )
                supplier to prediction
            } catch (e: Exception) {
                logger.error("Inference failed for supplier='{}', skipping", supplier.name, e)
                null
            }
        }

        val filtered = scoredCandidates.filter { (supplier, prediction) ->
            val pass = prediction.matchType in allowedTypes
            if (!pass) {
                logger.info(
                    "Step 4 — FILTERED OUT: supplier='{}', matchType={} (not in {})",
                    supplier.name,
                    prediction.matchType,
                    allowedTypes,
                )
            }
            pass
        }

        logger.info(
            "Step 4 — Post-filter: {} of {} candidates passed (rfqId={})",
            filtered.size,
            scoredCandidates.size,
            request.rfqId,
        )

        if (filtered.isEmpty()) {
            logger.warn("No candidates passed filter for rfqId={}", request.rfqId)
            return null
        }

        val result = filtered
            .sortedBy { (_, prediction) -> MATCH_TYPE_ORDER[prediction.matchType] ?: 99 }
            .map { (supplier, prediction) -> supplier.toCandidate(prediction.matchType) }

        logger.info(
            "=== ONLINE PIPELINE COMPLETE === rfqId={}, recommendations={}",
            request.rfqId,
            result.map { "${it.name} [${it.matchType}]" },
        )

        return result
    }

    private fun TppSupplierCandidate.toCandidate(matchType: MatchType): SupplierRecommendationCandidate =
        SupplierRecommendationCandidate(
            supplierId = UUID.fromString(supplierId),
            unifiedSupplierId = UUID.fromString(unifiedSupplierId),
            matchType = matchType,
            modelVersion = MODEL_VERSION,
            customerInNeed = true,
            isCustomer = true,
            name = name,
            website = website,
            profileUrl = profileUrl,
            country = country,
            distributionArea = distributionArea,
            description = description,
            descriptionDe = null,
            descriptionEn = description,
            supplierTypes = supplierTypes,
            products = products,
            keywords = keywords,
            productCategories = productCategories,
            rawRecommendationJson = objectMapper.valueToTree(this),
        )

    private companion object {
        private val logger = LoggerFactory.getLogger(RecommendationsServiceClientImpl::class.java)
        private const val MODEL_VERSION = "xlmr-rfq-supplier-v1"
        private val MATCH_TYPE_ORDER = mapOf(
            MatchType.MATCH to 0,
            MatchType.WEAK_MATCH to 1,
            MatchType.RELATED to 2,
            MatchType.NO_MATCH to 3,
        )
    }
}
