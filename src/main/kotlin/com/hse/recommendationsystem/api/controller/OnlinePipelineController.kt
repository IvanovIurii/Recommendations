package com.hse.recommendationsystem.api.controller

import com.hse.recommendationsystem.infrastructure.pipeline.OnlinePipelineEvent
import com.hse.recommendationsystem.infrastructure.pipeline.OnlinePipelineEventTracker
import com.hse.recommendationsystem.infrastructure.pipeline.OnlinePipelineState
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/online-pipeline")
@CrossOrigin(origins = ["http://localhost:5173"])
class OnlinePipelineController(
    private val onlinePipelineEventTracker: OnlinePipelineEventTracker,
) {
    @GetMapping("/{rfqId}/events")
    fun getEvents(
        @PathVariable rfqId: UUID,
        @RequestParam(name = "since", defaultValue = "0") since: Int,
    ): List<OnlinePipelineEvent> = onlinePipelineEventTracker.getEvents(rfqId, since)

    @GetMapping("/{rfqId}/state")
    fun getState(@PathVariable rfqId: UUID): OnlinePipelineState? =
        onlinePipelineEventTracker.getState(rfqId)
}
