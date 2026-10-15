package com.umc.pediupatrao.config;

import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Autorização em duas camadas:
 *  1) regras por URL abaixo (barram o acesso já no filtro do Spring Security);
 *  2) @PreAuthorize nos controllers e services (habilitado por @EnableMethodSecurity).
 * Esconder botões na tela é só conveniência: quem garante a permissão é o backend.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UsuarioDetailsService usuarioDetailsService;

    public SecurityConfig(UsuarioDetailsService usuarioDetailsService) {
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(usuarioDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF ligado (padrão). Formulários Thymeleaf recebem o token automaticamente
                // e as chamadas fetch() enviam o token lido das meta tags do layout.
                .csrf(csrf -> {})
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/error", "/css/**", "/dist/**", "/plugins/**", "/js/**", "/images/**").permitAll()

                        // Usuários e páginas administrativas: só ADMIN
                        .requestMatchers("/usuarios/**", "/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/admin", "/configuracoes", "/teste").hasRole("ADMIN")

                        // Auditoria: somente leitura, para ADMIN e GERENTE
                        .requestMatchers(HttpMethod.GET, "/auditoria/**", "/api/auditoria/**").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers("/auditoria/**", "/api/auditoria/**").denyAll()

                        // Descontos e cancelamentos: exclusivos do GERENTE (inclusive em relação ao ADMIN)
                        .requestMatchers("/pedidos/*/desconto", "/pedidos/*/cancelar",
                                "/api/pedidos/*/desconto", "/api/pedidos/*/cancelamento").hasRole("GERENTE")

                        // Registro de pedidos: ATENDENTE
                        .requestMatchers("/pedidos/novo").hasRole("ATENDENTE")
                        .requestMatchers(HttpMethod.POST, "/pedidos", "/api/pedidos").hasRole("ATENDENTE")

                        // Andamento do pedido: ATENDENTE e GERENTE
                        .requestMatchers("/pedidos/*/status", "/api/pedidos/*/status").hasAnyRole("ATENDENTE", "GERENTE")

                        // Consulta de pedidos: os três perfis
                        .requestMatchers(HttpMethod.GET, "/pedidos/**", "/api/pedidos/**").hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")

                        // Clientes: ATENDENTE cadastra/atualiza, GERENTE consulta (detalhes no @PreAuthorize)
                        .requestMatchers("/clientes/**", "/api/clientes/**").hasAnyRole("ATENDENTE", "GERENTE")

                        // Produtos: leitura para todos (necessária para montar pedidos), gestão para ADMIN e GERENTE
                        .requestMatchers(HttpMethod.GET, "/api/produtos/**").authenticated()
                        .requestMatchers("/produtos/**", "/api/produtos/**").hasAnyRole("ADMIN", "GERENTE")

                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        // APIs respondem 401 em vez de redirecionar para a tela de login
                        .defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**"))
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        .successHandler(authenticationSuccessHandler())
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            response.sendRedirect("/"); // Redireciona para a página inicial após login bem-sucedido
        };
    }
}
