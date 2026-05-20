package com.hse.recommendationsystem.infrastructure.clients

import com.hse.recommendationsystem.application.TppClient
import com.hse.recommendationsystem.application.TppSupplierCandidate
import com.hse.recommendationsystem.application.domain.model.SupplierRecommendationsRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class MockTppClientImpl : TppClient {

    override fun getCandidates(request: SupplierRecommendationsRequest): List<TppSupplierCandidate> {
        logger.info("=== TPP RECALL === rfqId={}, title='{}' — returning {} mock candidates", request.rfqId, request.title, MOCK_SUPPLIERS.size)
        return MOCK_SUPPLIERS
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(MockTppClientImpl::class.java)

        private val MOCK_SUPPLIERS = listOf(
            TppSupplierCandidate(
                supplierId = "a1000000-0000-4000-8000-000000000001",
                unifiedSupplierId = "b1000000-0000-4000-8000-000000000001",
                name = "GUARNICIONERIA HNOS. PEDRAZA",
                website = "https://guarnicioneria-pedraza.example",
                profileUrl = "https://marketplace.example/suppliers/guarnicioneria-pedraza",
                country = "ES",
                distributionArea = "EU",
                description = "WE ARE LEATHER ARTISANS, MANUFACTURING ALL TYPES OF ITEMS FOR HUNTING PRACTICE. " +
                    "WE USE FIRST-CLASS LEATHERS SUCH AS CALF HIDES FOR THE PRODUCTION OF RIFLE AND SHOTGUN CASES, " +
                    "GREASED SUEDE FOR HOLSTERS AND CASES, AND ALSO NUBUCK AND BOX-CALF TO CREATE LADIES BAGS AND BELTS.",
                supplierTypes = listOf("Production"),
                products = listOf("Leather cases", "Bags", "Belts"),
                keywords = listOf("leather", "artisan", "calf", "nubuck", "box-calf"),
                productCategories = listOf("Textile production"),
            ),
            TppSupplierCandidate(
                supplierId = "a1000000-0000-4000-8000-000000000002",
                unifiedSupplierId = "b1000000-0000-4000-8000-000000000002",
                name = "Sporttextil Berger KG",
                website = "https://sporttextil-berger.example",
                profileUrl = "https://marketplace.example/suppliers/sporttextil-berger",
                country = "CN",
                distributionArea = "international",
                description = "Manufacturer of sportswear and activewear. We produce running shirts, cycling jerseys, " +
                    "and gym wear using polyester and elastane. We do not work with cotton.",
                supplierTypes = listOf("Wholesaler"),
                products = listOf("Running shirts", "Cycling jerseys", "Gym leggings", "Sports bras"),
                keywords = listOf("sportswear", "running", "cycling", "polyester", "activewear"),
                productCategories = listOf("Textile production"),
            ),
            TppSupplierCandidate(
                supplierId = "a1000000-0000-4000-8000-000000000003",
                unifiedSupplierId = "b1000000-0000-4000-8000-000000000003",
                name = "Kafferösterei Bremen GmbH",
                website = "https://kaffeeroesterei-bremen.example",
                profileUrl = "https://marketplace.example/suppliers/kaffeeroesterei-bremen",
                country = "DE",
                distributionArea = "europe",
                description = "Specialty coffee roaster offering single-origin and blended coffees. " +
                    "We roast, grind, and package coffee beans for wholesale and private label. " +
                    "We do not process tea or herbal products.",
                supplierTypes = listOf("Production"),
                products = listOf("Single-origin coffee beans", "Espresso blends", "Ground coffee", "Coffee capsules"),
                keywords = listOf("Kaffee", "Rösterei", "Espresso", "Bohnen", "Bio-Kaffee", "Privatmarke"),
                productCategories = listOf("Food and beverages"),
            ),
            TppSupplierCandidate(
                supplierId = "a1000000-0000-4000-8000-000000000004",
                unifiedSupplierId = "b1000000-0000-4000-8000-000000000004",
                name = "Sunshine Yoga Studio",
                website = "https://sunshine-yoga.example",
                profileUrl = "https://marketplace.example/suppliers/sunshine-yoga",
                country = "TH",
                distributionArea = "",
                description = "",
                supplierTypes = listOf("Service provider"),
                products = emptyList(),
                keywords = emptyList(),
                productCategories = listOf("Sports and fitness"),
            ),
            TppSupplierCandidate(
                supplierId = "a1000000-0000-4000-8000-000000000005",
                unifiedSupplierId = "b1000000-0000-4000-8000-000000000005",
                name = "Nordic Components GmbH",
                website = "https://nordic-components.example",
                profileUrl = "https://marketplace.example/suppliers/nordic-components",
                country = "DE",
                distributionArea = "EU",
                description = "Industrial fasteners and sheet metal components for automotive and machinery sectors.",
                supplierTypes = listOf("Manufacturer", "Distributor"),
                products = listOf("Bolts", "Brackets", "Sheet metal parts"),
                keywords = listOf("fasteners", "CNC", "automotive"),
                productCategories = listOf("Mechanical parts"),
            ),
            TppSupplierCandidate(
                supplierId = "a1000000-0000-4000-8000-000000000006",
                unifiedSupplierId = "b1000000-0000-4000-8000-000000000006",
                name = "Alpine Precision AG",
                website = "https://alpine-precision.example",
                profileUrl = "https://marketplace.example/suppliers/alpine-precision",
                country = "CH",
                distributionArea = "DACH",
                description = "High-precision machining and assemblies for medical and aerospace industries.",
                supplierTypes = listOf("Manufacturer"),
                products = listOf("Shafts", "Housings", "Surgical instruments"),
                keywords = listOf("precision", "CNC", "aerospace", "medical"),
                productCategories = listOf("Machining"),
            ),
            TppSupplierCandidate(
                supplierId = "a1000000-0000-4000-8000-000000000007",
                unifiedSupplierId = "b1000000-0000-4000-8000-000000000007",
                name = "Mediterranean Organic Foods SL",
                website = "https://med-organic.example",
                profileUrl = "https://marketplace.example/suppliers/med-organic",
                country = "ES",
                distributionArea = "EU",
                description = "Organic food producer specializing in herbal teas, infusions, and dried herbs. " +
                    "EU organic certified. Private label and contract packaging available.",
                supplierTypes = listOf("Manufacturer"),
                products = listOf("Herbal teas", "Dried herbs", "Infusion blends", "Tea bags"),
                keywords = listOf("organic", "herbal", "tea", "private label", "EU certified"),
                productCategories = listOf("Food and beverages"),
            ),
            TppSupplierCandidate(
                supplierId = "a1000000-0000-4000-8000-000000000008",
                unifiedSupplierId = "b1000000-0000-4000-8000-000000000008",
                name = "Scandinavian Packaging AB",
                website = "https://scan-packaging.example",
                profileUrl = "https://marketplace.example/suppliers/scan-packaging",
                country = "SE",
                distributionArea = "Nordics",
                description = "Packaging materials and labeling solutions for food and consumer goods.",
                supplierTypes = listOf("Distributor"),
                products = listOf("Boxes", "Labels", "Biodegradable packaging"),
                keywords = listOf("packaging", "labels", "biodegradable"),
                productCategories = listOf("Packaging"),
            ),
        )
    }
}
