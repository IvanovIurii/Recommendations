package com.hse.recommendationsystem.api

import com.hse.recommendationsystem.api.exception.ValidationException
import com.hse.recommendationsystem.application.domain.exception.InvalidRfqStatusException
import com.hse.recommendationsystem.application.domain.exception.RfqNotFoundException
import com.hse.recommendationsystem.application.domain.exception.SupplierNotRecommendedException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.client.HttpClientErrorException

@ControllerAdvice
class ExceptionHandler {

    @ExceptionHandler(ValidationException::class)
    fun handle(ex: ValidationException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest().body(ErrorResponse(message = ex.message ?: "Bad Request"))

    @ExceptionHandler(RfqNotFoundException::class)
    fun handle(ex: RfqNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(message = ex.message ?: "Not Found"))

    @ExceptionHandler(InvalidRfqStatusException::class)
    fun handle(ex: InvalidRfqStatusException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest().body(ErrorResponse(message = ex.message ?: "Bad Request"))

    @ExceptionHandler(SupplierNotRecommendedException::class)
    fun handle(ex: SupplierNotRecommendedException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest().body(ErrorResponse(message = ex.message ?: "Bad Request"))

    @ExceptionHandler(Exception::class)
    fun handle(ex: Exception): ResponseEntity<ErrorResponse> =
        if (ex is HttpClientErrorException.BadRequest) {
            ResponseEntity.badRequest().body(ErrorResponse(message = ex.message ?: "Bad Request"))
        } else {
            ResponseEntity.internalServerError().body(ErrorResponse(message = ex.message ?: "Server Error"))
        }
}

data class ErrorResponse(val message: String)
