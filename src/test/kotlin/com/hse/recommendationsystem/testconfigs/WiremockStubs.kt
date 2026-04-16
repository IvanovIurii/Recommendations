package com.hse.recommendationsystem.testconfigs

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo

object WiremockStubs {
    fun setupRecommendationsFind(wireMockServer: WireMockServer) {
        wireMockServer.resetAll()
        val body =
            WiremockStubs::class.java.getResourceAsStream("/fixtures/recommendations-find-response.json")!!
                .bufferedReader()
                .readText()
        wireMockServer.stubFor(
            post(urlPathEqualTo("/recommendations/find"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body),
                ),
        )
    }
}
