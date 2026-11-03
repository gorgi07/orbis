package ru.itmo.orb.config

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import ru.itmo.orb.repository.AppUserRepository
import ru.itmo.orb.service.RegistrationService

/**
 * Только локальная разработка: alice/bob всегда имеют задокументированные
 * пароли. Это исправляет ситуацию, когда в сохранённой локальной БД
 * остался другой хеш пароля от старой версии приложения.
 * На сервере данный компонент НЕ создаётся.
 */
@Configuration
@Profile("local")
class LocalDemoUsers {
    private val logger = LoggerFactory.getLogger(LocalDemoUsers::class.java)

    @Bean
    fun seedLocalUsers(
        users: AppUserRepository,
        registration: RegistrationService,
        passwordEncoder: PasswordEncoder
    ) = ApplicationRunner {
        val examples = mapOf(
            "alice" to "local_alice_password",
            "bob" to "local_bob_password"
        )
        for ((username, password) in examples) {
            val existing = users.findByUsername(username)
            if (existing == null) {
                registration.register(username, password, password)
                logger.info("Local demo account '{}' created", username)
            } else if (!passwordEncoder.matches(password, existing.passwordHash)) {
                // Данные Person/Location и чужие аккаунты при этом не затрагиваются.
                existing.passwordHash = requireNotNull(passwordEncoder.encode(password))
                users.saveAndFlush(existing)
                logger.info("Local demo account '{}' password restored", username)
            }
        }
        logger.info("Orbis local demo accounts ready: alice, bob")
    }
}
