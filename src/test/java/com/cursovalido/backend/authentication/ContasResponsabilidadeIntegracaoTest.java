package com.cursovalido.backend.authentication;

import com.cursovalido.backend.authentication.validation.email.ValidacaoEmailService;

import com.cursovalido.backend.authentication.entity.*;
import com.cursovalido.backend.authentication.repository.*;
import com.cursovalido.backend.authentication.service.*;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContasResponsabilidadeIntegracaoTest {
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired ContaUsuarioService contas;
    @Autowired TokenEmailService tokensEmail;
    @Autowired TokenEmailRepository registrosTokens;
    @Autowired TokenJwtService tokens;
    @Autowired SessaoRepository sessoes;
    @Autowired ResponsabilidadeService responsabilidade;
    @Autowired AceiteResponsabilidadeRepository aceites;
    @Autowired UsuarioDetalhesService detalhes;
    @Autowired ConviteAcessoRepository convites;
    @Autowired LogAuditoriaRepository logs;
    @Autowired LogAuditoriaService auditoria;
    @Autowired TermosUsuarioService termos;
    @Autowired PoliticaRepository politicas;
    @MockitoBean EmailService emails;
    @MockitoBean com.cursovalido.backend.forum.service.UsuarioForumService usuariosForum;
    @MockitoBean ValidacaoEmailService validacaoEmail;
    @Autowired ConviteAcessoService servicoConvites;

    @ParameterizedTest
    @EnumSource(value = Perfil.class, names = {"PROFESSOR", "ADMINISTRADOR"})
    void exclusaoPreservaAceitesLogsConvitesELiberaEmail(Perfil perfil) {
        Usuario usuario = criar(perfil);
        String email = usuario.getEmail();
        responsabilidade.registrar(usuario, true, "1.0", "127.0.0.1");
        ConviteAcesso convite = new ConviteAcesso("destino@example.com", Perfil.PROFESSOR,
                UUID.randomUUID().toString(), LocalDateTime.now().plusDays(1));
        convite.setCriadoPor(usuario.getId());
        convites.saveAndFlush(convite);
        String jwt = tokens.gerar(usuario);
        var logsAntes = logs.findByUsuarioIdOrderByDataHoraAsc(usuario.getId());
        contas.excluirConta(usuario.getId(), tokensEmail.criarExclusao(usuario));
        var historico = usuarios.findById(usuario.getId()).orElseThrow();
        assertThat(historico.getExcluidoEm()).isNotNull();
        assertThat(historico.isAtivo()).isFalse();
        assertThat(historico.isEmailConfirmado()).isFalse();
        assertThat(historico.getSenha()).isEmpty();
        assertThat(historico.getDataNascimento()).isNull();
        assertThat(usuarios.existsByEmail(email)).isFalse();
        assertThat(aceites.existsByUsuarioIdAndPerfilAndVersao(usuario.getId(), perfil, "1.0")).isTrue();
        assertThat(logs.findByUsuarioIdOrderByDataHoraAsc(usuario.getId())).containsAll(logsAntes);
        assertThat(auditoria.verificarIntegridade()).isTrue();
        assertThat(convites.findById(convite.getId()).orElseThrow().getCriadoPor()).isEqualTo(usuario.getId());
        assertThatThrownBy(() -> tokens.extrairEmail(jwt)).isInstanceOf(IllegalArgumentException.class);
        assertThat(registrosTokens.findAll()).noneMatch(t -> t.getUsuario().getId().equals(usuario.getId()));
        Usuario novo = criar(perfil);
        novo.setEmail(email);
        usuarios.saveAndFlush(novo);
        assertThat(usuarios.findByEmail(email).orElseThrow().getId()).isNotEqualTo(usuario.getId());
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class, names = {"PROFESSOR", "ADMINISTRADOR"})
    void primeiroAcessoExigeAceiteEspecificoERegistraVersaoIpEData(Perfil perfil) {
        var usuario = criar(perfil);
        var antes = (UsuarioAutenticado) detalhes.loadUserByUsername(usuario.getEmail());
        assertThat(antes.isTermosAceitos()).isFalse();
        assertThatThrownBy(() -> responsabilidade.registrar(usuario, false, "1.0", "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> responsabilidade.registrar(usuario, true, "antiga", "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> responsabilidade.registrar(usuario, true, "1.0", null))
                .isInstanceOf(IllegalArgumentException.class);
        responsabilidade.registrar(usuario, true, "1.0", "2001:db8::1");
        assertThat(((UsuarioAutenticado) detalhes.loadUserByUsername(usuario.getEmail())).isTermosAceitos()).isTrue();
        var aceite = aceites.findAll().stream().filter(a -> a.getUsuarioId().equals(usuario.getId())).findFirst().orElseThrow();
        assertThat(aceite.getVersao()).isEqualTo("1.0");
        assertThat(aceite.getPerfil()).isEqualTo(perfil);
        assertThat(aceite.getEnderecoIp()).isEqualTo("2001:db8::1");
        assertThat(aceite.getAceitoEm()).isNotNull();
        usuario.setPerfil(perfil == Perfil.PROFESSOR ? Perfil.ADMINISTRADOR : Perfil.PROFESSOR);
        usuarios.saveAndFlush(usuario);
        assertThat(responsabilidade.atualizado(usuario)).isFalse();
    }

    @Test
    void aceiteGeralNaoSubstituiResponsabilidadeENovoAceiteEhTransacional() {
        var usuario = criar(Perfil.ADMINISTRADOR);
        politicas.save(new Politica("Termos", "contas-v1", "Termos gerais"));
        assertThatThrownBy(() -> termos.aceitarTermosNovamente(usuario.getId(), "contas-v1", false, "1.0", "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(responsabilidade.atualizado(usuario)).isFalse();
        termos.aceitarTermosNovamente(usuario.getId(), "contas-v1", true, "1.0", "127.0.0.1");
        assertThat(responsabilidade.atualizado(usuario)).isTrue();
        assertThat(usuario.getVersaoTermos()).isEqualTo("contas-v1");
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class, names = {"PROFESSOR", "ADMINISTRADOR"})
    void conviteExigeAceiteDoPerfilAntesDeCriarUsuario(Perfil perfil) throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String token = UUID.randomUUID().toString();
        String hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        var convite = new ConviteAcesso(email, perfil, hash, LocalDateTime.now().plusDays(1));
        convite.setEmailConfirmado(true);
        convites.saveAndFlush(convite);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/convites/consultar")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.email").value(email))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.perfil").value(perfil.name()));
        politicas.save(new Politica("Termos", "convite-responsabilidade", "Termos gerais"));
        var semAceite = new com.cursovalido.backend.authentication.dto.CadastroUsuarioRequest(
                "Convidado", email, "SenhaTeste123!", LocalDate.of(1990, 1, 1),
                true, true, "convite-responsabilidade");
        assertThatThrownBy(() -> servicoConvites.completar(token, semAceite, "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(usuarios.existsByEmail(email)).isFalse();
        var comAceite = new com.cursovalido.backend.authentication.dto.CadastroUsuarioRequest(
                "Convidado", email, "SenhaTeste123!", LocalDate.of(1990, 1, 1),
                true, true, "convite-responsabilidade", true, "1.0");
        servicoConvites.completar(token, comAceite, "127.0.0.1");
        var usuario = usuarios.findByEmail(email).orElseThrow();
        assertThat(usuario.getPerfil()).isEqualTo(perfil);
        assertThat(responsabilidade.atualizado(usuario)).isTrue();
        assertThat(convite.getStatus()).isEqualTo(StatusConvite.CONCLUIDO);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/convites/consultar")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void consultaDoConviteRecusaTokenInvalidoEExpirado() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/convites/consultar")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"token\":\"invalido\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        String token = "expirado";
        String hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        convites.saveAndFlush(new ConviteAcesso("expirado@example.com", Perfil.PROFESSOR, hash, LocalDateTime.now().minusDays(1)));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/convites/consultar")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"token\":\"expirado\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
    }

    private Usuario criar(Perfil perfil) {
        var usuario = new Usuario();
        usuario.setNomeCompleto("Pessoa Teste");
        usuario.setEmail(UUID.randomUUID() + "@example.com");
        usuario.setMatricula(UUID.randomUUID().toString());
        usuario.setSenha("hash");
        usuario.setDataNascimento(LocalDate.of(1990, 1, 1));
        usuario.setDeclarouMaiorIdade(true);
        usuario.setAceitouTermosLgpd(true);
        usuario.setVersaoTermos("1.0");
        usuario.setDataHoraAceite(LocalDateTime.now());
        usuario.setEmailConfirmado(true);
        usuario.setPerfil(perfil);
        return usuarios.saveAndFlush(usuario);
    }
}
