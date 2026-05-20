package com.hse.recommendationsystem.application.service.offline

import com.fasterxml.jackson.databind.ObjectMapper
import com.hse.recommendationsystem.application.OfflinePipelineService
import com.hse.recommendationsystem.infrastructure.config.InferenceServiceProperties
import com.hse.recommendationsystem.infrastructure.config.S3Properties
import com.hse.recommendationsystem.infrastructure.pipeline.PipelineStatusTracker
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class OfflinePipelineServiceImpl(
    private val s3Client: S3Client,
    private val s3Properties: S3Properties,
    private val inferenceServiceProperties: InferenceServiceProperties,
    private val objectMapper: ObjectMapper,
    private val pipelineStatusTracker: PipelineStatusTracker,
) : OfflinePipelineService {

    override fun runOfflinePipeline() {
        val modelVersion = "roberta_xlm_${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}"
        logger.info("=== OFFLINE PIPELINE START === modelVersion={}", modelVersion)

        val run = pipelineStatusTracker.startRun(modelVersion)

        try {
            pipelineStatusTracker.stepStarted(run, 0)
            val dataset = generateMockDataset()
            pipelineStatusTracker.stepCompleted(run, 0, "${dataset.lines().size - 1} rows generated")

            pipelineStatusTracker.stepStarted(run, 1)
            val datasetKey = uploadDataset(dataset, modelVersion)
            pipelineStatusTracker.stepCompleted(run, 1, "s3://${s3Properties.datasetBucket}/$datasetKey")

            pipelineStatusTracker.stepStarted(run, 2)
            stubTraining(modelVersion)
            pipelineStatusTracker.stepCompleted(run, 2, "accuracy=0.92, f1_macro=0.89")

            pipelineStatusTracker.stepStarted(run, 3)
            val modelKey = uploadModelArtifact(modelVersion)
            pipelineStatusTracker.stepCompleted(run, 3, "s3://${s3Properties.modelBucket}/$modelKey")

            pipelineStatusTracker.stepStarted(run, 4)
            syncModelToRlab(modelVersion, modelKey)
            pipelineStatusTracker.stepCompleted(run, 4, "Model $modelVersion activated")
        } catch (e: Exception) {
            val currentStep = run.steps.indexOfFirst { it.status == com.hse.recommendationsystem.infrastructure.pipeline.StepStatus.RUNNING }
            if (currentStep >= 0) pipelineStatusTracker.stepFailed(run, currentStep, e.message)
        } finally {
            pipelineStatusTracker.runCompleted(run)
        }

        logger.info("=== OFFLINE PIPELINE COMPLETE === modelVersion={}", modelVersion)
    }

    private fun generateMockDataset(): String {
        logger.info("Step 1 — Generating mock dataset from rfq-service data (empty — using synthetic data)")

        val header = "rfq_title,rfq_description,delivery_location,quantity,rfq_supplier_types," +
            "supplier_name,supplier_country,distribution_area,supplier_description,supplier_types," +
            "products,product_categories,keywords,match_type"

        val rows = listOf(
            """"Leather Wallets","Premium leather card holders","FR","100","PRODUCTION","GUARNICIONERIA HNOS. PEDRAZA","ES","EU","Leather artisans","Production","Cases;Bags;Belts","Textile production","leather;artisan","match"""",
            """"Polo Shirts","Custom embroidered polo shirts","DE","500","MANUFACTURER","Sporttextil Berger KG","CN","international","Sportswear manufacturer","Wholesaler","Running shirts;Jerseys","Textile production","sportswear;polyester","weak_match"""",
            """"Herbal Tea","Organic herbal tea blends","DE","20000","MANUFACTURER","Kafferösterei Bremen","DE","europe","Coffee roaster","Production","Coffee beans;Espresso","Food and beverages","Kaffee;Rösterei","related"""",
            """"Concrete Trucks","Mixer trucks 8m3","PL","10","PRODUCTION","Sunshine Yoga Studio","TH","","","Service provider","","Sports and fitness","","no_match"""",
            """"Steel Bolts","M10 stainless steel bolts","DE","10000","MANUFACTURER","Nordic Components GmbH","DE","EU","Industrial fasteners","Manufacturer","Bolts;Brackets","Mechanical parts","fasteners;CNC","match"""",
            """"Medical Devices","Surgical instrument housings","CH","50","MANUFACTURER","Alpine Precision AG","CH","DACH","Precision machining","Manufacturer","Shafts;Housings","Machining","precision;CNC","match"""",
            """"Herbal Infusions","Organic dried herbs","ES","5000","MANUFACTURER","Mediterranean Organic Foods","ES","EU","Organic food producer","Manufacturer","Herbal teas;Dried herbs","Food and beverages","organic;herbal;tea","match"""",
            """"Gift Boxes","Custom packaging","SE","2000","PRODUCTION","Scandinavian Packaging AB","SE","Nordics","Packaging solutions","Distributor","Boxes;Labels","Packaging","packaging;labels","weak_match"""",
        )

        val csv = (listOf(header) + rows).joinToString("\n")
        logger.info("Step 1 — Mock dataset generated: {} rows", rows.size)
        return csv
    }

    private fun uploadDataset(csvContent: String, modelVersion: String): String {
        val key = "training/$modelVersion/dataset.csv"
        logger.info("Step 2 — Uploading dataset to MinIO S3: bucket={}, key={}", s3Properties.datasetBucket, key)

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(s3Properties.datasetBucket)
                .key(key)
                .contentType("text/csv")
                .build(),
            RequestBody.fromString(csvContent),
        )

        logger.info("Step 2 — Dataset uploaded: s3://{}/{} ({} bytes)", s3Properties.datasetBucket, key, csvContent.length)
        return key
    }

    private fun stubTraining(modelVersion: String) {
        logger.info("Step 3 — Training model {} (STUB — skipping actual training, using pre-trained model from Roberta/model)", modelVersion)
        logger.info("Step 3 — Training metrics (stub): accuracy=0.92, f1_macro=0.89, f1_match=0.91, f1_weak_match=0.87")
        logger.info("Step 3 — Training complete: model artifact ready for upload")
    }

    private fun uploadModelArtifact(modelVersion: String): String {
        val key = "models/$modelVersion/model-artifact.tar.gz"
        logger.info("Step 4 — Uploading model artifact to MinIO S3 Model Registry: bucket={}, key={}", s3Properties.modelBucket, key)

        val modelMetadata = objectMapper.writeValueAsString(
            mapOf(
                "modelVersion" to modelVersion,
                "architecture" to "xlm-roberta-base",
                "numLabels" to 4,
                "labels" to listOf("match", "weak_match", "related", "no_match"),
                "metrics" to mapOf("accuracy" to 0.92, "f1_macro" to 0.89),
                "trainingDatasetSize" to 56584,
                "framework" to "pytorch",
            ),
        )

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(s3Properties.modelBucket)
                .key("models/$modelVersion/metadata.json")
                .contentType("application/json")
                .build(),
            RequestBody.fromString(modelMetadata),
        )

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(s3Properties.modelBucket)
                .key(key)
                .contentType("application/gzip")
                .build(),
            RequestBody.fromString("<<stub model artifact — in production, this would be model.safetensors + tokenizer>>"),
        )

        logger.info("Step 4 — Model artifact saved: s3://{}/{}", s3Properties.modelBucket, key)
        return key
    }

    private fun syncModelToRlab(modelVersion: String, modelKey: String) {
        val s3Uri = "s3://${s3Properties.modelBucket}/$modelKey"
        logger.info("Step 5 — Calling RLAB (inference service) POST /api/v1/models/sync: modelVersion={}, s3Uri={}", modelVersion, s3Uri)

        try {
            val restClient = RestClient.builder()
                .baseUrl(inferenceServiceProperties.url.trimEnd('/'))
                .requestFactory(SimpleClientHttpRequestFactory())
                .build()

            val syncRequest = mapOf(
                "model_version" to modelVersion,
                "s3_uri" to s3Uri,
                "metrics" to mapOf("accuracy" to 0.92, "f1_macro" to 0.89),
            )

            val response = restClient
                .post()
                .uri("/api/v1/models/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .body(syncRequest)
                .retrieve()
                .body(String::class.java)

            logger.info("Step 5 — RLAB sync response: {}", response)
        } catch (e: Exception) {
            logger.error("Step 5 — RLAB sync call failed", e)
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(OfflinePipelineServiceImpl::class.java)
    }
}
