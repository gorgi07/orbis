package ru.itmo.orb.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import ru.itmo.orb.repository.AppUserRepository
import java.util.Locale

@Configuration
class SecurityConfig {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun userDetailsService(users: AppUserRepository): UserDetailsService = UserDetailsService { username ->
        val normalized = username.trim().lowercase(Locale.ROOT)
        val account = users.findByUsername(normalized)
            ?: throw UsernameNotFoundException("Неверный логин или пароль")
        User.withUsername(account.username)
            .password(account.passwordHash)
            .roles("USER")
            .build()
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.authorizeHttpRequests { requests ->
            requests.requestMatchers("/css/**", "/js/**", "/icons/**", "/login", "/register", "/error", "/favicon.ico").permitAll()
                .anyRequest().authenticated()
        }
            .formLogin { login ->
                login.loginPage("/login")
                    .loginProcessingUrl("/login")
                    .usernameParameter("username")
                    .passwordParameter("password")
                    .failureUrl("/login?error")
                    .defaultSuccessUrl("/people", true)
                    .permitAll()
            }
            .logout { logout -> logout.logoutSuccessUrl("/login?logout") }
        // CSRF включён по умолчанию; обе публичные HTML-формы передают CSRF-токен.
        return http.build()
    }
}
