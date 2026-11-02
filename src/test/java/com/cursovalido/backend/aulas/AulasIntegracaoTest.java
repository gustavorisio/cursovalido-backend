package com.cursovalido.backend.aulas;

import com.cursovalido.backend.aulas.repository.*;
import com.cursovalido.backend.aulas.service.AlunoAulasService;
import com.cursovalido.backend.aulas.service.YoutubeClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.cursovalido.backend.authentication.entity.*;
import com.cursovalido.backend.authentication.repository.UsuarioRepositorio;
import com.cursovalido.backend.authentication.security.UsuarioAutenticado;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AulasIntegracaoTest {
    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepositorio usuarios;
    @Autowired private CursoRepository cursos;
    @Autowired private ModuloRepository modulos;
    @Autowired private AulaRepository aulas;
    @Autowired private MatriculaRepository matriculas;
    @Autowired private AlunoAulasService alunoAulas;
    @Autowired private ProgressoAulaRepository progressos;
    @MockitoBean private YoutubeClient youtubeClient;
    private Usuario professor;

    @BeforeEach
    void preparar() {
        professor = criarProfessor("professor");
    }

    @Test
    void criarEditarConsultarEInativarAula() throws Exception {
        long idModulo = prepararModulo();
        long id = criarAula(idModulo);
        assertThat(aulas.findById(id).orElseThrow().getYoutubeVideoId()).isEqualTo("M7lc1UVf-VE");
        mvc.perform(put("/api/aulas/" + id).with(comoProfessor(professor))
                .contentType(MediaType.APPLICATION_JSON).content(dadosAula("Atualizada", "https://youtu.be/abcdefghijk")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.youtubeVideoId").value("abcdefghijk"));
        mvc.perform(get("/api/modulos/" + idModulo + "/aulas").with(comoProfessor(professor)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].titulo").value("Atualizada"));
        mvc.perform(delete("/api/aulas/" + id).with(comoProfessor(professor)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/aulas/" + id).with(comoProfessor(professor)))
                .andExpect(status().isNotFound());
        assertThat(aulas.findById(id).orElseThrow().isAtivo()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = { 400, 502, 503 })
    void falhaDoYoutubeNaoCriaNemAlteraAula(int status) throws Exception {
        long idModulo = prepararModulo();
        long id = criarAula(idModulo);
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.valueOf(status), "Falha controlada"))
                .when(youtubeClient).validarVideo("abcdefghijk");
        mvc.perform(post("/api/modulos/" + idModulo + "/aulas").with(comoProfessor(professor))
                .contentType(MediaType.APPLICATION_JSON).content(dadosAula("Nova", "https://youtu.be/abcdefghijk")))
                .andExpect(status().is(status));
        mvc.perform(put("/api/aulas/" + id).with(comoProfessor(professor))
                .contentType(MediaType.APPLICATION_JSON).content(dadosAula("Alterada", "https://youtu.be/abcdefghijk")))
                .andExpect(status().is(status));
        var existentes = aulas.findByIdModuloAndAtivoTrueOrderByOrdemAscIdAsc(idModulo);
        assertThat(existentes).hasSize(1);
        assertThat(existentes.getFirst().getTitulo()).isEqualTo("Java");
        assertThat(existentes.getFirst().getYoutubeVideoId()).isEqualTo("M7lc1UVf-VE");
    }

    @Test
    void impedirAcessoDeOutroProfessor() throws Exception {
        long id = criarAula(prepararModulo());
        Usuario outro = criarProfessor("outro");
        mvc.perform(get("/api/aulas/" + id).with(comoProfessor(outro))).andExpect(status().isForbidden());
        mvc.perform(put("/api/aulas/" + id).with(comoProfessor(outro)).contentType(MediaType.APPLICATION_JSON)
                .content(dadosAula("Indevida", "https://youtu.be/M7lc1UVf-VE"))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/aulas/" + id).with(comoProfessor(outro))).andExpect(status().isForbidden());
        assertThat(aulas.findById(id).orElseThrow().getTitulo()).isEqualTo("Java");
    }

    @Test
    void inativarCursoBloqueiaAulasFilhas() throws Exception {
        long id = criarAula(prepararModulo());
        long idCurso = cursos.findByIdProfessorAndAtivoTrueOrderByIdDesc(professor.getId()).getFirst().getId();
        mvc.perform(delete("/api/cursos/" + idCurso).with(comoProfessor(professor))).andExpect(status().isNoContent());
        mvc.perform(get("/api/aulas/" + id).with(comoProfessor(professor))).andExpect(status().isNotFound());
    }

    @Test
    void rejeitarLinkEEntradaInvalidosSemSalvar() throws Exception {
        long idModulo = prepararModulo();
        mvc.perform(post("/api/modulos/" + idModulo + "/aulas").with(comoProfessor(professor))
                .contentType(MediaType.APPLICATION_JSON).content(dadosAula("Java", "https://evil.com/M7lc1UVf-VE")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/modulos/" + idModulo + "/aulas").with(comoProfessor(professor))
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"titulo":" ","descricao":"Teste","ordem":0,"youtubeUrl":"https://youtu.be/M7lc1UVf-VE"}
                        """))
                .andExpect(status().isBadRequest());
        assertThat(aulas.findByIdModuloAndAtivoTrueOrderByOrdemAscIdAsc(idModulo)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "ALUNO", "ADMINISTRADOR", "TERMOS_PENDENTES" })
    void impedirRotasParaOutrosPerfis(String perfil) throws Exception {
        var principal = new UsuarioAutenticado(professor.getId(), "teste@example.com", "senha", perfil,
                true, !perfil.equals("TERMOS_PENDENTES"), true);
        var acesso = authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        for (String rota : new String[] { "/api/cursos", "/api/cursos/1/modulos", "/api/modulos/1/aulas", "/api/aulas/1" }) {
            mvc.perform(get(rota).with(acesso)).andExpect(status().isForbidden());
            mvc.perform(post(rota).with(acesso).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
            mvc.perform(put(rota).with(acesso).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
            mvc.perform(delete(rota).with(acesso)).andExpect(status().isForbidden());
        }
    }

    @Test
    void exigirAutenticacao() throws Exception {
        mvc.perform(get("/api/cursos")).andExpect(status().isUnauthorized());
    }

    @Test
    void editarCursoEModuloEInativarModulo() throws Exception {
        long idModulo = prepararModulo();
        long idAula = criarAula(idModulo);
        long idCurso = modulos.findById(idModulo).orElseThrow().getIdCurso();
        mvc.perform(put("/api/cursos/" + idCurso).with(comoProfessor(professor)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Curso atualizado\",\"descricao\":\"Nova descricao\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.titulo").value("Curso atualizado"));
        mvc.perform(put("/api/modulos/" + idModulo).with(comoProfessor(professor)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Modulo atualizado\",\"descricao\":\"Nova descricao\",\"ordem\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ordem").value(2));
        mvc.perform(delete("/api/modulos/" + idModulo).with(comoProfessor(professor))).andExpect(status().isNoContent());
        mvc.perform(get("/api/aulas/" + idAula).with(comoProfessor(professor))).andExpect(status().isNotFound());
        mvc.perform(get("/api/cursos/" + idCurso + "/modulos").with(comoProfessor(professor)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    private long prepararModulo() throws Exception {
        mvc.perform(post("/api/cursos").with(comoProfessor(professor)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Java\",\"descricao\":\"Curso de Java\"}"))
                .andExpect(status().isCreated());
        long idCurso = cursos.findByIdProfessorAndAtivoTrueOrderByIdDesc(professor.getId()).getFirst().getId();
        mvc.perform(post("/api/cursos/" + idCurso + "/modulos").with(comoProfessor(professor))
                .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"Inicio\",\"descricao\":\"Basico\",\"ordem\":1}"))
                .andExpect(status().isCreated());
        return modulos.findByIdCursoAndAtivoTrueOrderByOrdemAscIdAsc(idCurso).getFirst().getId();
    }

    @Test
    void matriculaGratuitaLiberaAulasImediatamenteSemDuplicar() throws Exception {
        long idModulo = prepararModulo();
        long idAula = criarAula(idModulo);
        long idCurso = modulos.findById(idModulo).orElseThrow().getIdCurso();
        Usuario aluno = criarAluno();
        mvc.perform(get("/api/aluno/cursos").with(comoAluno(aluno)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(idCurso));
        mvc.perform(get("/api/aluno/aulas/" + idAula).with(comoAluno(aluno)))
                .andExpect(status().isForbidden());
        for (int tentativa = 0; tentativa < 2; tentativa++) {
            mvc.perform(post("/api/aluno/cursos/" + idCurso + "/matriculas").with(comoAluno(aluno)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.idCurso").value(idCurso))
                    .andExpect(jsonPath("$.matriculadoEm").isNotEmpty());
        }
        assertThat(matriculas.findByIdAlunoOrderByIdDesc(aluno.getId())).hasSize(1);
        mvc.perform(get("/api/aluno/matriculas").with(comoAluno(aluno)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/aluno/cursos/" + idCurso).with(comoAluno(aluno)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/aluno/cursos/" + idCurso + "/modulos").with(comoAluno(aluno)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(idModulo));
        mvc.perform(get("/api/aluno/modulos/" + idModulo + "/aulas").with(comoAluno(aluno)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].youtubeVideoId").value("M7lc1UVf-VE"));
        mvc.perform(get("/api/aluno/aulas/" + idAula).with(comoAluno(aluno)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.youtubeVideoId").value("M7lc1UVf-VE"));
    }

    @Test
    void matriculaNaoLiberaConteudoDeOutroCurso() throws Exception {
        long primeiroModulo = prepararModulo();
        long idCurso = modulos.findById(primeiroModulo).orElseThrow().getIdCurso();
        long outroModulo = prepararModulo();
        long outraAula = criarAula(outroModulo);
        Usuario aluno = criarAluno();
        alunoAulas.matricular(idCurso, aluno.getId());
        mvc.perform(get("/api/aluno/aulas/" + outraAula).with(comoAluno(aluno)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/aluno/modulos/" + outroModulo + "/aulas").with(comoAluno(aluno)))
                .andExpect(status().isForbidden());
    }

    @Test
    void matriculaDeOutroAlunoNaoLiberaAula() throws Exception {
        long idModulo = prepararModulo();
        long idAula = criarAula(idModulo);
        long idCurso = modulos.findById(idModulo).orElseThrow().getIdCurso();
        Usuario aluno = criarAluno();
        alunoAulas.matricular(idCurso, aluno.getId());
        Usuario outro = criarProfessor("segundo-aluno");
        outro.setCpf("12345678903");
        outro.setPerfil(Perfil.ALUNO);
        usuarios.saveAndFlush(outro);
        mvc.perform(get("/api/aluno/aulas/" + idAula).with(comoAluno(outro))).andExpect(status().isForbidden());
        mvc.perform(get("/api/aluno/matriculas").with(comoAluno(outro)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void contaInativaPerdeAcessoMesmoComMatricula() throws Exception {
        long idModulo = prepararModulo();
        long idAula = criarAula(idModulo);
        Usuario aluno = criarAluno();
        alunoAulas.matricular(modulos.findById(idModulo).orElseThrow().getIdCurso(), aluno.getId());
        aluno.setAtivo(false);
        usuarios.saveAndFlush(aluno);
        mvc.perform(get("/api/aluno/aulas/" + idAula).with(comoAluno(aluno))).andExpect(status().isForbidden());
    }

    @Test
    void cursoModuloEAulaInativosNaoLiberamConteudo() throws Exception {
        long idModulo = prepararModulo();
        long idAula = criarAula(idModulo);
        long idCurso = modulos.findById(idModulo).orElseThrow().getIdCurso();
        Usuario aluno = criarAluno();
        alunoAulas.matricular(idCurso, aluno.getId());
        var aula = aulas.findById(idAula).orElseThrow();
        aula.setAtivo(false);
        aulas.saveAndFlush(aula);
        mvc.perform(get("/api/aluno/aulas/" + idAula).with(comoAluno(aluno))).andExpect(status().isNotFound());
        mvc.perform(get("/api/aluno/modulos/" + idModulo + "/aulas").with(comoAluno(aluno)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        aula.setAtivo(true);
        aulas.saveAndFlush(aula);
        var modulo = modulos.findById(idModulo).orElseThrow();
        modulo.setAtivo(false);
        modulos.saveAndFlush(modulo);
        mvc.perform(get("/api/aluno/aulas/" + idAula).with(comoAluno(aluno))).andExpect(status().isNotFound());
        modulo.setAtivo(true);
        modulos.saveAndFlush(modulo);
        var curso = cursos.findById(idCurso).orElseThrow();
        curso.setAtivo(false);
        cursos.saveAndFlush(curso);
        mvc.perform(get("/api/aluno/aulas/" + idAula).with(comoAluno(aluno))).andExpect(status().isNotFound());
        mvc.perform(post("/api/aluno/cursos/" + idCurso + "/matriculas").with(comoAluno(aluno)))
                .andExpect(status().isNotFound());
    }

    @Test
    void alunoNaoPodeEditarConteudo() throws Exception {
        Usuario aluno = criarAluno();
        mvc.perform(post("/api/cursos").with(comoAluno(aluno)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Indevido\",\"descricao\":\"Teste\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/aluno/aulas/1").with(comoAluno(aluno))).andExpect(status().isForbidden());
    }

    @Test
    void matriculaExigeAlunoAutenticadoECursoExistente() throws Exception {
        mvc.perform(post("/api/aluno/cursos/1/matriculas")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/aluno/cursos/1/matriculas").with(comoProfessor(professor)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/aluno/cursos/999999/matriculas").with(comoAluno(criarAluno())))
                .andExpect(status().isNotFound());
    }

    private Usuario criarAluno() {
        Usuario aluno = criarProfessor("aluno");
        aluno.setPerfil(Perfil.ALUNO);
        return usuarios.saveAndFlush(aluno);
    }

    @Test
    void retomarAulaEConcluirManualmenteSemPercentualMinimo() throws Exception {
        long idModulo = prepararModulo();
        long idAula = criarAula(idModulo);
        Usuario aluno = criarAluno();
        var matricula = alunoAulas.matricular(modulos.findById(idModulo).orElseThrow().getIdCurso(), aluno.getId());
        String rota = "/api/aluno/aulas/" + idAula;
        mvc.perform(get(rota + "/progresso").with(comoAluno(aluno)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.posicaoSegundos").value(0))
                .andExpect(jsonPath("$.concluida").value(false));
        assertThat(progressos.findByIdMatriculaAndIdAula(matricula.getId(), idAula)).isEmpty();
        mvc.perform(put(rota + "/progresso").with(comoAluno(aluno)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"posicaoSegundos\":120}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.posicaoSegundos").value(120));
        mvc.perform(get(rota + "/progresso").with(comoAluno(aluno)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.posicaoSegundos").value(120));
        mvc.perform(post(rota + "/conclusao").with(comoAluno(aluno)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.concluida").value(true));
        var concluido = progressos.findByIdMatriculaAndIdAula(matricula.getId(), idAula).orElseThrow();
        var dataConclusao = concluido.getConcluidaEm();
        mvc.perform(post(rota + "/conclusao").with(comoAluno(aluno))).andExpect(status().isOk());
        assertThat(progressos.findByIdMatriculaAndIdAula(matricula.getId(), idAula).orElseThrow().getConcluidaEm())
                .isEqualTo(dataConclusao);
        mvc.perform(put(rota + "/progresso").with(comoAluno(aluno)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"posicaoSegundos\":30}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.posicaoSegundos").value(30))
                .andExpect(jsonPath("$.concluida").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = { "{\"posicaoSegundos\":-1}", "{}", "{\"posicaoSegundos\":null}" })
    void rejeitarPosicaoInvalida(String corpo) throws Exception {
        long idModulo = prepararModulo();
        long idAula = criarAula(idModulo);
        Usuario aluno = criarAluno();
        var matricula = alunoAulas.matricular(modulos.findById(idModulo).orElseThrow().getIdCurso(), aluno.getId());
        mvc.perform(put("/api/aluno/aulas/" + idAula + "/progresso").with(comoAluno(aluno))
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isBadRequest());
        assertThat(progressos.findByIdMatriculaAndIdAula(matricula.getId(), idAula)).isEmpty();
    }

    @Test
    void progressoEConclusaoExigemMatriculaEConteudoAtivo() throws Exception {
        long idModulo = prepararModulo();
        long idAula = criarAula(idModulo);
        Usuario aluno = criarAluno();
        String rota = "/api/aluno/aulas/" + idAula;
        mvc.perform(get(rota + "/progresso").with(comoAluno(aluno))).andExpect(status().isForbidden());
        mvc.perform(put(rota + "/progresso").with(comoAluno(aluno)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"posicaoSegundos\":10}")).andExpect(status().isForbidden());
        mvc.perform(post(rota + "/conclusao").with(comoAluno(aluno))).andExpect(status().isForbidden());
        mvc.perform(post(rota + "/conclusao").with(comoProfessor(professor))).andExpect(status().isForbidden());
        mvc.perform(get(rota + "/progresso")).andExpect(status().isUnauthorized());
        alunoAulas.matricular(modulos.findById(idModulo).orElseThrow().getIdCurso(), aluno.getId());
        mvc.perform(delete("/api/modulos/" + idModulo).with(comoProfessor(professor))).andExpect(status().isNoContent());
        mvc.perform(post(rota + "/conclusao").with(comoAluno(aluno))).andExpect(status().isNotFound());
    }

    @Test
    void concluirSemEnviarPosicaoEReiniciarProgressoAoTrocarVideo() throws Exception {
        long idModulo = prepararModulo();
        long idAula = criarAula(idModulo);
        Usuario aluno = criarAluno();
        alunoAulas.matricular(modulos.findById(idModulo).orElseThrow().getIdCurso(), aluno.getId());
        mvc.perform(post("/api/aluno/aulas/" + idAula + "/conclusao").with(comoAluno(aluno)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.posicaoSegundos").value(0))
                .andExpect(jsonPath("$.concluida").value(true));
        mvc.perform(put("/api/aulas/" + idAula).with(comoProfessor(professor)).contentType(MediaType.APPLICATION_JSON)
                .content(dadosAula("Novo video", "https://youtu.be/abcdefghijk"))).andExpect(status().isOk());
        mvc.perform(get("/api/aluno/aulas/" + idAula + "/progresso").with(comoAluno(aluno)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.concluida").value(false))
                .andExpect(jsonPath("$.posicaoSegundos").value(0));
    }

    private RequestPostProcessor comoAluno(Usuario usuario) {
        var principal = new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), "senha", "ALUNO", true, true, true);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private long criarAula(long idModulo) throws Exception {
        mvc.perform(post("/api/modulos/" + idModulo + "/aulas").with(comoProfessor(professor))
                .contentType(MediaType.APPLICATION_JSON).content(dadosAula("Java", "https://youtu.be/M7lc1UVf-VE")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.youtubeVideoId").value("M7lc1UVf-VE"));
        return aulas.findByIdModuloAndAtivoTrueOrderByOrdemAscIdAsc(idModulo).getFirst().getId();
    }

    private String dadosAula(String titulo, String url) {
        return "{\"titulo\":\"" + titulo + "\",\"descricao\":\"Introducao\",\"ordem\":1,\"youtubeUrl\":\"" + url + "\"}";
    }

    private RequestPostProcessor comoProfessor(Usuario usuario) {
        var principal = new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), "senha", "PROFESSOR", true, true, true);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    protected Usuario criarProfessor(String nome) {
        Usuario usuario = new Usuario();
        usuario.setNomeCompleto(nome);
        usuario.setEmail(nome + "@aulas.example.com");
        usuario.setSenha("hash-teste");
        usuario.setCpf(String.format("%011d", usuarios.count() + 1));
        usuario.setMatricula("aulas-" + nome);
        usuario.setTelefone("11999999999");
        usuario.setEndereco(new Endereco("01001000", "Rua teste", "Centro", "Sao Paulo", "SP"));
        usuario.setDataNascimento(LocalDate.of(2000, 1, 1));
        usuario.setDeclarouMaiorIdade(true);
        usuario.setAceitouTermosLgpd(true);
        usuario.setDataHoraAceite(LocalDateTime.now());
        usuario.setVersaoTermos("teste-v1");
        usuario.setPerfil(Perfil.PROFESSOR);
        usuario.setEmailConfirmado(true);
        return usuarios.save(usuario);
    }
}
