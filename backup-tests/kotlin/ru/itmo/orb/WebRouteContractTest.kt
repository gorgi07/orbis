package ru.itmo.orb

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.model
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import ru.itmo.orb.api.LocationResponse
import ru.itmo.orb.api.PersonResponse
import ru.itmo.orb.service.LocationService
import ru.itmo.orb.service.NotFound
import ru.itmo.orb.service.PersonService
import ru.itmo.orb.web.*
import java.time.ZonedDateTime

/**
 * Checks real Spring MVC handler registration, the common Thymeleaf shell,
 * and meaningful 404 responses without requiring a PostgreSQL connection.
 */
class WebRouteContractTest {
    private lateinit var mvc: MockMvc
    private lateinit var locations: LocationService
    private lateinit var people: PersonService

    @BeforeEach
    fun setup() {
        locations = Mockito.mock(LocationService::class.java)
        people = Mockito.mock(PersonService::class.java)
        mvc = MockMvcBuilders.standaloneSetup(
            HomePageController(), PeoplePageController(people),
            LocationPageController(locations), UtilityPageController()
        ).setControllerAdvice(PageLayoutAdvice(), PageErrorAdvice()).build()
    }

    @Test
    fun `location detail URL is a mapped HTML page for an existing location`() {
        Mockito.`when`(locations.get(3L)).thenReturn(LocationResponse(3L, 10L, 20f, "Офис", 0L))

        mvc.perform(get("/locations/3"))
            .andExpect(status().isOk)
            .andExpect(view().name("app"))
            .andExpect(model().attribute("mode", "location-detail"))
            .andExpect(model().attribute("entityId", "3"))
        Mockito.verify(locations).get(3L)
    }

    @Test
    fun `deleted or unknown location returns branded 404`() {
        Mockito.`when`(locations.get(404L)).thenThrow(NotFound("Локация #404 не найдена"))

        mvc.perform(get("/locations/404"))
            .andExpect(status().isNotFound)
            .andExpect(view().name("error/404"))
            .andExpect(model().attribute("errorMessage", "Локация #404 не найдена"))
            .andExpect(model().attribute("returnUrl", "/locations"))
    }

    @Test
    fun `non numeric location id returns 404 instead of an exception page`() {
        mvc.perform(get("/locations/not-a-number"))
            .andExpect(status().isNotFound)
            .andExpect(view().name("error/404"))
    }

    @Test
    fun `location list remains accessible`() {
        mvc.perform(get("/locations"))
            .andExpect(status().isOk)
            .andExpect(view().name("app"))
            .andExpect(model().attribute("mode", "locations"))
    }

    @Test
    fun `person detail URL renders the people screen for existing person`() {
        Mockito.`when`(people.get(5)).thenReturn(PersonResponse(
            5, "Анна", 1, 2.0, ZonedDateTime.parse("2026-11-01T12:00:00Z"),
            null, null, null, 170.0, 60.0, null, 0L
        ))

        mvc.perform(get("/people/5"))
            .andExpect(status().isOk)
            .andExpect(view().name("app"))
            .andExpect(model().attribute("mode", "detail"))
            .andExpect(model().attribute("entityId", "5"))
    }

    @Test
    fun `deleted person returns 404`() {
        Mockito.`when`(people.get(999)).thenThrow(NotFound("Человек #999 не найден"))
        mvc.perform(get("/people/999"))
            .andExpect(status().isNotFound)
            .andExpect(view().name("error/404"))
    }

    @Test
    fun `people list create operations and audit routes are preserved`() {
        mvc.perform(get("/people"))
            .andExpect(status().isOk)
            .andExpect(model().attribute("mode", "list"))
        mvc.perform(get("/people/new"))
            .andExpect(status().isOk)
            .andExpect(model().attribute("mode", "new"))
        mvc.perform(get("/operations"))
            .andExpect(status().isOk)
            .andExpect(model().attribute("mode", "operations"))
        mvc.perform(get("/audit"))
            .andExpect(status().isOk)
            .andExpect(model().attribute("mode", "audit"))
    }
}
