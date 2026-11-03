package ru.itmo.orb.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "person_lab_users")
class AppUser(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 40)
    var username: String = "",

    @Column(name = "password_hash", nullable = false, length = 100)
    var passwordHash: String = "",

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)
