package com.hse.recommendationsystem.infrastructure.repository

import com.hse.recommendationsystem.domain.model.SupplierProfileSnapshot
import com.hse.recommendationsystem.domain.repository.SupplierProfileSnapshotRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class SupplierProfileSnapshotRepositoryImpl(
    private val jdbcTemplate: JdbcTemplate,
) : SupplierProfileSnapshotRepository {
    override fun save(snapshot: SupplierProfileSnapshot) {
        jdbcTemplate.update(
            """
            INSERT INTO supplier_profile_snapshot (
                rfq_id, supplier_id, name, website, profile_url, country, distribution_area,
                description, description_de, description_en,
                supplier_types, products, keywords, product_categories, snapshot_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
        ) { ps ->
            var i = 1
            ps.setObject(i++, snapshot.rfqId)
            ps.setObject(i++, snapshot.supplierId)
            ps.setString(i++, snapshot.name)
            ps.setString(i++, snapshot.website)
            ps.setString(i++, snapshot.profileUrl)
            ps.setString(i++, snapshot.country)
            ps.setString(i++, snapshot.distributionArea)
            ps.setString(i++, snapshot.description)
            ps.setString(i++, snapshot.descriptionDe)
            ps.setString(i++, snapshot.descriptionEn)
            PreparedStatementArray.setTextArray(ps, i++, snapshot.supplierTypes)
            PreparedStatementArray.setTextArray(ps, i++, snapshot.products)
            PreparedStatementArray.setTextArray(ps, i++, snapshot.keywords)
            PreparedStatementArray.setTextArray(ps, i++, snapshot.productCategories)
            ps.setTimestamp(i, java.sql.Timestamp.from(snapshot.snapshotAt))
        }
    }
}
