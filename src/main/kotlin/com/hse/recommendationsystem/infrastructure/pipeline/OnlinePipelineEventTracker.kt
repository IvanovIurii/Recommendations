package com.hse.recommendationsystem.infrastructure.pipeline

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class OnlinePipelineEvent(
    val timestamp: Instant = Instant.now(),
    val stage: String,
    val message: String,
    val level: String = "INFO",
    val data: Map<String, Any?> = emptyMap(),
)

data class OnlinePipelineState(
    val rfqId: UUID,
    val events: MutableList<OnlinePipelineEvent> = CopyOnWriteArrayList(),
    val startedAt: Instant = Instant.now(),
    var currentStage: String = "CREATED",
)

@Component
class OnlinePipelineEventTracker {

    private val sessions = ConcurrentHashMap<UUID, OnlinePipelineState>()

    fun startSession(rfqId: UUID): OnlinePipelineState {
        val state = OnlinePipelineState(rfqId = rfqId)
        sessions[rfqId] = state
        return state
    }

    fun addEvent(rfqId: UUID, stage: String, message: String, level: String = "INFO", data: Map<String, Any?> = emptyMap()) {
        val state = sessions[rfqId] ?: return
        state.currentStage = stage
        state.events.add(OnlinePipelineEvent(stage = stage, message = message, level = level, data = data))
    }

    fun getState(rfqId: UUID): OnlinePipelineState? = sessions[rfqId]

    fun getEvents(rfqId: UUID, sinceIndex: Int = 0): List<OnlinePipelineEvent> {
        val state = sessions[rfqId] ?: return emptyList()
        return if (sinceIndex < state.events.size) state.events.subList(sinceIndex, state.events.size) else emptyList()
    }
}
