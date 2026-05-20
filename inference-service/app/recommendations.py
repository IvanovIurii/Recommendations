import uuid
from typing import Any, Dict, List, Optional

from fastapi import APIRouter
from pydantic import BaseModel

from app.pipeline import XLMRMatchPredictor

router = APIRouter(prefix="/recommendations")

MATCH_TYPE_ORDER = {"match": 0, "weak_match": 1, "related": 2, "no_match": 3}

MOCK_SUPPLIERS: List[Dict[str, Any]] = [
    {
        "supplierId": "a1000000-0000-4000-8000-000000000001",
        "unifiedSupplierId": "b1000000-0000-4000-8000-000000000001",
        "name": "GUARNICIONERIA HNOS. PEDRAZA",
        "website": "https://guarnicioneria-pedraza.example",
        "profileUrl": "https://marketplace.example/suppliers/guarnicioneria-pedraza",
        "country": "ES",
        "distributionArea": "EU",
        "description": "WE ARE LEATHER ARTISANS, MANUFACTURING ALL TYPES OF ITEMS FOR HUNTING PRACTICE. "
        "WE USE FIRST-CLASS LEATHERS SUCH AS CALF HIDES FOR THE PRODUCTION OF RIFLE AND SHOTGUN CASES, "
        "GREASED SUEDE FOR HOLSTERS AND CASES, AND ALSO NUBUCK AND BOX-CALF TO CREATE LADIES BAGS AND BELTS.",
        "supplierTypes": ["Production"],
        "products": ["Leather cases", "Bags", "Belts"],
        "keywords": ["leather", "artisan", "calf", "nubuck", "box-calf"],
        "productCategories": ["Textile production"],
    },
    {
        "supplierId": "a1000000-0000-4000-8000-000000000002",
        "unifiedSupplierId": "b1000000-0000-4000-8000-000000000002",
        "name": "Sporttextil Berger KG",
        "website": "https://sporttextil-berger.example",
        "profileUrl": "https://marketplace.example/suppliers/sporttextil-berger",
        "country": "CN",
        "distributionArea": "international",
        "description": "Manufacturer of sportswear and activewear. We produce running shirts, cycling jerseys, "
        "and gym wear using polyester and elastane. We do not work with cotton.",
        "supplierTypes": ["Wholesaler"],
        "products": ["Running shirts", "Cycling jerseys", "Gym leggings", "Sports bras"],
        "keywords": ["sportswear", "running", "cycling", "polyester", "activewear"],
        "productCategories": ["Textile production"],
    },
    {
        "supplierId": "a1000000-0000-4000-8000-000000000003",
        "unifiedSupplierId": "b1000000-0000-4000-8000-000000000003",
        "name": "Kafferösterei Bremen GmbH",
        "website": "https://kaffeeroesterei-bremen.example",
        "profileUrl": "https://marketplace.example/suppliers/kaffeeroesterei-bremen",
        "country": "DE",
        "distributionArea": "europe",
        "description": "Specialty coffee roaster offering single-origin and blended coffees. "
        "We roast, grind, and package coffee beans for wholesale and private label. "
        "We do not process tea or herbal products.",
        "supplierTypes": ["Production"],
        "products": ["Single-origin coffee beans", "Espresso blends", "Ground coffee", "Coffee capsules"],
        "keywords": ["Kaffee", "Rösterei", "Espresso", "Bohnen", "Bio-Kaffee", "Privatmarke"],
        "productCategories": ["Food and beverages"],
    },
    {
        "supplierId": "a1000000-0000-4000-8000-000000000004",
        "unifiedSupplierId": "b1000000-0000-4000-8000-000000000004",
        "name": "Sunshine Yoga Studio",
        "website": "https://sunshine-yoga.example",
        "profileUrl": "https://marketplace.example/suppliers/sunshine-yoga",
        "country": "TH",
        "distributionArea": "",
        "description": "",
        "supplierTypes": ["Service provider"],
        "products": [],
        "keywords": [],
        "productCategories": ["Sports and fitness"],
    },
    {
        "supplierId": "a1000000-0000-4000-8000-000000000005",
        "unifiedSupplierId": "b1000000-0000-4000-8000-000000000005",
        "name": "Nordic Components GmbH",
        "website": "https://nordic-components.example",
        "profileUrl": "https://marketplace.example/suppliers/nordic-components",
        "country": "DE",
        "distributionArea": "EU",
        "description": "Industrial fasteners and sheet metal components for automotive and machinery sectors.",
        "supplierTypes": ["Manufacturer", "Distributor"],
        "products": ["Bolts", "Brackets", "Sheet metal parts"],
        "keywords": ["fasteners", "CNC", "automotive"],
        "productCategories": ["Mechanical parts"],
    },
    {
        "supplierId": "a1000000-0000-4000-8000-000000000006",
        "unifiedSupplierId": "b1000000-0000-4000-8000-000000000006",
        "name": "Alpine Precision AG",
        "website": "https://alpine-precision.example",
        "profileUrl": "https://marketplace.example/suppliers/alpine-precision",
        "country": "CH",
        "distributionArea": "DACH",
        "description": "High-precision machining and assemblies for medical and aerospace industries.",
        "supplierTypes": ["Manufacturer"],
        "products": ["Shafts", "Housings", "Surgical instruments"],
        "keywords": ["precision", "CNC", "aerospace", "medical"],
        "productCategories": ["Machining"],
    },
    {
        "supplierId": "a1000000-0000-4000-8000-000000000007",
        "unifiedSupplierId": "b1000000-0000-4000-8000-000000000007",
        "name": "Mediterranean Organic Foods SL",
        "website": "https://med-organic.example",
        "profileUrl": "https://marketplace.example/suppliers/med-organic",
        "country": "ES",
        "distributionArea": "EU",
        "description": "Organic food producer specializing in herbal teas, infusions, and dried herbs. "
        "EU organic certified. Private label and contract packaging available.",
        "supplierTypes": ["Manufacturer"],
        "products": ["Herbal teas", "Dried herbs", "Infusion blends", "Tea bags"],
        "keywords": ["organic", "herbal", "tea", "private label", "EU certified"],
        "productCategories": ["Food and beverages"],
    },
    {
        "supplierId": "a1000000-0000-4000-8000-000000000008",
        "unifiedSupplierId": "b1000000-0000-4000-8000-000000000008",
        "name": "Scandinavian Packaging AB",
        "website": "https://scan-packaging.example",
        "profileUrl": "https://marketplace.example/suppliers/scan-packaging",
        "country": "SE",
        "distributionArea": "Nordics",
        "description": "Packaging materials and labeling solutions for food and consumer goods.",
        "supplierTypes": ["Distributor"],
        "products": ["Boxes", "Labels", "Biodegradable packaging"],
        "keywords": ["packaging", "labels", "biodegradable"],
        "productCategories": ["Packaging"],
    },
]


