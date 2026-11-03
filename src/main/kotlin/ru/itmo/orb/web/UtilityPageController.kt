package ru.itmo.orb.web

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class UtilityPageController {
    @GetMapping("/operations")
    fun operations(model: Model): String = model.renderApp(AppScreen.OPERATIONS)

    @GetMapping("/audit")
    fun audit(model: Model): String = model.renderApp(AppScreen.AUDIT)
}
