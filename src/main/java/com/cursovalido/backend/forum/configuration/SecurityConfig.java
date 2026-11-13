package com.cursovalido.backend.forum.configuration;

import com.cursovalido.backend.authentication.configuration.HttpsFilter;
import com.cursovalido.backend.authentication.configuration.LimiteRequisicoesFilter;
import com.cursovalido.backend.authentication.configuration.TokenJwtFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
        private final TokenJwtFilter tokenJwtFiltro;
        @Value("${aplicacao.frontend-origens:http://localhost:3000,http://localhost:5173}")
        private String origensFrontend;
        @Value("${seguranca.https-obrigatorio:false}")
        private boolean httpsObrigatorio;
        @Value("${security.bcrypt.strength:12}")
        private int custoSenha;
        @Value("${seguranca.limite-requisicoes:30}")
        private int limiteRequisicoes;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http.csrf(csrf -> csrf.disable())
                                .headers(headers -> headers
                                                .frameOptions(frame -> frame.disable())
                                                .addHeaderWriter((request, response) -> {
                                                        if (documentoPublico(request)) {
                                                                response.setHeader("Content-Security-Policy",
                                                                                "frame-ancestors " + origensFrontend
                                                                                                .replace(',', ' ')
                                                                                                .trim() + ";");
                                                        } else {
                                                                response.setHeader("X-Frame-Options", "DENY");
                                                        }
                                                }))
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .sessionManagement(sessao -> sessao.sessionCreationPolicy(
                                                SessionCreationPolicy.STATELESS))
                                .exceptionHandling(excecoes -> excecoes
                                                .authenticationEntryPoint((request, response, exception) -> response
                                                                .sendError(401, "Autenticacao necessaria")))
                                .authorizeHttpRequests(authorize -> authorize
                                                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                // Rotas publicas de autenticacao e cadastro.
                                                .requestMatchers(HttpMethod.POST, "/api/suporte/solicitacoes")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/autenticacao/cadastro",
                                                                "/api/autenticacao/login",
                                                                "/api/autenticacao/confirmar-2fa",
                                                                "/api/autenticacao/reenviar-2fa",
                                                                "/api/autenticacao/confirmar-email",
                                                                "/api/autenticacao/reenviar-confirmacao",
                                                                "/api/autenticacao/solicitar-alteracao-senha",
                                                                "/api/autenticacao/alterar-senha",
                                                                "/api/convites/confirmar-email",
                                                                "/api/convites/completar-cadastro")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/politicas/ativas",
                                                                "/api/politicas/privacidade/ativa",
                                                                "/politica-privacidade.html", "/termos-aceite.html")
                                                .permitAll()
                                                // Administracao; os servicos exigem o principal nas operacoes
                                                // exclusivas.
                                                .requestMatchers(HttpMethod.GET, "/api/administracao/dashboard/resumo",
                                                                "/api/administracao/dashboard/matriculas-por-mes",
                                                                "/api/administracao/dashboard/cursos-mais-procurados",
                                                                "/api/administracao/dashboard/usuarios-por-perfil")
                                                .hasRole("ADMINISTRADOR")
                                                .requestMatchers("/api/administracao/dashboard",
                                                                "/api/administracao/dashboard/**")
                                                .denyAll()
                                                .requestMatchers(HttpMethod.POST, "/api/politicas",
                                                                "/api/politicas/privacidade")
                                                .hasRole("ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.GET, "/api/administracao/permissoes",
                                                                "/api/administracao/usuarios",
                                                                "/api/administracao/convites",
                                                                "/api/administracao/convites/pendentes",
                                                                "/api/administracao/convites/auditoria/integridade",
                                                                "/api/administracao/cursos",
                                                                "/api/administracao/cursos/*",
                                                                "/api/administracao/cursos/*/modulos",
                                                                "/api/administracao/modulos/*/aulas",
                                                                "/api/administracao/aulas/*")
                                                .hasRole("ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.POST, "/api/administracao/convites",
                                                                "/api/administracao/convites/*/aprovar",
                                                                "/api/administracao/convites/*/rejeitar",
                                                                "/api/administracao/convites/*/reenviar",
                                                                "/api/administracao/usuarios/*/suspensoes")
                                                .hasRole("ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.DELETE, "/api/administracao/convites/*",
                                                                "/api/administracao/convites/*/cancelar",
                                                                "/api/administracao/usuarios/*/suspensoes/*")
                                                .hasRole("ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.POST,
                                                                "/api/autenticacao/aceitar-termos-novamente")
                                                .hasAuthority("TERMOS_PENDENTES")
                                                .requestMatchers(HttpMethod.POST, "/api/autenticacao/logout")
                                                .authenticated()
                                                // Operacoes da propria conta, para os tres perfis.
                                                .requestMatchers(HttpMethod.GET, "/api/autenticacao/meus-dados",
                                                                "/api/autenticacao/exportar-dados",
                                                                "/api/autenticacao/restricoes")
                                                .hasAnyRole("ALUNO", "PROFESSOR", "ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.POST, "/api/autenticacao/revogar-termos",
                                                                "/api/autenticacao/conta/solicitar-exclusao",
                                                                "/api/autenticacao/alterar-senha-autenticado")
                                                .hasAnyRole("ALUNO", "PROFESSOR", "ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.DELETE, "/api/autenticacao/conta")
                                                .hasAnyRole("ALUNO", "PROFESSOR", "ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.GET, "/api/catalogo/cursos", "/api/catalogo/temas").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/temas").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.PUT, "/api/temas/*").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.DELETE, "/api/temas/*").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.PUT, "/api/administracao/cursos/*").hasRole("ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.DELETE, "/api/administracao/cursos/*").hasRole("ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.PUT, "/api/administracao/aulas/*",
                                                                "/api/administracao/aulas/*/arquivar",
                                                                "/api/administracao/aulas/*/reabrir")
                                                .hasRole("ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.GET, "/api/catalogo/roadmap").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/catalogo/roadmap/questionarios/*").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/catalogo/roadmap/questionarios/*/resultado").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/aluno/roadmap/questionarios/*/resultado").hasRole("ALUNO")
                                                .requestMatchers(HttpMethod.POST, "/api/aluno/roadmap/questionarios/*/resultado").hasRole("ALUNO")
                                                .requestMatchers("/api/administracao/caminhos", "/api/administracao/caminhos/**").hasRole("ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.GET, "/api/aluno/cursos/*/diagnostico", "/api/aluno/cursos/*/modulos/*/questionario", "/api/aluno/cursos/*/plano").hasRole("ALUNO")
                                                .requestMatchers(HttpMethod.POST, "/api/aluno/cursos/*/diagnostico/respostas", "/api/aluno/cursos/*/modulos/*/questionario/respostas").hasRole("ALUNO")
                                                .requestMatchers(HttpMethod.GET, "/api/professor/cursos/*/diagnostico", "/api/professor/cursos/*/modulos/*/questionario").hasRole("PROFESSOR")
                                                .requestMatchers(HttpMethod.PUT, "/api/professor/cursos/*/diagnostico", "/api/professor/cursos/*/modulos/*/questionario").hasRole("PROFESSOR")
                                                .requestMatchers(HttpMethod.GET, "/api/administracao/cursos/*/diagnostico", "/api/administracao/cursos/*/modulos/*/questionario").hasRole("ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.PUT, "/api/administracao/cursos/*/diagnostico", "/api/administracao/cursos/*/modulos/*/questionario").hasRole("ADMINISTRADOR")
                                                // Catalogo, matriculas e progresso pessoais do aluno.
                                                .requestMatchers(HttpMethod.POST, "/api/aluno/cursos/*/matriculas")
                                                .hasRole("ALUNO")
                                                .requestMatchers(HttpMethod.PUT, "/api/aluno/aulas/*/progresso")
                                                .hasRole("ALUNO")
                                                .requestMatchers(HttpMethod.POST, "/api/aluno/aulas/*/conclusao")
                                                .hasRole("ALUNO")
                                                .requestMatchers(HttpMethod.GET, "/api/aluno/cursos",
                                                                "/api/aluno/cursos/*",
                                                                "/api/aluno/matriculas", "/api/aluno/cursos/*/modulos",
                                                                "/api/aluno/modulos/*/aulas", "/api/aluno/aulas/*",
                                                                "/api/aluno/aulas/*/progresso")
                                                .hasRole("ALUNO")
                                                .requestMatchers("/api/aluno", "/api/aluno/**").denyAll()
                                                // As rotas de gestao continuam exclusivas do professor.
                                                .requestMatchers(HttpMethod.GET, "/api/cursos", "/api/cursos/*",
                                                                "/api/cursos/*/modulos",
                                                                "/api/modulos/*", "/api/modulos/*/aulas",
                                                                "/api/aulas/*")
                                                .hasRole("PROFESSOR")
                                                .requestMatchers(HttpMethod.POST, "/api/cursos",
                                                                "/api/cursos/*/modulos",
                                                                "/api/modulos/*/aulas")
                                                .hasRole("PROFESSOR")
                                                .requestMatchers(HttpMethod.PUT, "/api/cursos/*", "/api/modulos/*",
                                                                "/api/aulas/*")
                                                .hasRole("PROFESSOR")
                                                .requestMatchers(HttpMethod.DELETE, "/api/cursos/*", "/api/modulos/*",
                                                                "/api/aulas/*")
                                                .hasRole("PROFESSOR")
                                                // Noticias: leitura publica e gestao exclusiva do administrador.
                                                .requestMatchers(HttpMethod.GET, "/api/noticias", "/api/noticias/*")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/noticias")
                                                .hasRole("ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.PUT, "/api/noticias/*")
                                                .hasRole("ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.DELETE, "/api/noticias/*")
                                                .hasRole("ADMINISTRADOR")
                                                // Forum: autoria e moderacao sao verificadas pelos servicos.
                                                .requestMatchers(HttpMethod.GET, "/api/forum/topicos",
                                                                "/api/forum/topicos/*/comentarios")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/forum/users")
                                                .hasAnyRole("ALUNO", "PROFESSOR", "ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.POST, "/api/forum/topicos",
                                                                "/api/forum/topicos/*/comentarios")
                                                .hasAnyRole("ALUNO", "PROFESSOR", "ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.PUT, "/api/forum/topicos/*",
                                                                "/api/forum/topicos/*/arquivar",
                                                                "/api/forum/topicos/*/fechar",
                                                                "/api/forum/comentarios/*")
                                                .hasAnyRole("ALUNO", "PROFESSOR", "ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.DELETE, "/api/forum/comentarios/*",
                                                                "/api/forum/topicos/*/comentarios/*")
                                                .hasAnyRole("ALUNO", "PROFESSOR", "ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.DELETE, "/api/forum/topicos/*")
                                                .hasRole("ADMINISTRADOR")
                                                .anyRequest().denyAll())
                                .addFilterBefore(tokenJwtFiltro, UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(new HttpsFilter(httpsObrigatorio),
                                                TokenJwtFilter.class)
                                .addFilterBefore(new LimiteRequisicoesFilter(limiteRequisicoes),
                                                TokenJwtFilter.class);
                return http.build();
        }

        @Bean
        public PasswordEncoder codificadorSenha() {
                if (custoSenha < 10 || custoSenha > 16)
                        throw new IllegalStateException("BCRYPT_STRENGTH deve estar entre 10 e 16");
                return new BCryptPasswordEncoder(custoSenha);
        }

        @Bean
        public AuthenticationManager gerenciadorAutenticacao(AuthenticationConfiguration configuracao)
                        throws Exception {
                return configuracao.getAuthenticationManager();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuracao = new CorsConfiguration();
                configuracao.setAllowedOrigins(java.util.Arrays.stream(origensFrontend.split(","))
                                .map(String::trim).filter(origem -> !origem.isEmpty()).distinct().toList());
                configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"));
                configuracao.setAllowedHeaders(List.of("Content-Type", "Authorization", "Accept", "Cache-Control", "X-User-Id"));
                configuracao.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
                configuracao.setMaxAge(3600L);
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
