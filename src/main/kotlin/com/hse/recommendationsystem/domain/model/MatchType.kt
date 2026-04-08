package com.hse.recommendationsystem.domain.model

enum class MatchType(val databaseValue: String) {
    MATCH("match"),
    WEAK_MATCH("weak_match"),
    RELATED("related"),
    NO_MATCH("no_match"),
    ;

    // todo: simplify it
    companion object {
        fun fromDatabase(value: String?): MatchType? =
            entries.firstOrNull { it.databaseValue.equals(value, ignoreCase = true) }
    }
}
