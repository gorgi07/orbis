package ru.itmo.orb.web

import org.springframework.ui.Model

/**
 * This application renders its cards in JavaScript after serving the common
 * Thymeleaf shell. Keep the page identifiers in one place so URL handlers
 * never have to guess the names expected by app.js.
 */
internal enum class AppScreen(val clientMode: String) {
    PEOPLE("list"),
    PERSON_NEW("new"),
    PERSON_DETAIL("detail"),
    LOCATIONS("locations"),
    LOCATION_DETAIL("location-detail"),
    OPERATIONS("operations"),
    AUDIT("audit")
}

internal fun Model.renderApp(screen: AppScreen, entityId: Number? = null): String {
    addAttribute("mode", screen.clientMode)
    addAttribute("entityId", entityId?.toString() ?: "")
    return "app"
}
