package com.hse.recommendationsystem.infrastructure.repository

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.postgresql.util.PGobject

internal object JsonbSupport {
    fun toPgObject(
        objectMapper: ObjectMapper,
        node: JsonNode?,
    ): PGobject? {
        if (node == null) return null
        val pg = PGobject()
        pg.type = "jsonb"
        pg.value = objectMapper.writeValueAsString(node)
        return pg
    }

    fun parseJson(
        objectMapper: ObjectMapper,
        value: String?,
    ): JsonNode? {
        if (value.isNullOrBlank()) return null
        return objectMapper.readTree(value)
    }
}
