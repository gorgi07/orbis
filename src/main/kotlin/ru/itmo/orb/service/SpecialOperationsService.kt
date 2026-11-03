package ru.itmo.orb.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.orb.api.IdentifierResponse
import ru.itmo.orb.api.PersonResponse
import ru.itmo.orb.api.toResponse
import ru.itmo.orb.model.Color
import ru.itmo.orb.repository.PersonRepository

@Service
class SpecialOperationsService(
    private val people: PersonRepository,
    private val audit: AuditService,
    private val events: ChangePublisher,
    private val idempotency: IdempotencyService
) {
    @Transactional
    fun deleteOneByWeight(weight: Double, key: String): IdentifierResponse {
        if (!weight.isFinite() || weight <= 0.0) throw InvalidInput("Вес должен быть конечным числом больше нуля")
        val (id, replayed) = idempotency.execute(key, "person.delete-by-weight", weight.toString()) {
            val p = people.findFirstByWeightOrderByIdAsc(weight) ?: return@execute null
            val personId = requireNotNull(p.id)
            val old = p.toResponse().toString()
            people.delete(p)
            people.flush()
            audit.record("PERSON", personId, "DELETE_BY_WEIGHT", "weight=$weight; before=$old")
            events.changed("PERSON", personId, "DELETE")
            personId
        }
        return IdentifierResponse(id, replayed)
    }

    @Transactional(readOnly = true)
    fun maxId(): PersonResponse? = people.findFirstByOrderByIdDesc()?.toResponse()

    @Transactional(readOnly = true)
    fun countShorterThan(height: Double): Long {
        if (!height.isFinite()) throw InvalidInput("Введите конечное число")
        return people.countByHeightLessThan(height)
    }

    @Transactional(readOnly = true)
    fun countHairColor(color: Color): Long = people.countByHairColor(color)

    @Transactional(readOnly = true)
    fun countEyeColor(color: Color): Long = people.countByEyeColor(color)
}
