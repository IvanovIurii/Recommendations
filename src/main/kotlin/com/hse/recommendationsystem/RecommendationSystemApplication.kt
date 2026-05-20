package com.hse.recommendationsystem

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableJdbcRepositories(basePackages = ["com.hse.recommendationsystem.infrastructure.persistence"])
class RecommendationSystemApplication

fun main(args: Array<String>) {
    runApplication<RecommendationSystemApplication>(*args)
}
