package com.cursovalido.backend.authentication;

import com.cursovalido.backend.authentication.dto.PoliticaRequest;
import com.cursovalido.backend.authentication.entity.Politica;
import com.cursovalido.backend.authentication.entity.PoliticaPrivacidade;
import com.cursovalido.backend.authentication.entity.VersaoDocumento;
import com.cursovalido.backend.authentication.repository.PoliticaRepository;
import com.cursovalido.backend.authentication.repository.PrivacidadeRepository;
import com.cursovalido.backend.authentication.repository.VersaoDocumentoRepository;
import com.cursovalido.backend.authentication.service.EmailService;
import com.cursovalido.backend.authentication.service.PoliticaService;
import com.cursovalido.backend.authentication.service.PrivacidadeService;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:historico;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "seguranca.https-obrigatorio=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HistoricoDocumentosIntegracaoTest {
    @Autowired VersaoDocumentoRepository versoes;
    @BeforeEach
    void prepararVersoes() {
        versoes.saveAndFlush(new VersaoDocumento("TERMOS", 0));
        versoes.saveAndFlush(new VersaoDocumento("PRIVACIDADE", 0));
    }

    @Autowired MockMvc mvc;
    @Autowired PoliticaRepository termos;
    @Autowired PrivacidadeRepository privacidade;
    @Autowired PoliticaService servicoTermos;
    @Autowired PrivacidadeService servicoPrivacidade;
    @MockitoBean PrivilegiosAdministrativosService privilegios;
    @MockitoBean EmailService emails;

    @Test
    void publicaSemApagarTermosAnterioresEPermiteConsultarSemLogin() throws Exception {
        var anterior = termos.saveAndFlush(new Politica("Termos anteriores", "historico-termos", "Texto original dos termos"));
        var publicada = servicoTermos.publicar(new PoliticaRequest("Termos novos", "Texto atualizado"), 1L);
        termos.flush();
        assertThat(termos.findById(anterior.getId()).orElseThrow().isAtiva()).isFalse();
        assertThat(termos.findByVersao(publicada.versao())).isPresent();
        assertThat(servicoTermos.listarHistorico()).anySatisfy(documento -> {
            assertThat(documento.versao()).isEqualTo(anterior.getVersao());
            assertThat(documento.conteudo()).isEqualTo("Texto original dos termos");
            assertThat(documento.dataPublicacao()).isEqualTo(anterior.getDataPublicacao());
        });
        mvc.perform(get("/api/politicas/historico")).andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.versao == 'historico-termos')].conteudo").value("Texto original dos termos"));
        mvc.perform(get("/api/politicas/historico").header("Authorization", "Bearer expirado"))
            .andExpect(status().isOk());
    }

    @Test
    void publicaSemApagarPrivacidadeAnteriorEHistoricoNaoIncluiVersaoAtiva() throws Exception {
        var anterior = privacidade.saveAndFlush(new PoliticaPrivacidade("Privacidade anterior", "historico-privacidade", "Texto original da privacidade"));
        var publicada = servicoPrivacidade.publicar(new PoliticaRequest("Privacidade nova", "Texto atualizado"), 1L);
        privacidade.flush();
        assertThat(privacidade.findById(anterior.getId()).orElseThrow().isAtiva()).isFalse();
        assertThat(servicoPrivacidade.listarHistorico()).noneMatch(documento -> documento.versao().equals(publicada.versao()));
        mvc.perform(get("/api/politicas/privacidade/historico"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.versao == 'historico-privacidade')].conteudo").value("Texto original da privacidade"));
    }
}
