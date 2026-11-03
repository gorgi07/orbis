package ru.itmo.orb.model

import jakarta.persistence.*
import jakarta.validation.constraints.Pattern

@Entity
@Table(name = "person_lab_locations")
class Location(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false) var x: Long = 0,
    @Column(nullable = false) var y: Float = 0f,
    @field:Pattern(regexp = "(?s).*\\S.*", message = "Название не должно быть пустым")
    @Column(length = 200) var name: String? = null,
    @Version @Column(nullable = false) var version: Long = 0
)
