package ru.itmo.orb.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.itmo.orb.model.AppUser

interface AppUserRepository : JpaRepository<AppUser, Long> {
    fun findByUsername(username: String): AppUser?
    fun existsByUsername(username: String): Boolean
}
