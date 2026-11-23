package com.cursovalido.backend.authentication.configuration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class LimiteAcoesFilterTest {
    private final Clock relogio = mock(Clock.class);
    private final LimiteRequisicoesFilter intervalo = new LimiteRequisicoesFilter(10000, 10000, relogio);

    @org.junit.jupiter.api.BeforeEach
    void configurarRelogio() { when(relogio.instant()).thenAnswer(invocacao -> Instant.ofEpochMilli(relogio.millis())); }

    @AfterEach
    void limpar() { SecurityContextHolder.clearContext(); }

    private MockHttpServletResponse enviar(String metodo, String caminho, String corpo) throws Exception {
        var request = new MockHttpServletRequest(metodo, caminho);
        request.setContent(corpo.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var response = new MockHttpServletResponse();
        intervalo.doFilter(request, response, (req, res) -> {});
        return response;
    }

    private void autenticar(long id) {
        var usuario = new UsuarioAutenticado(id, "usuario@example.com", "senha", Perfil.ADMINISTRADOR, true, true, true);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
    }

    @Test
    void administrativoBloqueiaRepeticaoMesmoComOutroCorpoELiberaEmDezSegundos() throws Exception {
        long inicio = Instant.parse("2026-11-23T12:00:00Z").toEpochMilli();
        when(relogio.millis()).thenReturn(inicio);
        autenticar(1);
        assertThat(enviar("POST", "/api/administracao/usuarios/7/suspensoes", "primeiro").getStatus()).isEqualTo(200);
        var repetida = enviar("POST", "/api/administracao/usuarios/7/suspensoes", "outro");
        assertThat(repetida.getStatus()).isEqualTo(429);
        assertThat(repetida.getHeader("Retry-After")).isEqualTo("10");
        assertThat(repetida.getContentAsString()).contains("LIMITE_REQUISICOES_ATINGIDO", "10 segundos");
        when(relogio.millis()).thenReturn(inicio + 9999);
        assertThat(enviar("POST", "/api/administracao/usuarios/7/suspensoes", "outro").getHeader("Retry-After")).isEqualTo("1");
        when(relogio.millis()).thenReturn(inicio + 10000);
        assertThat(enviar("POST", "/api/administracao/usuarios/7/suspensoes", "outro").getStatus()).isEqualTo(200);
    }

    @Test
    void duasEtapasAguardaTrintaSegundos() throws Exception {
        when(relogio.millis()).thenReturn(100000L);
        autenticar(1);
        assertThat(enviar("PUT", "/api/autenticacao/segundo-fator", "true").getStatus()).isEqualTo(200);
        assertThat(enviar("PUT", "/api/autenticacao/segundo-fator", "false").getHeader("Retry-After")).isEqualTo("30");
        when(relogio.millis()).thenReturn(129999L);
        assertThat(enviar("PUT", "/api/autenticacao/segundo-fator", "false").getStatus()).isEqualTo(429);
        when(relogio.millis()).thenReturn(130000L);
        assertThat(enviar("PUT", "/api/autenticacao/segundo-fator", "false").getStatus()).isEqualTo(200);
    }

    @Test
    void usuarioEAcaoIndependentesNaoSeBloqueiam() throws Exception {
        autenticar(1);
        enviar("PUT", "/api/administracao/cursos/1", "a");
        autenticar(2);
        assertThat(enviar("PUT", "/api/administracao/cursos/1", "a").getStatus()).isEqualTo(200);
        assertThat(enviar("PUT", "/api/administracao/cursos/2", "a").getStatus()).isEqualTo(200);
        assertThat(enviar("PUT", "/api/administracao/cursos/1", "b").getStatus()).isEqualTo(429);
    }

    @Test
    void gestaoDeConteudoTambemTemIntervalo() throws Exception {
        autenticar(1);
        for (String caminho : new String[] { "/api/administracao/cursos/1", "/api/administracao/aulas/2", "/api/temas/3", "/api/politicas/privacidade", "/api/forum/topicos/1/fechar" }) {
            assertThat(enviar("PUT", caminho, "a").getStatus()).isEqualTo(200);
            assertThat(enviar("PUT", caminho, "b").getStatus()).isEqualTo(429);
        }
    }

    @Test
    void consultasEOperacoesComunsNaoRecebemIntervaloNovo() throws Exception {
        var professor = new UsuarioAutenticado(3L, "professor@example.com", "senha", Perfil.PROFESSOR, true, true, true);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(professor, null, professor.getAuthorities()));
        for (int i = 0; i < 5; i++) {
            assertThat(enviar("GET", "/api/administracao/usuarios", "").getStatus()).isEqualTo(200);
            assertThat(enviar("POST", "/api/autenticacao/login", "a").getStatus()).isEqualTo(200);
            assertThat(enviar("POST", "/api/aluno/cursos/1/matriculas", "a").getStatus()).isEqualTo(200);
            assertThat(enviar("PUT", "/api/forum/topicos/1", "a").getStatus()).isEqualTo(200);
            assertThat(enviar("PUT", "/api/notificacoes/1/lida", "a").getStatus()).isEqualTo(200);
            assertThat(enviar("GET", "/api/autenticacao/meus-dados", "").getStatus()).isEqualTo(200);
            assertThat(enviar("OPTIONS", "/api/administracao/usuarios", "").getStatus()).isEqualTo(200);
            assertThat(enviar("PUT", "/api/cursos/1", "a").getStatus()).isEqualTo(200);
            assertThat(enviar("PUT", "/api/professor/cursos/1/diagnostico", "a").getStatus()).isEqualTo(200);
            assertThat(enviar("PUT", "/api/temas/1", "a").getStatus()).isEqualTo(200);
            assertThat(enviar("PUT", "/api/forum/topicos/1/fechar", "a").getStatus()).isEqualTo(200);
        }
    }

    @Test
    void exportacaoJsonTemIntervaloPorContaMesmoUsandoHead() throws Exception {
        when(relogio.millis()).thenReturn(100000L);
        autenticar(1);
        assertThat(enviar("GET", "/api/autenticacao/exportar-dados", "").getStatus()).isEqualTo(200);
        assertThat(enviar("HEAD", "/api/autenticacao/exportar-dados", "").getHeader("Retry-After")).isEqualTo("10");
        autenticar(2);
        assertThat(enviar("GET", "/api/autenticacao/exportar-dados", "").getStatus()).isEqualTo(200);
        autenticar(1);
        when(relogio.millis()).thenReturn(109999L);
        assertThat(enviar("GET", "/api/autenticacao/exportar-dados", "").getStatus()).isEqualTo(429);
        when(relogio.millis()).thenReturn(110000L);
        assertThat(enviar("GET", "/api/autenticacao/exportar-dados", "").getStatus()).isEqualTo(200);
    }

    @Test
    void emailESegurancaDaContaTemIntervaloMesmoComCorpoDiferente() throws Exception {
        for (String rota : new String[] { "conta/solicitar-exclusao", "solicitar-alteracao-senha",
                "alterar-senha", "alterar-senha-autenticado" }) {
            String caminho = "/api/autenticacao/" + rota;
            assertThat(enviar("POST", caminho, "primeiro").getStatus()).isEqualTo(200);
            assertThat(enviar("POST", caminho, "outro").getHeader("Retry-After")).isEqualTo("10");
        }
        assertThat(enviar("DELETE", "/api/autenticacao/conta", "000000").getStatus()).isEqualTo(200);
        assertThat(enviar("DELETE", "/api/autenticacao/conta", "111111").getStatus()).isEqualTo(429);
    }


    @Test
    void exportacoesSimultaneasAdmitemUmaUnicaRequisicao() throws Exception {
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(8)) {
            var tarefas = java.util.stream.IntStream.range(0, 20)
                    .<java.util.concurrent.Callable<Integer>>mapToObj(i -> () -> enviar("GET", "/api/autenticacao/exportar-dados", "").getStatus())
                    .toList();
            var respostas = new java.util.ArrayList<Integer>();
            for (var resultado : executor.invokeAll(tarefas)) respostas.add(resultado.get());
            assertThat(respostas).containsOnly(200, 429);
            assertThat(respostas.stream().filter(status -> status == 200).count()).isEqualTo(1);
        }
    }

    @Test
    void httpRepetidoRetorna429SemExecutarControladorNovamente() throws Exception {
        var controlador = new ControladorTeste();
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controlador)
                .addFilters(intervalo).build();
        var requisicao = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/administracao/usuarios/7/suspensoes");
        mvc.perform(requisicao).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        mvc.perform(requisicao)
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Retry-After", "10"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.codigo").value("LIMITE_REQUISICOES_ATINGIDO"));
        assertThat(controlador.chamadas).isEqualTo(1);
    }

    @org.springframework.web.bind.annotation.RestController
    static class ControladorTeste {
        int chamadas;

        @org.springframework.web.bind.annotation.PostMapping("/api/administracao/usuarios/7/suspensoes")
        public String enviar() { chamadas++; return "ok"; }
    }
}
