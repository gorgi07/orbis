package ru.itmo.orb.web

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import ru.itmo.orb.service.InvalidRegistrationException
import ru.itmo.orb.service.RegistrationService
import ru.itmo.orb.service.UsernameAlreadyExistsException

data class RegistrationForm(
    @field:NotBlank(message = "Введите логин")
    @field:Size(min = 3, max = 40, message = "Логин должен содержать от 3 до 40 символов")
    @field:Pattern(regexp = "^[\\p{L}\\p{N}._-]+$", message = "Допустимы буквы, цифры, точка, _ и -")
    var username: String = "",

    @field:Size(min = 8, message = "Пароль должен содержать минимум 8 символов")
    var password: String = "",

    var confirmPassword: String = ""
)

@Controller
class AuthController(private val registration: RegistrationService) {
    @GetMapping("/login")
    fun login(): String = "auth/login"

    @GetMapping("/register")
    fun registerPage(model: Model): String {
        model.addAttribute("registrationForm", RegistrationForm())
        return "auth/register"
    }

    @PostMapping("/register")
    fun register(
        @Valid @ModelAttribute("registrationForm") form: RegistrationForm,
        errors: BindingResult
    ): String {
        if (form.password != form.confirmPassword) {
            errors.rejectValue("confirmPassword", "passwordMismatch", "Пароли не совпадают")
        }
        if (form.password.toByteArray(Charsets.UTF_8).size > 72) {
            errors.rejectValue("password", "passwordTooLong", "Пароль не должен превышать 72 байта")
        }
        if (errors.hasErrors()) return "auth/register"

        return try {
            registration.register(form.username, form.password, form.confirmPassword)
            "redirect:/login?registered"
        } catch (ex: UsernameAlreadyExistsException) {
            errors.rejectValue("username", "duplicate", ex.message ?: "Логин занят")
            "auth/register"
        } catch (ex: InvalidRegistrationException) {
            errors.reject("invalidRegistration", ex.message ?: "Некорректные данные")
            "auth/register"
        }
    }
}
