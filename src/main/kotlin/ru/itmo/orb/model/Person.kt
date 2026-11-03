package ru.itmo.orb.model

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Entity
@Table(name = "person_lab_people")
class Person(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,
    @field:NotBlank
    @Column(nullable = false, length = 200)
    var name: String = "",
    @field:NotNull
    @Embedded
    var coordinates: Coordinates = Coordinates(),
    @field:NotNull
    @Column(name = "creation_date", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    var creationDate: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
    @Enumerated(EnumType.STRING) @Column(name = "eye_color", length = 16)
    var eyeColor: Color? = null,
    @Enumerated(EnumType.STRING) @Column(name = "hair_color", length = 16)
    var hairColor: Color? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "location_id")
    var location: Location? = null,
    @field:Positive
    @Column(nullable = false) var height: Double = 1.0,
    @field:Positive
    @Column(nullable = false) var weight: Double = 1.0,
    @Enumerated(EnumType.STRING) @Column(length = 32)
    var nationality: Country? = null,
    @Version @Column(nullable = false) var version: Long = 0
)
