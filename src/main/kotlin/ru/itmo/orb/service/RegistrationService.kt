package ru.itmo.orb.service

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.orb.model.AppUser
import ru.itmo.orb.model.AuditEvent
import ru.itmo.orb.repository.AppUserRepository
import ru.itmo.orb.repository.AuditRepository
import java.util.Locale

class UsernameAlreadyExistsException : RuntimeException("Этот логин уже занят")
class InvalidRegistrationException(message: String) : RuntimeException(message)

@Service
class RegistrationService(
    private val users: AppUserRepository,
    private val encoder: PasswordEncoder,
    private val audit: AuditRepository
) {
    companion object {
        private val USERNAME_PATTERN = Regex("^[\\p{L}\\p{N}._-]{3,40}$")
    }

    @Transactional
    fun register(username: String, password: String, confirmation: String): AppUser {
        val normalized = username.trim().lowercase(Locale.ROOT)
        if (!USERNAME_PATTERN.matches(normalized)) {
            throw InvalidRegistrationException("Логин: 3–40 символов (буквы, цифры, точка, _ или -)")
        }
        if (password.length < 8 || password.toByteArray(Charsets.UTF_8).size > 72) {
            throw InvalidRegistrationException("Пароль: не менее 8 символов и не более 72 байт")
        }
        if (password != confirmation) {
            throw InvalidRegistrationException("Пароли не совпадают")
        }
        if (users.existsByUsername(normalized)) {
            throw UsernameAlreadyExistsException()
        }

        // Unique constraint в PostgreSQL защищает от гонки двух регистраций с одним логином.
        val user = try {
            val passwordHash = requireNotNull(encoder.encode(password)) {
                "PasswordEncoder returned null"
            }
            users.saveAndFlush(AppUser(username = normalized, passwordHash = passwordHash))
        } catch (ex: DataIntegrityViolationException) {
            throw UsernameAlreadyExistsException()
        }
        // В этой же транзакции — событие аудита, без пароля и его хеша.
        audit.save(
            AuditEvent(
                actor = normalized,
                entityKind = "USER",
                entityId = user.id?.toString(),
                action = "REGISTER",
                details = "Регистрация нового пользователя"
            )
        )
        return user
    }
}
