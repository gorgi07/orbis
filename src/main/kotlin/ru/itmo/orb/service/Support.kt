package ru.itmo.orb.service

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import ru.itmo.orb.model.AuditEvent
import ru.itmo.orb.model.IdempotencyRecord
import ru.itmo.orb.repository.AuditRepository
import ru.itmo.orb.repository.IdempotencyRepository

fun currentActor(): String = SecurityContextHolder.getContext().authentication?.name ?: "system"
data class DataChanged(val entity: String, val id: String, val action: String)

@Service
class AuditService(private val auditRepository: AuditRepository) {
    private val log = LoggerFactory.getLogger(javaClass)
    fun record(entity: String, id: Any?, action: String, details: String) {
        val actor = currentActor()
        auditRepository.save(AuditEvent(actor = actor, entityKind = entity, entityId = id?.toString(), action = action, details = details))
        log.info("actor={} entity={} id={} action={}", actor, entity, id, action)
    }
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    fun recent() = auditRepository.findAllByOrderByIdDesc(PageRequest.of(0, 60)).content
}

@Service
class ChangePublisher(private val publisher: ApplicationEventPublisher) {
    fun changed(entity: String, id: Any?, action: String) = publisher.publishEvent(DataChanged(entity, id.toString(), action))
}

@Service
class IdempotencyService(private val repository: IdempotencyRepository) {
    // Execute inside the caller's transaction: record and side effect commit together.
    fun execute(key: String, operation: String, fingerprint: String, work: () -> Int?): Pair<Int?, Boolean> {
        if (key.isBlank() || key.length > 120) throw InvalidInput("Idempotency-Key обязателен (1–120 символов)")
        val actor = currentActor()
        val previous = repository.findByActorAndRequestKey(actor, key)
        if (previous != null) {
            if (previous.operation != operation || previous.fingerprint != fingerprint)
                throw Conflict("Ключ идемпотентности уже использован с другими параметрами")
            return previous.resultId to true
        }
        val result = work()
        repository.saveAndFlush(IdempotencyRecord(actor = actor, requestKey = key, operation = operation, fingerprint = fingerprint, resultId = result))
        return result to false
    }
}
