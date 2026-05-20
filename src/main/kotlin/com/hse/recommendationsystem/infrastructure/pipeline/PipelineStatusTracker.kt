package com.hse.recommendationsystem.infrastructure.pipeline

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

enum class StepStatus { PENDING, RUNNING, COMPLETED, FAILED }

data class PipelineStep(
    val index: Int,
    val name: String,
    val description: String,
    var status: StepStatus = StepStatus.PENDING,
    var startedAt: Instant? = null,
    var completedAt: Instant? = null,
    var detail: String? = null,
)

data class PipelineRun(
    val id: String,
    val modelVersion: String,
    val startedAt: Instant,
    var completedAt: Instant? = null,
    var status: StepStatus = StepStatus.RUNNING,
    val steps: MutableList<PipelineStep> = mutableListOf(),
)

@Component
class PipelineStatusTracker {

    private val runs = CopyOnWriteArrayList<PipelineRun>()

    fun startRun(modelVersion: String): PipelineRun {
        val run = PipelineRun(
            id = "${modelVersion}_${Instant.now().epochSecond}",
            modelVersion = modelVersion,
            startedAt = Instant.now(),
            steps = mutableListOf(
                PipelineStep(0, "Dataset Generation", "Generate training dataset from RFQ-supplier pairs"),
                PipelineStep(1, "Dataset Upload", "Upload dataset CSV to MinIO S3"),
                PipelineStep(2, "Model Training", "Train XLM-RoBERTa on labeled pairs"),
                PipelineStep(3, "Model Registry", "Upload model artifact to S3 Model Registry"),
                PipelineStep(4, "Model Sync", "Sync new model to RLAB inference service"),
            ),
        )
        runs.add(0, run)
        if (runs.size > 20) runs.removeAt(runs.size - 1)
        return run
    }

    fun stepStarted(run: PipelineRun, stepIndex: Int, detail: String? = null) {
        run.steps[stepIndex].status = StepStatus.RUNNING
        run.steps[stepIndex].startedAt = Instant.now()
        run.steps[stepIndex].detail = detail
    }

    fun stepCompleted(run: PipelineRun, stepIndex: Int, detail: String? = null) {
        run.steps[stepIndex].status = StepStatus.COMPLETED
        run.steps[stepIndex].completedAt = Instant.now()
        if (detail != null) run.steps[stepIndex].detail = detail
    }

    fun stepFailed(run: PipelineRun, stepIndex: Int, detail: String? = null) {
        run.steps[stepIndex].status = StepStatus.FAILED
        run.steps[stepIndex].completedAt = Instant.now()
        if (detail != null) run.steps[stepIndex].detail = detail
    }

    fun runCompleted(run: PipelineRun) {
        run.completedAt = Instant.now()
        run.status = if (run.steps.any { it.status == StepStatus.FAILED }) StepStatus.FAILED else StepStatus.COMPLETED
    }

    fun getRuns(): List<PipelineRun> = runs.toList()

    fun getLatestRun(): PipelineRun? = runs.firstOrNull()
}
