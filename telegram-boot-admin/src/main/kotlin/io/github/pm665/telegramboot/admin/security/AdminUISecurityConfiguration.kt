package io.github.pm665.telegramboot.admin.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class AdminUISecurityConfiguration {
    @Bean
    fun adminUiSecurityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/login/**", "/op-admin/**", "/tg-boot-admin/**")
                    .permitAll()
                    .requestMatchers(
                        "/operator-admin/**",
                        "/admin/**",
                        "/admins/**",
                        "/contacts/**",
                        "/operator-accounts/**",
                        "/operator-sessions/**",
                        "/parks/**",
                        "/park-bots/**",
                        "/park-command-urls/**",
                        "/park-standard-commands/**",
                        "/park-game-configs/**",
                        "/park-game-debug/**",
                        "/bots-health",
                        "/bots-health/check",
                    ).hasRole("SERVICE_ADMIN")
                    .anyRequest()
                    .permitAll()
            }.exceptionHandling { handling ->
                handling.authenticationEntryPoint { _, response, _ ->
                    response.sendRedirect("/login")
                }
            }.csrf { csrf ->
                csrf.disable()
            }.formLogin { form ->
                form.disable()
            }.httpBasic { basic ->
                basic.disable()
            }.logout(Customizer.withDefaults())
            .build()
}
