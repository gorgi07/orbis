package ru.itmo.orb.web

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import ru.itmo.orb.service.LocationService

/** HTML routes only. LocationApi remains responsible for JSON and mutations. */
@Controller
@RequestMapping("/locations")
class LocationPageController(private val locations: LocationService) {
    @GetMapping
    fun index(model: Model): String = model.renderApp(AppScreen.LOCATIONS)

    @GetMapping("/{id}")
    fun detail(@PathVariable id: Long, model: Model): String {
        // Check existence before returning 200. app.js then loads the full
        // location and the linked people via the existing REST endpoints.
        locations.get(id)
        return model.renderApp(AppScreen.LOCATION_DETAIL, id)
    }
}
