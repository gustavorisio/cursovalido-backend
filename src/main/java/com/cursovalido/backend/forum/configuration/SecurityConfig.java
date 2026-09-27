package com.cursovalido.backend.forum.configuration;

import com.cursovalido.backend.authentication.configuration.TokenJwtFiltro;
import com.cursovalido.backend.authentication.configuration.PermissoesSeguranca;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties(PermissoesSeguranca.class)
public class SecurityConfig {
    @Autowired
    private TokenJwtFiltro tokenJwtFiltro;
    @Autowired
    private PermissoesSeguranca permissoes;
    @Value("${aplicacao.frontend-origens:http://localhost:3000,http://localhost:5173}")
    private String origensFrontend;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .headers(headers -> headers
                        .frameOptions(frame -> frame.disable())
                        .addHeaderWriter((request, response) -> {
                            if (documentoPublico(request)) {
                                response.setHeader("Content-Security-Policy",
                                        "frame-ancestors " + origensFrontend.replace(',', ' ').trim() + ";");
                            } else {
                                response.setHeader("X-Frame-Options", "DENY");
                            }
                        }))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/autenticacao/cadastro", "/api/autenticacao/login",
                                "/api/autenticacao/confirmar-2fa",
                                "/api/autenticacao/reenviar-2fa",
                                "/api/autenticacao/confirmar-email", "/api/autenticacao/reenviar-confirmacao",
                                "/api/autenticacao/solicitar-alteracao-senha", "/api/autenticacao/alterar-senha",
                                "/api/enderecos/**", "/api/politicas/ativas",
                                "/politica-privacidade.html", "/termos-aceite.html")
                        .permitAll()
                        .requestMatchers("/api/convites/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/politicas").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/administracao/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/autenticacao/aceitar-termos-novamente")
                        .hasAuthority("TERMOS_PENDENTES")
                        .requestMatchers(HttpMethod.GET, permissoes.getLeituraAluno().toArray(String[]::new))
                        .hasAnyRole("ALUNO", "PROFESSOR", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, permissoes.getLeituraProfessor().toArray(String[]::new))
                        .hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, permissoes.getEscritaAluno().toArray(String[]::new))
                        .hasAnyRole("ALUNO", "PROFESSOR", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/forum/topicos/*")
                        .hasAnyRole("ALUNO", "PROFESSOR", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, permissoes.getEscritaAluno().toArray(String[]::new))
                        .hasAnyRole("ALUNO", "PROFESSOR", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, permissoes.getEscritaAluno().toArray(String[]::new))
                        .hasAnyRole("ALUNO", "PROFESSOR", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, permissoes.getEscritaProfessor().toArray(String[]::new))
                        .hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, permissoes.getEscritaProfessor().toArray(String[]::new))
                        .hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, permissoes.getEscritaProfessor().toArray(String[]::new))
                        .hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                        .requestMatchers(permissoes.getAdministrador().toArray(String[]::new)).hasRole("ADMINISTRADOR")
                        .anyRequest().authenticated())
                .addFilterBefore(tokenJwtFiltro, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder codificadorSenha() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager gerenciadorAutenticacao(AuthenticationConfiguration configuracao) throws Exception {
        return configuracao.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracao = new CorsConfiguration();
        configuracao.setAllowedOrigins(List.of(origensFrontend.split(",")));
        configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracao.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-User-Id"));
        configuracao.setExposedHeaders(List.of("Authorization"));
        configuracao.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource origem = new UrlBasedCorsConfigurationSource();
        origem.registerCorsConfiguration("/**", configuracao);
        return origem;
    }

    private boolean documentoPublico(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "/termos-aceite.html".equals(uri) || "/politica-privacidade.html".equals(uri);
    }
}