package ru.itmo.orb.service

import jakarta.persistence.criteria.JoinType
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.orb.api.*
import ru.itmo.orb.model.*
import ru.itmo.orb.repository.LocationRepository
import ru.itmo.orb.repository.PersonRepository

@Service
class PersonService(
    private val people: PersonRepository,
    private val locations: LocationRepository,
    private val audit: AuditService,
    private val events: ChangePublisher,
    private val idempotency: IdempotencyService
) {
    private val sortFields = mapOf(
        "id" to "id", "name" to "name", "creationDate" to "creationDate",
        "eyeColor" to "eyeColor", "hairColor" to "hairColor", "nationality" to "nationality",
        "height" to "height", "weight" to "weight",
        "coordinateX" to "coordinates.x", "coordinateY" to "coordinates.y",
        "locationName" to "location.name"
    )

    @Transactional(readOnly = true)
    fun list(page: Int, size: Int, sortBy: String, direction: String,
             name: String?, eyeColor: Color?, hairColor: Color?, nationality: Country?, locationName: String?): Page<PersonResponse> {
        if (size !in 1..100 || page < 0) throw InvalidInput("Некорректные параметры страницы")
        val property = sortFields[sortBy] ?: throw InvalidInput("Недопустимое поле сортировки")
        val sortDirection = when (direction.lowercase()) {
            "asc" -> Sort.Direction.ASC
            "desc" -> Sort.Direction.DESC
            else -> throw InvalidInput("Направление сортировки: asc либо desc")
        }
        val sort = Sort.by(sortDirection, property).and(Sort.by("id"))
        val spec = Specification<Person> { root, _, cb ->
            val conditions = mutableListOf<jakarta.persistence.criteria.Predicate>()
            if (name != null) conditions.add(cb.equal(root.get<String>("name"), name))
            if (eyeColor != null) conditions.add(cb.equal(root.get<Color>("eyeColor"), eyeColor))
            if (hairColor != null) conditions.add(cb.equal(root.get<Color>("hairColor"), hairColor))
            if (nationality != null) conditions.add(cb.equal(root.get<Country>("nationality"), nationality))
            if (locationName != null) {
                val locationJoin = root.join<Person, Location>("location", JoinType.LEFT)
                conditions.add(cb.equal(locationJoin.get<String>("name"), locationName))
            }
            cb.and(*conditions.toTypedArray())
        }
        return people.findAll(spec, PageRequest.of(page, size, sort)).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun get(id: Int) = find(id).toResponse()

    private fun find(id: Int) = people.findById(id).orElseThrow { NotFound("Человек #$id не найден") }

    private fun linkedLocation(id: Long?): Location? = id?.let {
        locations.findById(it).orElseThrow { NotFound("Локация #$it не найдена") }
    }

    private fun validate(input: PersonInput) {
        if (!input.coordinateY.isFinite() || !input.height.isFinite() || !input.weight.isFinite())
            throw InvalidInput("Все вещественные числа должны быть конечными")
        if (input.height <= 0 || input.weight <= 0) throw InvalidInput("Рост и вес должны быть больше нуля")
    }

    private fun applyInput(person: Person, input: PersonInput) {
        person.name = input.name.trim()
        person.coordinates = Coordinates(input.coordinateX, input.coordinateY)
        person.eyeColor = input.eyeColor
        person.hairColor = input.hairColor
        person.location = linkedLocation(input.locationId)
        person.height = input.height
        person.weight = input.weight
        person.nationality = input.nationality
    }

    @Transactional
    fun create(input: PersonInput, key: String): IdentifierResponse {
        validate(input)
        val (id, replayed) = idempotency.execute(key, "person.create", input.copy(version = null).toString()) {
            val entity = Person()
            applyInput(entity, input)
            val saved = people.saveAndFlush(entity)
            val newId = requireNotNull(saved.id)
            audit.record("PERSON", newId, "CREATE", "after=${saved.toResponse()}")
            events.changed("PERSON", newId, "CREATE")
            newId
        }
        return IdentifierResponse(id, replayed)
    }

    @Transactional
    fun update(id: Int, input: PersonInput): PersonResponse {
        validate(input)
        val p = find(id)
        if (input.version == null || input.version != p.version)
            throw Conflict("Данные изменились у другого пользователя. Обновите страницу")
        val before = p.toResponse().toString()
        applyInput(p, input)
        people.flush()
        audit.record("PERSON", id, "UPDATE", "before=$before; after=${p.toResponse()}")
        events.changed("PERSON", id, "UPDATE")
        return p.toResponse()
    }

    @Transactional
    fun delete(id: Int) {
        val p = people.findById(id).orElse(null) ?: return // idempotent DELETE
        val before = p.toResponse().toString()
        people.delete(p)
        people.flush()
        audit.record("PERSON", id, "DELETE", "before=$before")
        events.changed("PERSON", id, "DELETE")
    }
}
