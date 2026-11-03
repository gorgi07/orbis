package ru.itmo.orb.model

import jakarta.persistence.*

@Entity
@Table(name = "person_lab_idempotency", uniqueConstraints = [UniqueConstraint(columnNames = ["actor", "request_key"])])
class IdempotencyRecord(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @Column(nullable = false, length = 150) var actor: String = "",
    @Column(name = "request_key", nullable = false, length = 120) var requestKey: String = "",
    @Column(nullable = false, length = 80) var operation: String = "",
    @Column(nullable = false, length = 1000) var fingerprint: String = "",
    @Column(name = "result_id") var resultId: Int? = null
)
