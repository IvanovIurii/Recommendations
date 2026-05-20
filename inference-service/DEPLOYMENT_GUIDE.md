# Deploying the Inference Service (XLM-RoBERTa)

This guide describes how to run the inference service — a FastAPI application
that serves the fine-tuned XLM-RoBERTa model for RFQ–Supplier match
classification.

The service is part of the `recommendation-system` monorepo and lives in the
`inference-service/` directory.

## 1. Model artifacts

The `model/` directory must contain:

| File | Purpose |
|---|---|
| `config.json` | Model architecture + label mapping |
| `model.safetensors` | Trained weights (~1.1 GB for xlm-roberta-base) |
| `tokenizer.json` | Fast tokenizer vocabulary |
| `tokenizer_config.json` | Tokenizer configuration |

These files are produced by `trainer.save_model()` during training.
They are **not committed to git** (see `.gitignore`) — copy them manually
into `inference-service/model/`.

## 2. Running with Docker Compose (recommended)

From the repository root:

```bash
docker compose up -d
```

This starts the inference service alongside PostgreSQL, LocalStack, and MinIO.
The service is exposed on **port 8081**.

- API docs: http://localhost:8081/docs
- Health check: http://localhost:8081/health
- Streamlit UI: see section 6 below

The model directory is volume-mounted (`./inference-service/model:/app/model`),
so you can swap model files without rebuilding the image.

## 3. Running standalone (without Docker)

```bash
cd inference-service
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

> **CPU-only deployment:** Install the CPU-only PyTorch wheel to save ~1.5 GB:
> `pip install torch --index-url https://download.pytorch.org/whl/cpu`
>
> The model runs fine on CPU for single-request inference (~35–54 ms per pair
> depending on text length).

## 4. API endpoints

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/predict` | Classify a single RFQ–Supplier pair |
| `POST` | `/recommendations/find` | Classify against all mock suppliers (batch) |
| `POST` | `/api/v1/models/sync` | Notify the service of a new model version |
| `GET`  | `/api/v1/models/active` | Get the currently active model version |
| `GET`  | `/health` | Health check |

### `/predict` request

```json
{
  "rfq_title": "High-End Leather Card Holders",
  "rfq_description": "Premium leather wallets...",
  "delivery_location": "FR",
  "quantity": "100",
  "rfq_supplier_types": "PRODUCTION",
  "supplier_name": "GUARNICIONERIA HNOS. PEDRAZA",
  "supplier_country": "ES",
  "distribution_area": "EU",
  "supplier_description": "Leather artisans...",
  "supplier_types": "Production",
  "products": "Cases, Bags, Belts",
  "product_categories": "Textile production",
  "keywords": "leather, artisan"
}
```

### `/predict` response

```json
{
  "match_type": "match",
  "probabilities": {
    "match": 0.749,
    "weak_match": 0.121,
    "related": 0.083,
    "no_match": 0.047
  }
}
```

### `/api/v1/models/sync` request

Called by the offline pipeline when a new model is trained and saved to MinIO:

```json
{
  "model_version": "roberta_xlm_2026-05-02",
  "s3_uri": "s3://models/models/roberta_xlm_2026-05-02/model-artifact.tar.gz",
  "metrics": { "accuracy": 0.92, "f1_macro": 0.89 }
}
```

The service logs the sync, updates the active model version, and publishes a
`ModelSyncCompleted` event to the `model-sync-events` SQS queue.

## 5. Environment variables

| Variable | Default | Description |
|---|---|---|
| `MODEL_DIR` | `./model` (relative to app) | Path to the model directory |
| `AWS_ENDPOINT_URL` | — | LocalStack endpoint for SQS |
| `AWS_ACCESS_KEY_ID` | `test` | AWS credentials (LocalStack) |
| `AWS_SECRET_ACCESS_KEY` | `test` | AWS credentials (LocalStack) |
| `AWS_DEFAULT_REGION` | `us-east-1` | AWS region |
| `SQS_MODEL_SYNC_QUEUE` | `model-sync-events` | SQS queue for model sync events |

## 6. Streamlit demo UI

A Streamlit app is included for interactive demo and presentation screenshots.

```bash
cd inference-service
pip install -r requirements.txt
streamlit run streamlit_app.py
```

- Make sure the inference service is running (Docker or standalone).
- The UI defaults to `http://localhost:8081` (configurable via
  `INFERENCE_API_URL` env var).
- Includes pre-loaded example scenarios for all four match types.
- Streamlit UI: http://localhost:8501

## 7. Optimisations for production

### ONNX Runtime (faster CPU inference)

Convert the model to ONNX for 2–4x speedup on CPU:

```bash
pip install optimum[onnxruntime]
optimum-cli export onnx --model ./model/ ./model-onnx/
```

Then use `ORTModelForSequenceClassification` as a drop-in replacement:

```python
from optimum.onnxruntime import ORTModelForSequenceClassification

model = ORTModelForSequenceClassification.from_pretrained("./model-onnx/")
```

### Quantisation (smaller model, faster inference)

Apply dynamic quantisation to reduce the model from ~1.1 GB to ~280 MB:

```python
from optimum.onnxruntime import ORTQuantizer
from optimum.onnxruntime.configuration import AutoQuantizationConfig

quantizer = ORTQuantizer.from_pretrained("./model-onnx/")
qconfig = AutoQuantizationConfig.avx512_vnni(is_static=False)
quantizer.quantize(save_dir="./model-onnx-quantized/", quantization_config=qconfig)
```

### Batched inference

If you need to classify many pairs at once, add a batch endpoint that tokenises
all pairs together and runs a single forward pass. This is significantly faster
than classifying one pair at a time.

## 8. Model size considerations

| Variant | Parameters | Disk size |
|---|---|---|
| `xlm-roberta-base` | 278M | ~1.1 GB |
| `xlm-roberta-base` + ONNX | 278M | ~1.1 GB |
| `xlm-roberta-base` + ONNX + quantised | 278M | ~280 MB |

## 9. Dockerfile

The Dockerfile (`inference-service/Dockerfile`) builds a lightweight image:

```dockerfile
FROM python:3.11-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app/ ./app/

EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

Model weights are **volume-mounted** at runtime (not baked into the image) to
keep the image small and allow swapping models without a rebuild.

> **Tip:** For GPU deployment use `nvidia/cuda:12.x-runtime-ubuntu22.04` as
> base image and install the CUDA PyTorch wheel.
