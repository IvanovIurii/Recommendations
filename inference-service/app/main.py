import logging
from contextlib import asynccontextmanager
from typing import Dict, Optional

from fastapi import FastAPI
from pydantic import BaseModel, Field

from app.pipeline import XLMRMatchPredictor
from app.recommendations import create_find_endpoint
from app.model_sync import router as model_sync_router

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(name)s] %(levelname)s — %(message)s")

predictor = XLMRMatchPredictor()


@asynccontextmanager
async def lifespan(app: FastAPI):
    predictor.load()
    yield


app = FastAPI(
    title="RLAB — RFQ–Supplier Match Inference Service",
    description="Inference service (RLAB MVP) for RFQ–Supplier matching using fine-tuned XLM-RoBERTa. "
    "Provides /predict for single-pair scoring and /api/v1/models/sync for model lifecycle.",
    version="1.0.0",
    lifespan=lifespan,
)

app.include_router(create_find_endpoint(predictor))
app.include_router(model_sync_router)


class PredictionRequest(BaseModel):
    rfq_title: str = ""
    rfq_description: str = ""
    delivery_location: str = ""
    quantity: str = ""
    category_id: Optional[int] = None
    rfq_supplier_types: str = ""
    supplier_name: str = ""
    supplier_country: str = ""
    distribution_area: str = ""
    supplier_description: str = ""
    supplier_types: str = ""
    products: str = ""
    product_categories: str = ""
    keywords: str = ""


class PredictionResponse(BaseModel):
    match_type: str = Field(description="Predicted match category: match, weak_match, related, or no_match")
    probabilities: Dict[str, float] = Field(description="Per-class probabilities")


@app.post("/predict", response_model=PredictionResponse)
async def predict(request: PredictionRequest):
    result = predictor.predict(
        rfq_title=request.rfq_title,
        rfq_description=request.rfq_description,
        delivery_location=request.delivery_location,
        quantity=request.quantity,
        rfq_supplier_types=request.rfq_supplier_types,
        supplier_name=request.supplier_name,
        supplier_country=request.supplier_country,
        distribution_area=request.distribution_area,
        supplier_description=request.supplier_description,
        supplier_types=request.supplier_types,
        products=request.products,
        product_categories=request.product_categories,
        keywords=request.keywords,
    )
    return result


@app.get("/health")
async def health():
    return {"status": "ok"}
