package ru.itmo.orb.web

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import ru.itmo.orb.service.NotFound

/** HTML navigation errors render the branded 404 page; REST errors stay JSON. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice(assignableTypes = [PeoplePageController::class, LocationPageController::class])
class PageErrorAdvice {
    private fun renderNotFound(model: Model, message: String, request: HttpServletRequest): String {
        val isLocation = request.requestURI
            .removePrefix(request.contextPath)
            .startsWith("/locations/")
        model.addAttribute("errorMessage", message)
        model.addAttribute("returnUrl", if (isLocation) "/locations" else "/people")
        model.addAttribute("returnLabel", if (isLocation) "Вернуться к локациям" else "Вернуться к людям")
        return "error/404"
    }

    @ExceptionHandler(NotFound::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun notFound(ex: NotFound, model: Model, request: HttpServletRequest): String =
        renderNotFound(model, ex.message ?: "Запись не найдена", request)

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun invalidIdentifier(model: Model, request: HttpServletRequest): String =
        renderNotFound(model, "Неверный адрес записи", request)
}
