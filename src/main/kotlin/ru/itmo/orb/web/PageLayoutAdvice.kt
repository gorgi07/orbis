package ru.itmo.orb.web

import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute
import java.security.Principal

/** Shared HTML layout fields, not a catch-all page router. */
@ControllerAdvice(assignableTypes = [
    HomePageController::class, PeoplePageController::class,
    LocationPageController::class, UtilityPageController::class
])
class PageLayoutAdvice {
    @ModelAttribute("currentUser")
    fun currentUser(principal: Principal?): String = principal?.name ?: ""
}
