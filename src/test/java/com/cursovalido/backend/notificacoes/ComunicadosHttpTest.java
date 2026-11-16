package com.cursovalido.backend.notificacoes;

import com.cursovalido.backend.authentication.entity.*;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.service.PoliticaService;
import com.cursovalido.backend.authentication.service.ResponsabilidadeService;
import com.cursovalido.backend.authentication.service.TokenJwtService;
import com.cursovalido.backend.forum.configuration.FlywayConfig;
import com.cursovalido.backend.forum.support.PostgresTeste;
import com.cursovalido.backend.notificacoes.dto.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.jpa.hibernate.ddl-auto=validate")
@ActiveProfiles("test")
@ExtendWith(PostgresTeste.CicloDeVida.class)
@Import(ComunicadosHttpTest.MigracaoTeste.class)
class ComunicadosHttpTest {
        @LocalServerPort
        int porta;
        @Autowired
        UsuarioRepository usuarios;
        @Autowired
        TokenJwtService tokens;
        @Autowired
        PoliticaService politicas;
        @Autowired
        ResponsabilidadeService responsabilidade;
        @Autowired
        JdbcTemplate jdbc;
        RestClient cliente;
        String admin;
        String aluno;
        String professor;
        Long idAluno;

        @DynamicPropertySource
        static void banco(DynamicPropertyRegistry registry) {
                PostgresTeste.configurar(registry, "comunicados_http");
        }

        @BeforeEach
        void preparar() {
                jdbc.update("DELETE FROM tb_comunicados");
                cliente = RestClient.builder().baseUrl("http://127.0.0.1:" + porta)
                                .defaultStatusHandler(status -> true, (request, response) -> {
                                }).build();
                admin = tokens.gerar(criarUsuario(Perfil.ADMINISTRADOR));
                var conta = criarUsuario(Perfil.ALUNO);
                idAluno = conta.getId();
                aluno = tokens.gerar(conta);
                professor = tokens.gerar(criarUsuario(Perfil.PROFESSOR));
        }

        @Test
        void publicaConteudoNoLimiteEPersisteDepoisDaConsulta() {
                var dados = new ComunicadoRequest(" Aviso aos alunos ", "a".repeat(5000));
                var resposta = enviar(HttpMethod.POST, "/api/administracao/comunicados", admin, dados,
                                ComunicadoResponse.class);
                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(resposta.getBody().titulo()).isEqualTo("Aviso aos alunos");
                var lista = enviar(HttpMethod.GET, "/api/aluno/notificacoes", aluno, null,
                                PaginaComunicadosResponse.class).getBody();
                assertThat(lista.totalElementos()).isEqualTo(1);
                assertThat(lista.naoLidas()).isEqualTo(1);
                assertThat(lista.itens().getFirst().conteudo()).hasSize(5000);
                assertThat(lista.itens().getFirst().lida()).isFalse();
                assertThat(enviar(HttpMethod.GET, "/api/administracao/comunicados", admin, null,
                                PaginaComunicadosResponse.class).getBody().totalElementos()).isEqualTo(1);
        }

