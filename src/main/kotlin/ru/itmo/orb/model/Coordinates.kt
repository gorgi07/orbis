package ru.itmo.orb.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class Coordinates(
    @Column(name = "coord_x", nullable = false)
    var x: Int = 0,
    @Column(name = "coord_y", nullable = false)
    var y: Double = 0.0
)
