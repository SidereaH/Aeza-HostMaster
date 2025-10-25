package aeza.hostmaster.config;

import aeza.hostmaster.agents.services.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    public SecurityConfig(AgentService agentService) {
        this.agentService = agentService;
    }
    private final AgentService agentService; // реализует UserDetailsService
    // если AgentService не помечен как @Service, то внедрите ваш UserDetailsService

    @Bean
    public PasswordEncoder passwordEncoder() {
        // strength = 10 по умолчанию; можно увеличить, если нужно
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(agentService);
        provider.setPasswordEncoder(passwordEncoder);
        // Можно настроить скрытие деталей ошибок:
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    // AuthenticationManager нужен, если будете вручную аутентифицировать где-то
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // Основная конфигурация HTTP безопасности
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // для API обычно отключают, если не используете cookie-based сессии
            .authenticationProvider(daoAuthenticationProvider(passwordEncoder()))
            .authorizeHttpRequests(auth -> auth
                // публичные endpoint'ы (регистрация и возможно docs)
                .requestMatchers("/api/agents/register", "/api/docs/**", "/actuator/health").permitAll()
                // все остальные /api/agents/** требуют аутентификации
                .requestMatchers("/api/agents/**").hasRole("AGENT")
                // другие endpoint'ы — для админов/разработчиков можно отдельно
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults()) // включаем HTTP Basic
            .sessionManagement(session -> session.disable());

        return http.build();
    }
}