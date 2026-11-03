package ru.itmo.orb.web

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.itmo.orb.api.*
import ru.itmo.orb.model.Color
import ru.itmo.orb.model.Country
import ru.itmo.orb.service.*

@RestController
@RequestMapping("/api/people")
class PersonApi(private val service: PersonService) {
    @GetMapping
    fun list(@RequestParam(defaultValue = "0") page: Int,
             @RequestParam(defaultValue = "15") size: Int,
             @RequestParam(defaultValue = "id") sortBy: String,
             @RequestParam(defaultValue = "asc") direction: String,
             @RequestParam(required = false) name: String?,
             @RequestParam(required = false) eyeColor: Color?,
             @RequestParam(required = false) hairColor: Color?,
             @RequestParam(required = false) nationality: Country?,
             @RequestParam(required = false) locationName: String?) =
        service.list(page, size, sortBy, direction, name, eyeColor, hairColor, nationality, locationName).toPageResponse()

    @GetMapping("/{id}") fun one(@PathVariable id: Int) = service.get(id)
    @PostMapping
    fun create(@Valid @RequestBody input: PersonInput, @RequestHeader("Idempotency-Key") key: String): ResponseEntity<IdentifierResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.create(input, key))
    @PutMapping("/{id}") fun update(@PathVariable id: Int, @Valid @RequestBody input: PersonInput) = service.update(id, input)
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Int) = service.delete(id)
}

@RestController
@RequestMapping("/api/locations")
class LocationApi(private val service: LocationService) {
    @GetMapping fun all() = service.all()
    @GetMapping("/{id}") fun one(@PathVariable id: Long) = service.get(id)
    @GetMapping("/{id}/people") fun people(@PathVariable id: Long) = service.linkedPeople(id)
    @PostMapping @ResponseStatus(HttpStatus.CREATED) fun create(@Valid @RequestBody input: LocationInput) = service.create(input)
    @PutMapping("/{id}") fun update(@PathVariable id: Long, @Valid @RequestBody input: LocationInput) = service.update(id, input)
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long, @RequestParam(required = false) replacementLocationId: Long?) =
        service.deleteAndReassign(id, replacementLocationId)
}

@RestController
@RequestMapping("/api/operations")
class OperationsApi(private val service: SpecialOperationsService) {
    @PostMapping("/delete-by-weight")
    fun deleteByWeight(@RequestParam weight: Double, @RequestHeader("Idempotency-Key") key: String) = service.deleteOneByWeight(weight, key)
    @GetMapping("/max-id") fun maxId() = service.maxId()
    @GetMapping("/count-height") fun shorterThan(@RequestParam height: Double) = CountResponse(service.countShorterThan(height))
    @GetMapping("/count-hair") fun hair(@RequestParam color: Color) = CountResponse(service.countHairColor(color))
    @GetMapping("/count-eyes") fun eyes(@RequestParam color: Color) = CountResponse(service.countEyeColor(color))
}

@RestController
@RequestMapping("/api/audit")
class AuditApi(private val audit: AuditService) {
    @GetMapping fun recent() = audit.recent().map { mapOf(
        "id" to it.id, "actor" to it.actor, "entity" to it.entityKind,
        "entityId" to it.entityId, "action" to it.action, "details" to it.details,
        "at" to it.happenedAt.toString()
    ) }
}