class SupplierRecommendationsRequest(BaseModel):
    rfqId: Optional[str] = None
    title: Optional[str] = None
    description: Optional[str] = None
    deliveryLocation: Optional[str] = None
    quantity: Optional[str] = None
    supplierTypes: Optional[List[str]] = None
    buyerCountry: Optional[str] = None
    categoryId: Optional[int] = None
    senderProfileId: Optional[str] = None
    recommendCustomers: bool = True


def _classify_supplier(
    predictor: XLMRMatchPredictor,
    rfq: SupplierRecommendationsRequest,
    supplier: Dict[str, Any],
) -> Dict[str, Any]:
    result = predictor.predict(
        rfq_title=rfq.title or "",
        rfq_description=rfq.description or "",
        delivery_location=rfq.deliveryLocation or "",
        quantity=rfq.quantity or "",
        rfq_supplier_types=", ".join(rfq.supplierTypes) if rfq.supplierTypes else "",
        supplier_name=supplier.get("name", ""),
        supplier_country=supplier.get("country", ""),
        distribution_area=supplier.get("distributionArea", ""),
        supplier_description=supplier.get("description", ""),
        supplier_types=", ".join(supplier.get("supplierTypes", [])),
        products=", ".join(supplier.get("products", [])),
        product_categories=", ".join(supplier.get("productCategories", [])),
        keywords=", ".join(supplier.get("keywords", [])),
    )
    return {
        "supplierId": supplier["supplierId"],
        "matchType": result["match_type"],
        "customerInNeed": True,
        "isCustomer": True,
        "modelVersion": "xlmr-rfq-supplier-v1",
        "unifiedSupplierId": supplier.get("unifiedSupplierId"),
        "name": supplier.get("name"),
        "website": supplier.get("website"),
        "profileUrl": supplier.get("profileUrl"),
        "country": supplier.get("country"),
        "distributionArea": supplier.get("distributionArea"),
        "description": supplier.get("description"),
        "descriptionDe": None,
        "descriptionEn": supplier.get("description"),
        "supplierTypes": supplier.get("supplierTypes"),
        "products": supplier.get("products"),
        "keywords": supplier.get("keywords"),
        "productCategories": supplier.get("productCategories"),
    }


def create_find_endpoint(predictor: XLMRMatchPredictor) -> APIRouter:
    @router.post("/find")
    async def find_recommendations(request: SupplierRecommendationsRequest):
        results = []
        for supplier in MOCK_SUPPLIERS:
            classified = _classify_supplier(predictor, request, supplier)
            results.append(classified)

        results.sort(key=lambda r: MATCH_TYPE_ORDER.get(r["matchType"], 99))

        return {"data": {"supplierIdsWithFlag": results}}

    return router
