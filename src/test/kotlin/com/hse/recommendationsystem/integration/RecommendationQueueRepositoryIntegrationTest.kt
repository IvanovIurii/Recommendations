package com.hse.recommendationsystem.integration

import com.hse.recommendationsystem.domain.model.RfqRecommendationQueueType
import com.hse.recommendationsystem.domain.repository.RfqRecommendationsQueueRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class RecommendationQueueRepositoryIntegrationTest {
    @Autowired
    private lateinit var queueRepository: RfqRecommendationsQueueRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun cleanQueue() {
        jdbcTemplate.update("DELETE FROM rfq_recommendations_queue")
    }

    @Test
    fun `enqueue poll delete`() {
        val rfqId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        queueRepository.enqueue(rfqId, RfqRecommendationQueueType.CUSTOMER)

        val batch = queueRepository.pollOldest(10)
        assertThat(batch).hasSize(1)
        assertThat(batch[0].rfqId).isEqualTo(rfqId)
        assertThat(batch[0].queueType).isEqualTo(RfqRecommendationQueueType.CUSTOMER)

        queueRepository.deleteById(batch[0].id)

        assertThat(queueRepository.pollOldest(10)).isEmpty()
    }

    @Test
    fun `enqueue is idempotent per rfq and queue type`() {
        val rfqId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        queueRepository.enqueue(rfqId, RfqRecommendationQueueType.CUSTOMER)
        queueRepository.enqueue(rfqId, RfqRecommendationQueueType.CUSTOMER)

        val batch = queueRepository.pollOldest(10)
        assertThat(batch).hasSize(1)
    }
}
