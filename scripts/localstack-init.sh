#!/bin/bash
set -euo pipefail

export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1

TOPIC_ARN=$(awslocal sns create-topic --name supplier-recommendation-events --query TopicArn --output text)
QUEUE_URL=$(awslocal sqs create-queue --queue-name cns-recommendation-feedback --query QueueUrl --output text)
QUEUE_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$QUEUE_URL" \
  --attribute-names QueueArn \
  --query Attributes.QueueArn --output text)

awslocal sns subscribe \
  --topic-arn "$TOPIC_ARN" \
  --protocol sqs \
  --notification-endpoint "$QUEUE_ARN"

MODEL_SYNC_QUEUE_URL=$(awslocal sqs create-queue --queue-name model-sync-events --query QueueUrl --output text)

echo "LOCALSTACK_SNS_TOPIC_ARN=$TOPIC_ARN"
echo "LOCALSTACK_SQS_QUEUE_URL=$QUEUE_URL"
echo "LOCALSTACK_MODEL_SYNC_QUEUE_URL=$MODEL_SYNC_QUEUE_URL"
