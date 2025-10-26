package aeza.hostmaster.config;

import aeza.hostmaster.agents.services.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    public SecurityConfig(@Lazy AgentService agentService) {
        this.agentService = agentService;
    }

    private final AgentService agentService;
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Value("${SPRING_SECURITY_USER_NAME:admin}")
    private String adminUsername;

    @Value("${SPRING_SECURITY_USER_PASSWORD:admin}")
    private String adminPassword;

    @Bean
    public UserDetailsService adminUserDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails adminUser = User.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(adminUser);
    }

    @Bean
    public DaoAuthenticationProvider agentAuthenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(agentService);
        provider.setPasswordEncoder(passwordEncoder);

        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           UserDetailsService adminUserDetailsService,
                                           DaoAuthenticationProvider agentAuthenticationProvider) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/agents/register",
                                "/api/docs/**",
                                "/api/agents/docs",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"

                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/metric").hasRole("AGENT")
                        .requestMatchers(HttpMethod.GET, "/api/agent/**").hasRole("ADMIN")
                        .requestMatchers("/api/checks/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .userDetailsService(adminUserDetailsService)
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(agentAuthenticationProvider);

        return http.build();
    }
}