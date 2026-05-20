package com.hse.recommendationsystem.api.controller

import com.hse.recommendationsystem.infrastructure.pipeline.PipelineRun
import com.hse.recommendationsystem.infrastructure.pipeline.PipelineStatusTracker
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/pipeline")
@CrossOrigin(origins = ["http://localhost:5173"])
class PipelineStatusController(
    private val pipelineStatusTracker: PipelineStatusTracker,
) {
    @GetMapping("/runs")
    fun getRuns(): List<PipelineRun> = pipelineStatusTracker.getRuns()

    @GetMapping("/latest")
    fun getLatest(): PipelineRun? = pipelineStatusTracker.getLatestRun()
}
