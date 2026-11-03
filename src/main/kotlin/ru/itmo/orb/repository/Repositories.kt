package ru.itmo.orb.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import ru.itmo.orb.model.*

interface PersonRepository : JpaRepository<Person, Int>, JpaSpecificationExecutor<Person> {
    fun findByLocationId(id: Long): List<Person>
    fun existsByLocationId(id: Long): Boolean
    fun findFirstByWeightOrderByIdAsc(weight: Double): Person?
    fun findFirstByOrderByIdDesc(): Person?
    fun countByHeightLessThan(height: Double): Long
    fun countByHairColor(color: Color): Long
    fun countByEyeColor(color: Color): Long
}
interface LocationRepository : JpaRepository<Location, Long>
interface AuditRepository : JpaRepository<AuditEvent, Long> {
    fun findAllByOrderByIdDesc(pageable: Pageable): Page<AuditEvent>
}
interface IdempotencyRepository : JpaRepository<IdempotencyRecord, Long> {
    fun findByActorAndRequestKey(actor: String, requestKey: String): IdempotencyRecord?
}
