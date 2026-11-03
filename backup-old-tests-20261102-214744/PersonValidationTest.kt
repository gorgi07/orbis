package ru.itmo.orbis

import jakarta.validation.Validation
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import ru.itmo.orbis.api.PersonInput

class PersonValidationTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `name cannot be blank and height weight must be positive`() {
        val bad = PersonInput("  ", 0, 0.0, null, null, null, -1.0, 0.0, null)
        val invalid = validator.validate(bad)
        assertEquals(setOf("name", "height", "weight"), invalid.map { it.propertyPath.toString() }.toSet())
    }

    @Test
    fun `nullable enumerations and location are allowed`() {
        val valid = PersonInput("Person", 0, 0.0, null, null, null, 175.0, 75.0, null)
        assertTrue(validator.validate(valid).isEmpty())
    }
}
