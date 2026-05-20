# RFQ–Supplier Match Classifier

FastAPI service that classifies how well a supplier matches a Request for Quotation using a fine-tuned XLM-RoBERTa model.

The model outputs one of four classes: **match**, **weak_match**, **related**, or **no_match**.

## Setup

```bash
pip install -r requirements.txt
```

> For CPU-only deployment (saves ~1.5 GB):
> ```bash
> pip install torch --index-url https://download.pytorch.org/whl/cpu
> ```

## Run

### Option A: Docker Compose (full system)

From the **repository root** (`recommendation-system/`):

```bash
docker compose up -d
```

This starts all services:

| Service | URL |
|---|---|
| Inference service (FastAPI) | http://localhost:8081 |
| Inference API docs | http://localhost:8081/docs |
| Pipeline Monitor UI | http://localhost:5173 |
| RFQ Service (Spring Boot) | http://localhost:8080 |
| MinIO API | http://localhost:9002 |
| MinIO Console | http://localhost:9001 (minioadmin/minioadmin) |
| LocalStack | http://localhost:4566 |
| PostgreSQL | localhost:5433 |

Then start the Spring Boot app (RFQ Service):

```bash
./gradlew bootRun
```

RFQ Service runs at http://localhost:8080.

Then start the **Pipeline Monitor UI** (React + Vite):

```bash
cd frontend
npm install
npm run dev
```

Pipeline Monitor UI: http://localhost:5173

> **Note:** The default configuration in `application.yml` uses LocalStack at
> `localhost:4566` as the AWS endpoint for SQS/SNS. No real AWS credentials are needed
> for local development — the defaults (`test`/`test`) work out of the box.

### Option B: Standalone (inference service only)

```bash
cd inference-service
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

### Streamlit UI

In a separate terminal:

```bash
cd inference-service
streamlit run streamlit_app.py
```

Streamlit UI: http://localhost:8501

---

## How to Run & Test Everything (curl)

Below is the full end-to-end walkthrough for both UC1 (online) and UC2 (offline).
All commands assume Docker Compose is running and Spring Boot app is started.

### 0. Verify services are up

```bash
# Inference service health
curl -s http://localhost:8081/health | python3 -m json.tool

# RFQ Service health (Spring Boot actuator)
curl -s http://localhost:8080/actuator/health | python3 -m json.tool

# Active model version
curl -s http://localhost:8081/api/v1/models/active | python3 -m json.tool
```

### UC1 — Online Pipeline (end-to-end)

#### Step 1: Create an RFQ

```bash
curl -s -X POST http://localhost:8080/api/v1/rfq \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "buyer@example.com",
    "fullName": "Max Mustermann",
    "countryCode": "DE",
    "title": "High-End Leather Card Holders / Wallets",
    "description": "We are looking for a new manufacturing partner to relaunch two existing card holder / wallet models. Products: Slim card holder / wallet (2 models). Quantities: Small series, approx. 20-100 pcs per model per year. Materials: Leather will be supplied by us (box calf and exotic leathers). Quality level: Premium / luxury quality.",
    "deliveryLocation": "FR",
    "quantity": "20-100 pieces per model per year",
    "supplierTypes": ["PRODUCTION"],
    "buyerCountry": "DE",
    "categoryId": 100033
  }' | python3 -m json.tool
```

Save the `rfqId` from the response.

#### Step 2: Accept the RFQ (triggers recommendation pipeline)

```bash
curl -s -X POST http://localhost:8080/api/v1/rfq/{rfqId}/accept
```

Watch the Spring Boot logs — you will see the full pipeline:

```
=== ONLINE PIPELINE START === rfqId=...
Step 1 — TPP Recall: received 8 candidates
Step 3 — RLAB Inference: supplier='GUARNICIONERIA HNOS. PEDRAZA' → matchType=MATCH
Step 3 — RLAB Inference: supplier='Sporttextil Berger KG' → matchType=WEAK_MATCH
Step 4 — FILTERED OUT: supplier='Kafferösterei Bremen GmbH', matchType=RELATED
Step 4 — FILTERED OUT: supplier='Sunshine Yoga Studio', matchType=NO_MATCH
...
=== ONLINE PIPELINE COMPLETE === rfqId=..., recommendations=[GUARNICIONERIA... [MATCH], ...]
```

#### Step 3: Check the RFQ status (should be PROCESSED)

```bash
curl -s http://localhost:8080/api/v1/rfq/{rfqId} | python3 -m json.tool
```

#### Step 4: List recommendations

```bash
curl -s "http://localhost:8080/api/v1/rfq/{rfqId}/recommendations?page=0&pageSize=10" \
  | python3 -m json.tool
