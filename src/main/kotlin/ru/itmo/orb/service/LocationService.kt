package ru.itmo.orb.service

import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.orb.api.*
import ru.itmo.orb.model.Location
import ru.itmo.orb.repository.LocationRepository
import ru.itmo.orb.repository.PersonRepository

@Service
class LocationService(
    private val locations: LocationRepository,
    private val people: PersonRepository,
    private val audit: AuditService,
    private val events: ChangePublisher
) {
    private fun validate(input: LocationInput) {
        if (!input.y.isFinite()) throw InvalidInput("Координата Y должна быть конечным числом")
        if (input.name != null && input.name.isBlank()) throw InvalidInput("Название не может быть пустым; оставьте null")
    }
    private fun find(id: Long): Location = locations.findById(id).orElseThrow { NotFound("Локация #$id не найдена") }

    @Transactional(readOnly = true)
    fun all(): List<LocationResponse> = locations.findAll(Sort.by("id")).map { it.toResponse() }

    @Transactional(readOnly = true)
    fun get(id: Long): LocationResponse = find(id).toResponse()

    @Transactional(readOnly = true)
    fun linkedPeople(id: Long): List<PersonResponse> {
        find(id) // 404 if the location no longer exists
        return people.findByLocationId(id).map { it.toResponse() }
    }

    @Transactional
    fun create(input: LocationInput): LocationResponse {
        validate(input)
        val saved = locations.saveAndFlush(Location(x = input.x, y = input.y, name = input.name?.trim()))
        audit.record("LOCATION", saved.id, "CREATE", "after=${saved.toResponse()}")
        events.changed("LOCATION", saved.id, "CREATE")
        return saved.toResponse()
    }

    @Transactional
    fun update(id: Long, input: LocationInput): LocationResponse {
        validate(input)
        val location = find(id)
        if (input.version == null || location.version != input.version) throw Conflict("Локация обновлена другим пользователем")
        val before = location.toResponse().toString()
        location.x = input.x
        location.y = input.y
        location.name = input.name?.trim()
        locations.flush()
        audit.record("LOCATION", id, "UPDATE", "before=$before; after=${location.toResponse()}")
        events.changed("LOCATION", id, "UPDATE")
        return location.toResponse()
    }

    @Transactional
    fun deleteAndReassign(id: Long, replacementId: Long?) {
        val original = locations.findById(id).orElse(null) ?: return // idempotent DELETE
        if (id == replacementId) throw InvalidInput("Нужна другая локация")
        val linked = people.findByLocationId(id)
        if (linked.isNotEmpty() && replacementId == null)
            throw InvalidInput("Локация используется: необходимо указать другую локацию")
        val replacement = replacementId?.let { find(it) }
        linked.forEach { it.location = replacement }
        people.flush()
        locations.delete(original)
        locations.flush()
        audit.record("LOCATION", id, "DELETE_REASSIGN", "replacement=$replacementId, people=${linked.map { it.id }}")
        events.changed("LOCATION", id, "DELETE_REASSIGN")
    }
}