        @Test
        void validaConteudoVazioLimiteETituloNoServidor() {
                for (var dados : new ComunicadoRequest[] {
                                new ComunicadoRequest("Aviso", "x".repeat(5001)),
                                new ComunicadoRequest(" ", "Texto"), new ComunicadoRequest("Aviso", " "),
                                new ComunicadoRequest("x".repeat(256), "Texto") }) {
                        assertThat(enviar(HttpMethod.POST, "/api/administracao/comunicados", admin, dados, String.class)
                                        .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                }
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_comunicados", Long.class)).isZero();
        }

        @Test
        void exigeAutenticacaoEPerfilCorretoEmCadaOperacao() {
                var dados = new ComunicadoRequest("Aviso", "Texto");
                for (String token : new String[] { aluno, professor }) {
                        assertThat(enviar(HttpMethod.POST, "/api/administracao/comunicados", token, dados, String.class)
                                        .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                        assertThat(enviar(HttpMethod.GET, "/api/administracao/comunicados", token, null, String.class)
                                        .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                }
                var comunicado = publicar("Aviso");
                for (String token : new String[] { aluno, professor, admin }) {
                        var lista = enviar(HttpMethod.GET, "/api/notificacoes", token, null,
                                        PaginaComunicadosResponse.class);
                        assertThat(lista.getStatusCode()).isEqualTo(HttpStatus.OK);
                        assertThat(lista.getBody().naoLidas()).isEqualTo(1);
                        assertThat(enviar(HttpMethod.PUT, "/api/notificacoes/" + comunicado.id() + "/lida", token, null,
                                        String.class).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
                        assertThat(enviar(HttpMethod.GET, "/api/notificacoes/nao-lidas", token, null, Map.class)
                                        .getBody().get("quantidade")).isEqualTo(0);
                }
                assertThat(enviar(HttpMethod.POST, "/api/administracao/comunicados", null, dados, String.class)
                                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(enviar(HttpMethod.GET, "/api/notificacoes", null, null, String.class)
                                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(enviar(HttpMethod.PUT, "/api/notificacoes/" + comunicado.id() + "/lida", null, null,
                                String.class)
                                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void leituraIdempotentePertenceAoAlunoAutenticado() {
                var comunicado = publicar("Aviso");
                var outraConta = criarUsuario(Perfil.ALUNO);
                String outroAluno = tokens.gerar(outraConta);
                String rota = "/api/aluno/notificacoes/" + comunicado.id() + "/lida?usuarioId=" + outraConta.getId();
                for (int i = 0; i < 2; i++) {
                        assertThat(enviar(HttpMethod.PUT, rota, aluno, null, String.class).getStatusCode())
                                        .isEqualTo(HttpStatus.NO_CONTENT);
                }
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_comunicados_leituras WHERE usuario_id = ?",
                                Long.class, idAluno)).isEqualTo(1);
                assertThat(enviar(HttpMethod.GET, "/api/aluno/notificacoes", aluno, null,
                                PaginaComunicadosResponse.class).getBody().itens().getFirst().lida()).isTrue();
                assertThat(enviar(HttpMethod.GET, "/api/aluno/notificacoes", outroAluno, null,
                                PaginaComunicadosResponse.class).getBody().naoLidas()).isEqualTo(1);
                assertThat(enviar(HttpMethod.GET, "/api/aluno/notificacoes/nao-lidas", aluno, null, Map.class)
                                .getBody().get("quantidade")).isEqualTo(0);
        }

        @Test
        void ordenaPorRecenciaEPaginaSemPerderComunicados() {
                for (int i = 1; i <= 12; i++)
                        publicar("Aviso " + i);
                var primeira = enviar(HttpMethod.GET, "/api/aluno/notificacoes?page=0&size=10", aluno, null,
                                PaginaComunicadosResponse.class).getBody();
                var segunda = enviar(HttpMethod.GET, "/api/aluno/notificacoes?page=1&size=10", aluno, null,
                                PaginaComunicadosResponse.class).getBody();
                assertThat(primeira.itens()).hasSize(10);
                assertThat(primeira.totalPaginas()).isEqualTo(2);
                assertThat(primeira.itens().getFirst().titulo()).isEqualTo("Aviso 12");
                assertThat(segunda.itens()).hasSize(2);
                assertThat(segunda.itens().getLast().titulo()).isEqualTo("Aviso 1");
        }

        @Test
        void rejeitaPaginacaoInvalidaELeituraInexistente() {
                for (String parametros : new String[] { "page=-1", "size=0", "size=51", "page=abc" }) {
                        assertThat(enviar(HttpMethod.GET, "/api/aluno/notificacoes?" + parametros, aluno, null,
                                        String.class)
                                        .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                }
                assertThat(enviar(HttpMethod.PUT, "/api/aluno/notificacoes/999999/lida", aluno, null, String.class)
                                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void administradorEditaMantendoLeiturasEDataDaPublicacao() {
                var publicado = publicar("Original");
                var antes = enviar(HttpMethod.GET, "/api/aluno/notificacoes", aluno, null,
                                PaginaComunicadosResponse.class).getBody().itens().getFirst();
                enviar(HttpMethod.PUT, "/api/aluno/notificacoes/" + publicado.id() + "/lida", aluno, null,
                                String.class);
                var outroAdmin = tokens.gerar(criarUsuario(Perfil.ADMINISTRADOR));
                var atualizado = enviar(HttpMethod.PUT, "/api/administracao/comunicados/" + publicado.id(), outroAdmin,
                                new ComunicadoRequest(" Editado ", "a".repeat(5000)), ComunicadoResponse.class);
                assertThat(atualizado.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(atualizado.getBody().titulo()).isEqualTo("Editado");
                var depois = enviar(HttpMethod.GET, "/api/aluno/notificacoes", aluno, null,
                                PaginaComunicadosResponse.class).getBody();
                assertThat(depois.itens().getFirst().conteudo()).hasSize(5000);
                assertThat(depois.itens().getFirst().criadoEm()).isEqualTo(antes.criadoEm());
                assertThat(depois.itens().getFirst().lida()).isTrue();
                assertThat(depois.naoLidas()).isZero();
        }

        @Test
        void edicaoEExclusaoExigemAdministradorAtivo() {
                var publicado = publicar("Original");
                String rota = "/api/administracao/comunicados/" + publicado.id();
                var dados = new ComunicadoRequest("Editado", "Novo conteúdo");
                var inativo = criarUsuario(Perfil.ADMINISTRADOR);
                String tokenInativo = tokens.gerar(inativo);
                inativo.setAtivo(false);
                usuarios.saveAndFlush(inativo);
                for (String token : new String[] { aluno, professor, tokenInativo }) {
                        assertThat(enviar(HttpMethod.PUT, rota, token, dados, String.class).getStatusCode())
                                        .isEqualTo(HttpStatus.FORBIDDEN);
                        assertThat(enviar(HttpMethod.DELETE, rota, token, null, String.class).getStatusCode())
                                        .isEqualTo(HttpStatus.FORBIDDEN);
                }
                assertThat(enviar(HttpMethod.PUT, rota, null, dados, String.class).getStatusCode())
                                .isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(enviar(HttpMethod.DELETE, rota, null, null, String.class).getStatusCode())
                                .isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(jdbc.queryForObject("SELECT titulo FROM tb_comunicados WHERE id = ?", String.class,
                                publicado.id())).isEqualTo("Original");
        }

        @Test
        void edicaoInvalidaPreservaDadosEIdsInexistentesRetornam404() {
                var publicado = publicar("Original");
                String rota = "/api/administracao/comunicados/" + publicado.id();
                for (var dados : new ComunicadoRequest[] {
                                new ComunicadoRequest("Aviso", "a".repeat(5001)), new ComunicadoRequest(" ", "Texto"),
                                new ComunicadoRequest("Aviso", " "),
                                new ComunicadoRequest("a".repeat(256), "Texto") }) {
                        assertThat(enviar(HttpMethod.PUT, rota, admin, dados, String.class).getStatusCode())
                                        .isEqualTo(HttpStatus.BAD_REQUEST);
                }
                assertThat(jdbc.queryForObject("SELECT titulo FROM tb_comunicados WHERE id = ?", String.class,
                                publicado.id())).isEqualTo("Original");
                assertThat(enviar(HttpMethod.PUT, "/api/administracao/comunicados/999999", admin,
                                new ComunicadoRequest("Aviso", "Texto"), String.class).getStatusCode())
                                .isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(enviar(HttpMethod.DELETE, "/api/administracao/comunicados/999999", admin, null,
                                String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void exclusaoRemoveComunicadoLeiturasEAtualizaContagem() {
                var lido = publicar("Lido");
                var naoLido = publicar("Não lido");
                enviar(HttpMethod.PUT, "/api/aluno/notificacoes/" + lido.id() + "/lida", aluno, null, String.class);
                assertThat(enviar(HttpMethod.DELETE, "/api/administracao/comunicados/" + naoLido.id(), admin, null,
                                String.class).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
                var lista = enviar(HttpMethod.GET, "/api/aluno/notificacoes", aluno, null,
                                PaginaComunicadosResponse.class).getBody();
                assertThat(lista.totalElementos()).isEqualTo(1);
                assertThat(lista.naoLidas()).isZero();
                assertThat(enviar(HttpMethod.DELETE, "/api/administracao/comunicados/" + lido.id(), admin, null,
                                String.class).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tb_comunicados_leituras", Long.class)).isZero();
                assertThat(enviar(HttpMethod.PUT, "/api/aluno/notificacoes/" + lido.id() + "/lida", aluno, null,
                                String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(enviar(HttpMethod.GET, "/api/aluno/notificacoes", aluno, null,
                                PaginaComunicadosResponse.class).getBody().itens()).isEmpty();
        }

        private ComunicadoResponse publicar(String titulo) {
                var resposta = enviar(HttpMethod.POST, "/api/administracao/comunicados", admin,
                                new ComunicadoRequest(titulo, "Conteúdo do comunicado"), ComunicadoResponse.class);
                assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                return resposta.getBody();
        }

        private <T> ResponseEntity<T> enviar(HttpMethod metodo, String rota, String token, Object dados,
                        Class<T> tipo) {
                var pedido = cliente.method(metodo).uri(rota);
                if (token != null)
                        pedido.headers(h -> h.setBearerAuth(token));
                if (dados != null)
                        pedido.contentType(MediaType.APPLICATION_JSON).body(dados);
                return pedido.retrieve().toEntity(tipo);
        }

        private Usuario criarUsuario(Perfil perfil) {
                var usuario = new Usuario();
                usuario.setNomeCompleto("Teste comunicado");
                usuario.setEmail(UUID.randomUUID() + "@comunicados.example.com");
                usuario.setSenha("hash-de-teste");
                usuario.setMatricula(UUID.randomUUID().toString());
                usuario.setDataNascimento(LocalDate.of(2000, 1, 1));
                usuario.setDeclarouMaiorIdade(true);
                usuario.setAceitouTermosLgpd(true);
                usuario.setDataHoraAceite(LocalDateTime.now());
                usuario.setVersaoTermos(politicas.listarAtivas().getFirst().versao());
                usuario.setPerfil(perfil);
                usuario.setEmailConfirmado(true);
                usuarios.saveAndFlush(usuario);
                if (perfil != Perfil.ALUNO)
                        responsabilidade.registrar(usuario, true, "1.0", "127.0.0.1");
                return usuario;
        }

        @TestConfiguration
        static class MigracaoTeste {
                @Bean(initMethod = "migrate")
                Flyway flyway(DataSource dataSource) {
                        return FlywayConfig.configuracao().dataSource(dataSource).load();
                }

                @Bean
                static BeanFactoryPostProcessor migrarAntesDoJpa() {
                        return beanFactory -> beanFactory.getBeanDefinition("entityManagerFactory")
                                        .setDependsOn("flyway");
                }
        }
}