```

#### Step 5: Select a supplier

```bash
curl -s -X POST http://localhost:8080/api/v1/rfq/{rfqId}/recommendations/{supplierId}/select \
  -H 'Content-Type: application/json' \
  -d '{"reason": "Best match for leather products"}'
```

### UC2 — Offline Pipeline

The offline pipeline runs automatically ~15 seconds after Spring Boot starts.
Watch the logs for:

```
=== OFFLINE PIPELINE START === modelVersion=roberta_xlm_2026-05-02
Step 1 — Mock dataset generated: 8 rows
Step 2 — Dataset uploaded: s3://datasets/training/.../dataset.csv
Step 3 — Training model (STUB)
Step 4 — Model artifact saved: s3://models/models/.../model-artifact.tar.gz
Step 5 — Calling RLAB POST /api/v1/models/sync
=== OFFLINE PIPELINE COMPLETE ===
```

Then the inference service logs:

```
=== MODEL SYNC REQUEST === version=roberta_xlm_2026-05-02, s3_uri=...
Active model version updated: xlmr-rfq-supplier-v1 -> roberta_xlm_2026-05-02
Published ModelSyncCompleted event
```

And back in Spring Boot:

```
=== MODEL SYNC EVENT RECEIVED ===
=== NEW MODEL IN USE === modelVersion=roberta_xlm_2026-05-02, status=ACTIVE
```

#### Verify manually

```bash
# Check active model version on inference service
curl -s http://localhost:8081/api/v1/models/active | python3 -m json.tool

# Check MinIO buckets (via AWS CLI against MinIO)
aws --endpoint-url http://localhost:9002 s3 ls s3://datasets/ --recursive
aws --endpoint-url http://localhost:9002 s3 ls s3://models/ --recursive

# Trigger model sync manually
curl -s -X POST http://localhost:8081/api/v1/models/sync \
  -H 'Content-Type: application/json' \
  -d '{
    "model_version": "roberta_xlm_manual_test",
    "s3_uri": "s3://models/test/model.tar.gz",
    "metrics": {"accuracy": 0.95, "f1_macro": 0.92}
  }' | python3 -m json.tool
