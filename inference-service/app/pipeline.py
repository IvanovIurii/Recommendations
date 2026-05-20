import os
from pathlib import Path

import torch
from transformers import AutoTokenizer, AutoModelForSequenceClassification

MODEL_DIR = Path(os.environ.get("MODEL_DIR", str(Path(__file__).resolve().parent.parent / "model")))

ID2LABEL = {0: "match", 1: "weak_match", 2: "related", 3: "no_match"}


class XLMRMatchPredictor:
    def __init__(self, model_dir: Path = MODEL_DIR):
        self._model_dir = model_dir
        self._model = None
        self._tokenizer = None

    def load(self):
        self._tokenizer = AutoTokenizer.from_pretrained(self._model_dir)
        self._model = AutoModelForSequenceClassification.from_pretrained(
            self._model_dir,
        )
        self._model.eval()

    def predict(
        self,
        rfq_title: str = "",
        rfq_description: str = "",
        delivery_location: str = "",
        quantity: str = "",
        rfq_supplier_types: str = "",
        supplier_name: str = "",
        supplier_country: str = "",
        distribution_area: str = "",
        supplier_description: str = "",
        supplier_types: str = "",
        products: str = "",
        product_categories: str = "",
        keywords: str = "",
    ) -> dict:
        rfq_text = (
            f"Title: {rfq_title} "
            f"Description: {rfq_description} "
            f"Delivery: {delivery_location} "
            f"Quantity: {quantity} "
            f"Supplier types wanted: {rfq_supplier_types}"
        )
        supplier_text = (
            f"Name: {supplier_name} "
            f"Country: {supplier_country} "
            f"Area: {distribution_area} "
            f"Description: {supplier_description} "
            f"Types: {supplier_types} "
            f"Products: {products} "
            f"Categories: {product_categories} "
            f"Keywords: {keywords}"
        )

        inputs = self._tokenizer(
            rfq_text,
            supplier_text,
            truncation=True,
            max_length=512,
            return_tensors="pt",
        )

        with torch.no_grad():
            logits = self._model(**inputs).logits

        probs = torch.softmax(logits, dim=-1).squeeze().tolist()
        pred_id = int(torch.argmax(logits, dim=-1).item())

        return {
            "match_type": ID2LABEL[pred_id],
            "probabilities": {
                ID2LABEL[i]: round(p, 4) for i, p in enumerate(probs)
            },
        }
