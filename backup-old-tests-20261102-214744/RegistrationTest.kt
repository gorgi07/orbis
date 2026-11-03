package ru.itmo.orbis

import jakarta.validation.Validation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import ru.itmo.orbis.model.AppUser
import ru.itmo.orbis.model.AuditEvent
import ru.itmo.orbis.repository.AppUserRepository
import ru.itmo.orbis.repository.AuditRepository
import ru.itmo.orbis.service.InvalidRegistrationException
import ru.itmo.orbis.service.RegistrationService
import ru.itmo.orbis.service.UsernameAlreadyExistsException
import ru.itmo.orbis.web.RegistrationForm
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegistrationTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `login is required and password must have at least eight characters`() {
        val form = RegistrationForm(username = "", password = "123", confirmPassword = "123")
        val paths = validator.validate(form).map { it.propertyPath.toString() }.toSet()
        assertTrue("username" in paths)
        assertTrue("password" in paths)
    }

    @Test
    fun `name format accepts Russian and Latin letters but forbids whitespace`() {
        val good = RegistrationForm("Егор_2026", "12345678", "12345678")
        assertTrue(validator.validate(good).isEmpty())
        val bad = RegistrationForm("a b", "12345678", "12345678")
        assertTrue(validator.validate(bad).any { it.propertyPath.toString() == "username" })
    }

    @Test
    fun `password confirmation must match`() {
        val users = Mockito.mock(AppUserRepository::class.java)
        val audit = Mockito.mock(AuditRepository::class.java)
        val service = RegistrationService(users, BCryptPasswordEncoder(), audit)
        assertThrows<InvalidRegistrationException> {
            service.register("egor", "password123", "other-pass")
        }
        Mockito.verifyNoInteractions(users)
    }

    @Test
    fun `existing username is rejected case insensitively`() {
        val users = Mockito.mock(AppUserRepository::class.java)
        val audit = Mockito.mock(AuditRepository::class.java)
        Mockito.`when`(users.existsByUsername("egor")).thenReturn(true)
        val service = RegistrationService(users, BCryptPasswordEncoder(), audit)
        assertThrows<UsernameAlreadyExistsException> {
            service.register("  EGOR  ", "password123", "password123")
        }
        Mockito.verify(users).existsByUsername("egor")
        Mockito.verify(users, Mockito.never()).saveAndFlush(Mockito.any(AppUser::class.java))
    }

    @Test
    fun `registration hashes password and persists audit without secret`() {
        val users = Mockito.mock(AppUserRepository::class.java)
        val audit = Mockito.mock(AuditRepository::class.java)
        val encoder = BCryptPasswordEncoder()
        Mockito.`when`(users.saveAndFlush(Mockito.any(AppUser::class.java))).thenAnswer { invocation ->
            invocation.getArgument<AppUser>(0).also { it.id = 42 }
        }
        val service = RegistrationService(users, encoder, audit)
        val account = service.register("  USER_1  ", "secret12345", "secret12345")
        assertEquals("user_1", account.username)
        assertFalse(account.passwordHash.contains("secret12345"))
        assertTrue(encoder.matches("secret12345", account.passwordHash))
        val event = org.mockito.ArgumentCaptor.forClass(AuditEvent::class.java)
        Mockito.verify(audit).save(event.capture())
        assertEquals("REGISTER", event.value.action)
        assertEquals("user_1", event.value.actor)
        assertFalse(event.value.details.contains("secret12345"))
    }

    @Test
    fun `bcrypt UTF-8 limit is enforced`() {
        val users = Mockito.mock(AppUserRepository::class.java)
        val audit = Mockito.mock(AuditRepository::class.java)
        val service = RegistrationService(users, BCryptPasswordEncoder(), audit)
        val password = "Ы".repeat(37) // 74 bytes in UTF-8
        assertThrows<InvalidRegistrationException> {
            service.register("egor", password, password)
        }
    }
}