```

---

### Direct inference service testing (curl)

These calls go directly to the inference service, bypassing the RFQ Service.
Use port **8081** (Docker Compose) or **8000** (standalone).

## Testing Report

### Test environment

| Parameter | Value |
|---|---|
| Machine | Apple Silicon (macOS arm64) |
| Python | 3.9 |
| PyTorch | 2.8.0 (CPU) |
| Transformers | 4.57.6 |
| Model | xlm-roberta-base (278M params, 1.1 GB) |
| Serving | uvicorn + FastAPI, single worker |

### Test scenarios and results

All four model classes covered with >= 65% confidence on the top prediction.
Each scenario was run **10 times**; latency is measured end-to-end via the HTTP client
(includes tokenization, inference, JSON serialization, and localhost network round-trip).

| # | Scenario | Prediction | Confidence | Avg latency | Median | Min | Max |
|---|---|---|---|---|---|---|---|
| 1 | Leather wallets RFQ vs leather artisan (ES) | **match** | 74.9% | 51.4 ms | 51.2 ms | 50.2 ms | 54.0 ms |
| 2 | Cotton polo shirts RFQ vs sportswear mfg (CN) | **weak_match** | 66.0% | 42.5 ms | 42.4 ms | 42.2 ms | 42.9 ms |
| 3 | Herbal tea RFQ vs coffee roaster (DE) | **related** | 79.3% | 45.7 ms | 45.6 ms | 45.4 ms | 46.3 ms |
| 4 | Concrete trucks RFQ vs yoga studio (TH) | **no_match** | 78.9% | 34.5 ms | 34.5 ms | 34.2 ms | 35.0 ms |

**Key observations:**
- Inference latency ranges from **34 to 54 ms** on a local CPU, varying with input text length.
- Shorter inputs (scenario 4: sparse supplier fields) are faster; longer inputs (scenario 1: detailed descriptions) are slower.
- Latency is very stable across runs (< 4 ms spread), indicating predictable performance.

### Cloud deployment latency estimate

The table below estimates end-to-end latency for a real-world scenario:
**model hosted on Alibaba Cloud (Frankfurt `eu-central-1`), called from AWS (`eu-central-1` Frankfurt).**

| Component | Latency | Source |
|---|---|---|
| **Model inference (CPU)** | 30-60 ms | Measured locally; similar on a cloud 4-vCPU instance |
| **Model inference (GPU, NVIDIA A10)** | 7-22 ms | Benchmarks: xlm-roberta-base on NVIDIA A10 (batch=1, 50-500 chars) |
| **Model inference (ONNX + quantized CPU)** | 10-20 ms | 2-4x speedup over vanilla PyTorch per DEPLOYMENT_GUIDE |
| **Network: Alibaba Cloud Frankfurt <-> AWS Frankfurt** | 2-8 ms | Same-city cross-cloud via public internet or peering |
| **Network: Alibaba Cloud London <-> AWS Frankfurt** | 15-25 ms | Cross-region EU; AWS eu-central-1 to eu-west-1 ~20 ms |
| **Network: Alibaba Cloud Singapore <-> AWS Frankfurt** | 150-175 ms | Cross-continent; AWS eu-central-1 to ap-southeast-1 ~159 ms |
| **HTTP overhead (serialization, TLS)** | 2-5 ms | FastAPI JSON encode/decode + TLS handshake (amortized) |

#### Projected end-to-end latency (single request)

| Deployment | Inference | Network RTT | Overhead | **Total** |
|---|---|---|---|---|
| Alibaba Frankfurt (CPU) -> AWS Frankfurt | 45 ms | 5 ms | 3 ms | **~53 ms** |
| Alibaba Frankfurt (ONNX quantized CPU) -> AWS Frankfurt | 15 ms | 5 ms | 3 ms | **~23 ms** |
| Alibaba Frankfurt (GPU A10) -> AWS Frankfurt | 15 ms | 5 ms | 3 ms | **~23 ms** |
| Alibaba London (CPU) -> AWS Frankfurt | 45 ms | 20 ms | 3 ms | **~68 ms** |
| Alibaba Singapore (CPU) -> AWS Frankfurt | 45 ms | 165 ms | 3 ms | **~213 ms** |

#### Recommendations

1. **Co-locate in the same city** (both Frankfurt). Cross-cloud same-region latency is only 2-8 ms, making it negligible compared to inference time.
2. **Use ONNX Runtime + quantization** for CPU deployment. This cuts inference from ~45 ms to ~15 ms and reduces the model from 1.1 GB to 280 MB, making the total round-trip ~23 ms.
3. **Avoid cross-continent calls.** Alibaba Cloud Asia to AWS Europe adds 150+ ms of network latency, dominating the total time.
4. **Consider Direct Connect / Express Connect** for production. Alibaba Cloud ExpressConnect to AWS Direct Connect via a shared colocation (e.g., Equinix Frankfurt) can bring cross-cloud latency under 2 ms with SLA guarantees, though it adds provisioning cost and complexity.
5. **Batch inference** for bulk scoring. If classifying many RFQ-supplier pairs at once, batching (e.g., 16-32 pairs per forward pass) reduces per-pair time to ~5-10 ms on GPU.

### Comparison with LLM-based classification (ChatGPT, Qwen)

Instead of a fine-tuned XLM-R model, the same classification task can be done by
prompting a general-purpose LLM. Below is a comparison of latency and cost.

**Assumptions:** ~500 input tokens (RFQ + supplier text as a prompt), ~30 output tokens
(JSON with match_type + probabilities). Classification is a short-output task, so
**time-to-first-token (TTFT) dominates** the total latency.

#### Latency comparison

| Approach | Latency (single request) | Notes |
|---|---|---|
| **XLM-R fine-tuned (CPU, local)** | **35-54 ms** | Measured in this report |
| **XLM-R fine-tuned (ONNX quantized CPU)** | **~15 ms** | Estimated 2-4x speedup |
| **XLM-R fine-tuned (GPU A10)** | **7-22 ms** | Benchmark data |
| GPT-4o (OpenAI API) | **800-1,000 ms** | TTFT ~850 ms + ~30 tokens at 115 t/s |
| GPT-4o mini (OpenAI API) | **550-650 ms** | TTFT ~540 ms + ~30 tokens at 68 t/s |
| Qwen-Plus (Alibaba Cloud DashScope) | **500-800 ms** | Alibaba Cloud general latency 100-300 ms TTFT + generation |
| Qwen-Turbo (Alibaba Cloud DashScope) | **300-500 ms** | Fastest Qwen model, optimized for simple tasks |
| Qwen3.5-Plus (Alibaba Cloud) | **2,300-2,600 ms** | TTFT ~2.3 s; MoE architecture overhead |
| Qwen3.5 397B via DeepInfra | **700-1,000 ms** | TTFT ~670 ms, 138 t/s output |

The fine-tuned XLM-R model is **10-20x faster** than any LLM API for this task.

#### Cost comparison (per request and at scale)

| Approach | Cost per request | Cost per 100K requests | Notes |
|---|---|---|---|
| **XLM-R self-hosted (CPU)** | **~$0** | **$50-150/mo** (instance cost) | 4-vCPU cloud VM; unlimited requests |
| GPT-4o | $0.00155 | $155 | $2.50/1M input, $10/1M output |
| GPT-4o mini | $0.000093 | $9.30 | $0.15/1M input, $0.60/1M output |
| Qwen-Plus (DashScope) | $0.000236 | $23.60 | $0.40/1M input, $1.20/1M output |
| Qwen-Turbo (DashScope) | $0.000031 | $3.10 | $0.05/1M input, $0.20/1M output |
| Qwen 2.5 72B (DeepInfra) | $0.000122 | $12.20 | $0.23/1M blended |

At 100K requests/month the XLM-R instance pays for itself vs GPT-4o after **month 1**.
Even compared to the cheapest LLM option (Qwen-Turbo at $3.10/100K), self-hosting
breaks even at ~500K-1M requests/month — but with the advantage of **no per-request
cost ceiling, no rate limits, and full data privacy**.

#### Quality trade-offs

| Factor | Fine-tuned XLM-R | LLM (zero-shot / few-shot) |
|---|---|---|
| Accuracy on this task | High (trained on 56K labeled pairs) | Moderate without fine-tuning; GPT-4o zero-shot typically ~85% on classification |
| Consistency | Deterministic (same input = same output) | Non-deterministic; temperature / sampling variance |
| Latency | 15-54 ms | 300-2,600 ms |
| Cost at scale | Fixed (instance cost) | Linear with request volume |
| Multilingual | Native (XLM-R trained on 100 languages) | Good (GPT-4o, Qwen both multilingual) |
| Maintenance | Requires retraining when categories change | Prompt update only |
| Cold start | ~10 s model load, then instant | No cold start (API always warm) |

#### Verdict

For this specific 4-class classification task with labeled training data, the **fine-tuned
XLM-R model is the clear winner**: 10-20x lower latency, deterministic outputs, zero
marginal cost at scale, and full data control. LLM APIs are better suited for
prototyping, tasks without labeled data, or when the classification schema changes
frequently and retraining is impractical.

### Data sources

- AWS inter-region latency: [economize.cloud/resources/aws/latency](https://www.economize.cloud/resources/aws/latency)
- Cross-cloud latency (Alibaba/AWS): [cloudflew.com](https://cloudflew.com/article/Network_en/612) — measured 80 ms average for non-colocated regions, <2 ms for same-city Direct Connect
- XLM-RoBERTa inference benchmarks: [arxiv.org/html/2510.18921v1](https://arxiv.org/html/2510.18921v1) — Apple Silicon vs NVIDIA A10 comparison
- Alibaba Cloud Southeast Asia latency: [dev.to comparison](https://dev.to/hdlfacebook_b3f6272d8b31c/real-world-comparison-deploying-cloud-infrastructure-with-alibaba-cloud-tencent-cloud-and-aws-in-54op)
- GPT-4o latency: [ailatency.com](https://www.ailatency.com/models/openai-gpt-4o.html) — TTFT ~850 ms, total ~618-992 ms
- GPT-4o mini latency: [ailatency.com](https://www.ailatency.com/models/openai-gpt-4o-mini.html) — TTFT ~540 ms, total ~575-631 ms
- OpenAI pricing: [openai.com/api/pricing](https://openai.com/api/pricing/)
- Qwen3.5 benchmarks: [deepinfra.com](https://deepinfra.com/blog/qwen3-5-397b-a17b-api-benchmarks) — Alibaba Cloud TTFT 2.31 s
- Qwen/DashScope pricing: [theneuralbase.com](https://theneuralbase.com/qwen/learn/advanced/dashscope-pricing-comparison/), [doc.aiscouncil.com](https://doc.aiscouncil.com/providers/qwen/)

---

## Test Scenarios (curl)

All scenarios below produce a confident prediction (>= 65% probability for the top class).

### 1. match (74.9%) — Luxury leather wallets vs leather artisan

```bash
curl -s http://localhost:8081/predict -H 'Content-Type: application/json' -d '{
  "rfq_title": "High-End Leather Card Holders / Wallets",
  "rfq_description": "We are looking for a new manufacturing partner to relaunch two existing card holder / wallet models. Products: Slim card holder / wallet (2 models). Quantities: Small series, approx. 20-100 pcs per model per year. Materials: Leather will be supplied by us (box calf and exotic leathers). Quality level: Premium / luxury quality.",
  "delivery_location": "FR",
  "quantity": "20-100 pieces per model per year",
  "category_id": 100033,
  "rfq_supplier_types": "PRODUCTION",
  "supplier_name": "GUARNICIONERIA HNOS. PEDRAZA",
  "supplier_country": "ES",
  "distribution_area": "",
  "supplier_description": "WE ARE LEATHER ARTISANS, MANUFACTURING ALL TYPES OF ITEMS FOR HUNTING PRACTICE. WE USE FIRST-CLASS LEATHERS SUCH AS CALF HIDES FOR THE PRODUCTION OF RIFLE AND SHOTGUN CASES, GREASED SUEDE FOR HOLSTERS AND CASES, AND ALSO NUBUCK AND BOX-CALF TO CREATE LADIES BAGS AND BELTS.",
  "supplier_types": "Production",
  "products": "",
  "product_categories": "Textile production",
  "keywords": ""
}' | python3 -m json.tool
```

### 2. weak_match (66.0%) — Cotton polo shirts vs sportswear manufacturer

```bash
curl -s http://localhost:8081/predict -H 'Content-Type: application/json' -d '{
  "rfq_title": "Custom embroidered polo shirts corporate",
  "rfq_description": "We need 500 custom embroidered polo shirts with company logo for our sales team. 100% cotton piqué, various sizes S-XXL.",
  "delivery_location": "DE",
  "quantity": "500",
  "category_id": 100033,
  "rfq_supplier_types": "MANUFACTURER",
  "supplier_name": "Sporttextil Berger KG",
  "supplier_country": "CN",
  "distribution_area": "international",
  "supplier_description": "Manufacturer of sportswear and activewear. We produce running shirts, cycling jerseys, and gym wear using polyester and elastane. We do not work with cotton.",
  "supplier_types": "Wholesaler",
  "products": "Running shirts,Cycling jerseys,Gym leggings,Sports bras",
  "product_categories": "Textile production",
  "keywords": "sportswear,running,cycling,polyester,activewear"
}' | python3 -m json.tool
```

### 3. related (79.3%) — Herbal tea vs coffee roaster

```bash
curl -s http://localhost:8081/predict -H 'Content-Type: application/json' -d '{
  "rfq_title": "Organic herbal tea blends private label",
  "rfq_description": "We are seeking a tea manufacturer for private label organic herbal tea blends. Chamomile, peppermint, rooibos blends. Packaged in biodegradable tea bags, retail-ready boxes. EU organic certification required.",
  "delivery_location": "DE",
  "quantity": "20000 boxes",
  "category_id": 100010,
  "rfq_supplier_types": "MANUFACTURER",
  "supplier_name": "Kafferösterei Bremen GmbH",
  "supplier_country": "DE",
  "distribution_area": "europe",
  "supplier_description": "Specialty coffee roaster offering single-origin and blended coffees. We roast, grind, and package coffee beans for wholesale and private label. We do not process tea or herbal products.",
  "supplier_types": "Production",
  "products": "Single-origin coffee beans,Espresso blends,Ground coffee,Coffee capsules",
  "product_categories": "Food and beverages",
  "keywords": "Kaffee,Rösterei,Espresso,Bohnen,Kaffeeröstung,Bio-Kaffee,Privatmarke"
}' | python3 -m json.tool
```

### 4. no_match (78.9%) — Concrete mixer trucks vs yoga studio

```bash
curl -s http://localhost:8081/predict -H 'Content-Type: application/json' -d '{
  "rfq_title": "Concrete mixer trucks 8 cubic meters",
  "rfq_description": "We need concrete mixer trucks with 8m3 drum capacity for our construction fleet. Must be Euro 6 emission standard.",
  "delivery_location": "PL",
  "quantity": "10",
  "category_id": 100080,
  "rfq_supplier_types": "PRODUCTION",
  "supplier_name": "Sunshine Yoga Studio",
  "supplier_country": "TH",
  "distribution_area": "",
  "supplier_description": "",
  "supplier_types": "Service provider",
  "products": "",
  "product_categories": "Sports and fitness",
  "keywords": ""
}' | python3 -m json.tool
```
