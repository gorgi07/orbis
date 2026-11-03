package ru.itmo.orb.web

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import ru.itmo.orb.api.ErrorResponse
import ru.itmo.orb.service.*

@RestControllerAdvice(assignableTypes = [PersonApi::class, LocationApi::class, OperationsApi::class, AuditApi::class])
class ErrorHandler {
    @ExceptionHandler(NotFound::class)
    fun notFound(e: NotFound) = ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(e.message ?: "Не найдено"))
    @ExceptionHandler(Conflict::class, ObjectOptimisticLockingFailureException::class)
    fun conflict(e: Exception) = ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse(e.message ?: "Конфликт изменений"))
    @ExceptionHandler(InvalidInput::class)
    fun invalid(e: InvalidInput) = ResponseEntity.badRequest().body(ErrorResponse(e.message ?: "Неверные данные"))
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(e: MethodArgumentNotValidException) = ResponseEntity.badRequest().body(ErrorResponse(
        e.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
    ))
    @ExceptionHandler(HttpMessageNotReadableException::class, MethodArgumentTypeMismatchException::class, MissingRequestHeaderException::class)
    fun malformed(e: Exception) = ResponseEntity.badRequest().body(ErrorResponse("Некорректный запрос: ${e.message?.take(120)}"))
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun constraint(e: DataIntegrityViolationException) = ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse("Ограничение базы данных нарушено или ключ используется конкурентным запросом"))
}
