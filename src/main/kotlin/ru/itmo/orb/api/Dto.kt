package ru.itmo.orb.api

import jakarta.validation.constraints.*
import ru.itmo.orb.model.*
import java.time.ZonedDateTime

// Request-DTOs are immutable; only JPA Entities are mutable.
data class PersonInput(
    @field:NotBlank @field:Size(max = 200) val name: String,
    val coordinateX: Int,
    val coordinateY: Double,
    val eyeColor: Color?,
    val hairColor: Color?,
    val locationId: Long?,
    @field:DecimalMin(value = "0.0", inclusive = false) val height: Double,
    @field:DecimalMin(value = "0.0", inclusive = false) val weight: Double,
    val nationality: Country?,
    val version: Long? = null
)
data class LocationInput(
    val x: Long,
    val y: Float,
    @field:Size(max = 200) val name: String?,
    val version: Long? = null
)
data class LocationResponse(val id: Long, val x: Long, val y: Float, val name: String?, val version: Long)
data class PersonResponse(
    val id: Int, val name: String,
    val coordinateX: Int, val coordinateY: Double,
    val creationDate: ZonedDateTime, val eyeColor: Color?, val hairColor: Color?,
    val location: LocationResponse?, val height: Double, val weight: Double,
    val nationality: Country?, val version: Long
)
data class IdentifierResponse(val id: Int?, val replayed: Boolean = false)
data class CountResponse(val count: Long)
data class ErrorResponse(val error: String)
data class ReassignLocationInput(@field:NotNull val replacementLocationId: Long)

fun Location.toResponse() = LocationResponse(requireNotNull(id), x, y, name, version)
fun Person.toResponse() = PersonResponse(
    requireNotNull(id), name, coordinates.x, coordinates.y, creationDate,
    eyeColor, hairColor, location?.toResponse(), height, weight, nationality, version
)
