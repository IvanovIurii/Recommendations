import json
import logging
import os
from datetime import datetime, timezone

import boto3
from fastapi import APIRouter
from pydantic import BaseModel

logger = logging.getLogger("inference.model_sync")

router = APIRouter(prefix="/api/v1/models")


class ModelSyncRequest(BaseModel):
    model_version: str
    s3_uri: str
    metrics: dict | None = None


class ModelSyncResponse(BaseModel):
    status: str
    model_version: str
    message: str


def _get_sqs_client():
    endpoint_url = os.environ.get("AWS_ENDPOINT_URL")
    return boto3.client(
        "sqs",
        endpoint_url=endpoint_url,
        region_name=os.environ.get("AWS_DEFAULT_REGION", "us-east-1"),
        aws_access_key_id=os.environ.get("AWS_ACCESS_KEY_ID", "test"),
        aws_secret_access_key=os.environ.get("AWS_SECRET_ACCESS_KEY", "test"),
    )


def _publish_model_loaded_event(model_version: str, s3_uri: str):
    queue_name = os.environ.get("SQS_MODEL_SYNC_QUEUE", "model-sync-events")
    try:
        sqs = _get_sqs_client()
        queue_url = sqs.get_queue_url(QueueName=queue_name)["QueueUrl"]
        event = {
            "eventType": "ModelSyncCompleted",
            "modelVersion": model_version,
            "s3Uri": s3_uri,
            "syncedAt": datetime.now(timezone.utc).isoformat(),
            "status": "ACTIVE",
        }
        sqs.send_message(QueueUrl=queue_url, MessageBody=json.dumps(event))
        logger.info(
            "Published ModelSyncCompleted event for model_version=%s to queue=%s",
            model_version,
            queue_name,
        )
    except Exception:
        logger.exception("Failed to publish ModelSyncCompleted event")


_active_model_version: str = "xlmr-rfq-supplier-v1"


@router.post("/sync", response_model=ModelSyncResponse)
async def sync_model(request: ModelSyncRequest):
    global _active_model_version

    logger.info(
        "=== MODEL SYNC REQUEST === version=%s, s3_uri=%s, metrics=%s",
        request.model_version,
        request.s3_uri,
        request.metrics,
    )

    logger.info(
        "Simulating model download from S3: %s (in production, would download and load weights)",
        request.s3_uri,
    )
    logger.info(
        "Model %s loaded successfully (stub — using pre-loaded model from /app/model)",
        request.model_version,
    )

    old_version = _active_model_version
    _active_model_version = request.model_version
    logger.info(
        "Active model version updated: %s -> %s",
        old_version,
        _active_model_version,
    )

    _publish_model_loaded_event(request.model_version, request.s3_uri)

    return ModelSyncResponse(
        status="ACTIVE",
        model_version=request.model_version,
        message=f"Model {request.model_version} synced and activated",
    )


@router.get("/active")
async def get_active_model():
    return {"model_version": _active_model_version, "status": "ACTIVE"}
