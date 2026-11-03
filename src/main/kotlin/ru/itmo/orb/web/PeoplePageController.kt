package ru.itmo.orb.web

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import ru.itmo.orb.service.PersonService

/** HTML routes only. /api/people is handled separately by PersonApi. */
@Controller
@RequestMapping("/people")
class PeoplePageController(private val people: PersonService) {
    @GetMapping
    fun index(model: Model): String = model.renderApp(AppScreen.PEOPLE)

    @GetMapping("/new")
    fun create(model: Model): String = model.renderApp(AppScreen.PERSON_NEW)

    @GetMapping("/{id}")
    fun detail(@PathVariable id: Int, model: Model): String {
        // A deleted/unknown person must result in HTTP 404, not an empty shell.
        people.get(id)
        return model.renderApp(AppScreen.PERSON_DETAIL, id)
    }
}
