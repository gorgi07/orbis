package ru.itmo.orb.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "person_lab_audit")
class AuditEvent(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @Column(nullable = false, length = 150) var actor: String = "",
    @Column(name = "entity_kind", nullable = false, length = 40) var entityKind: String = "",
    @Column(name = "entity_id", length = 50) var entityId: String? = null,
    @Column(nullable = false, length = 40) var action: String = "",
    @Column(nullable = false, columnDefinition = "text") var details: String = "",
    @Column(name = "happened_at", nullable = false) var happenedAt: Instant = Instant.now()
)
