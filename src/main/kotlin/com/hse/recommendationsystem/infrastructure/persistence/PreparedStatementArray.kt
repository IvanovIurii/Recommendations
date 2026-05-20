package com.hse.recommendationsystem.infrastructure.persistence

import java.sql.PreparedStatement
import java.sql.Types

internal object PreparedStatementArray {
    fun setTextArray(ps: PreparedStatement, index: Int, values: List<String>?) {
        if (values == null) {
            ps.setNull(index, Types.ARRAY)
        } else {
            ps.setArray(index, ps.connection.createArrayOf("text", values.toTypedArray()))
        }
    }
}
