package com.cursovalido.backend.forum.repository;

import static org.assertj.core.api.Assertions.*;
import com.cursovalido.backend.forum.entity.*;
import com.cursovalido.backend.forum.support.PostgresTeste;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.*;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@ActiveProfiles("test")
@ExtendWith(PostgresTeste.CicloDeVida.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ForumRepositoriosTest {
    @Autowired private UsuarioForumRepositorio usuarios;
    @Autowired private TopicoRepositorio topicos;
    @Autowired private ComentarioRepositorio comentarios;
    @Autowired private EntityManager entityManager;
    private UsuarioForum autor;
    private final LocalDateTime base = LocalDateTime.of(2026, 1, 1, 12, 0);

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registry) { PostgresTeste.configurar(registry, "forum_repository"); }

    @BeforeEach
    void criarAutor() { autor = usuarios.save(new UsuarioForum(7L, "Autor", "autor@example.com", "ALUNO")); }

    @Test
    void listarRecentes_comFiltros_deveIgnorarArquivadosEBuscarSemDistinguirMaiusculas() {
        var java = topico("Java", 1);
        topico("Outro", 2);
        var arquivado = topico("JAVA arquivado", 3);
        arquivado.setAtivo(false);
        topicos.saveAndFlush(arquivado);
        entityManager.clear();
        var lista = topicos.listarRecentes(PageRequest.of(0, 10), "jAvA");
        assertThat(lista).extracting(t -> t.id()).containsExactly(java.getId());
    }

    @Test
    void listarRecentes_comPaginacao_deveOrdenarMaisRecentesPrimeiro() {
        var primeiro = topico("Primeiro", 1);
        var segundo = topico("Segundo", 2);
        topicos.flush();
        assertThat(topicos.listarRecentes(PageRequest.of(0, 1), "")).extracting(t -> t.id()).containsExactly(segundo.getId());
        assertThat(topicos.listarRecentes(PageRequest.of(1, 1), "")).extracting(t -> t.id()).containsExactly(primeiro.getId());
    }

    @Test
    void listarRecentes_comComentariosAtivosEApagados_deveContarSomenteAtivos() {
        var topico = topico("Contagem", 1);
        comentario(topico, true, 1);
        comentario(topico, true, 2);
        comentario(topico, false, 3);
        comentarios.flush();
        assertThat(topicos.listarRecentes(PageRequest.of(0, 10), "")).singleElement().satisfies(resumo -> {
            assertThat(resumo.quantidadeRespostas()).isEqualTo(2);
            assertThat(resumo.nomeAutor()).isEqualTo("Autor");
        });
        assertThat(comentarios.contarPorTopico(topico.getId())).isEqualTo(2);
    }

    @Test
    void listarPorTopicoAtivo_comComentariosMistos_deveFiltrarEOrdenar() {
        var principal = topico("Principal", 1);
        var outro = topico("Outro", 2);
        var segundo = comentario(principal, true, 2);
        var primeiro = comentario(principal, true, 1);
        comentario(principal, false, 3);
        comentario(outro, true, 4);
        comentarios.flush();
        entityManager.clear();
        assertThat(comentarios.listarPorTopicoAtivo(principal.getId())).extracting(Comentario::getId)
                .containsExactly(primeiro.getId(), segundo.getId());
    }

    @Test
    void listarPorTopicoAtivo_comTopicoArquivado_deveOcultarComentarios() {
        var topico = topico("Arquivado", 1);
        comentario(topico, true, 1);
        topico.setAtivo(false);
        topicos.saveAndFlush(topico);
        assertThat(comentarios.listarPorTopicoAtivo(topico.getId())).isEmpty();
    }

    @Test
    void listarUsuarios_comAtivosEInativos_deveFiltrarEOrdenarPorId() {
        usuarios.save(new UsuarioForum(3L, "Outro", "outro@example.com", "PROFESSOR"));
        var inativo = new UsuarioForum(1L, "Inativo", "inativo@example.com", "ALUNO");
        inativo.setAtivo(false);
        usuarios.saveAndFlush(inativo);
        assertThat(usuarios.findByAtivoTrueOrderByIdAsc()).extracting(UsuarioForum::getId).containsExactly(3L, 7L);
    }

    @Test
    void sincronizar_comUsuarioNovo_deveInserirPorSqlNativo() {
        usuarios.sincronizar(99L, "Novo", "novo@example.com", "PROFESSOR", true);
        entityManager.clear();
        assertThat(usuarios.findById(99L)).hasValueSatisfying(usuario -> {
            assertThat(usuario.getNome()).isEqualTo("Novo");
            assertThat(usuario.getEmail()).isEqualTo("novo@example.com");
            assertThat(usuario.getPerfil()).isEqualTo("PROFESSOR");
            assertThat(usuario.isAtivo()).isTrue();
        });
    }

    @Test
    void sincronizar_comUsuarioExistente_deveAtualizarSemDuplicar() {
        usuarios.flush();
        usuarios.sincronizar(7L, "Atualizado", "novo@example.com", "ADMINISTRADOR", false);
        entityManager.clear();
        assertThat(usuarios.count()).isEqualTo(1);
        assertThat(usuarios.findById(7L)).hasValueSatisfying(usuario -> {
            assertThat(usuario.getNome()).isEqualTo("Atualizado");
            assertThat(usuario.getEmail()).isEqualTo("novo@example.com");
            assertThat(usuario.getPerfil()).isEqualTo("ADMINISTRADOR");
            assertThat(usuario.isAtivo()).isFalse();
        });
    }

    @Test
    void buscarPorAutor_comAutoresDiferentes_deveRetornarSomenteTopicosDoAutor() {
        var principal = topico("Principal", 1);
        var outro = usuarios.save(new UsuarioForum(8L, "Outro", "outro@example.com", "ALUNO"));
        var outroTopico = topico("Outro", 2);
        outroTopico.setIdAutor(outro.getId());
        outroTopico.setUsuarioAutor(outro);
        topicos.saveAndFlush(outroTopico);
        assertThat(topicos.findByIdAutor(7L)).extracting(Topico::getId).containsExactly(principal.getId());
    }

    @Test
    void excluirPorTopico_comDoisTopicos_devePreservarComentariosDoOutro() {
        var principal = topico("Principal", 1);
        var outro = topico("Outro", 2);
        comentario(principal, true, 1);
        var preservado = comentario(outro, true, 2);
        comentarios.flush();
        comentarios.deleteByTopicoId(principal.getId());
        comentarios.flush();
        entityManager.clear();
        assertThat(comentarios.findAll()).extracting(Comentario::getId).containsExactly(preservado.getId());
    }

    @Test
    void excluirPorAutor_comAutoresDiferentes_devePreservarComentariosDoOutro() {
        var topico = topico("Principal", 1);
        comentario(topico, true, 1);
        var outro = usuarios.save(new UsuarioForum(8L, "Outro", "outro@example.com", "ALUNO"));
        var preservado = comentario(topico, true, 2);
        preservado.setIdAutor(outro.getId());
        preservado.setUsuarioAutor(outro);
        comentarios.saveAndFlush(preservado);
        comentarios.deleteByIdAutor(7L);
        comentarios.flush();
        entityManager.clear();
        assertThat(comentarios.findAll()).extracting(Comentario::getId).containsExactly(preservado.getId());
    }

    @Test
    void salvarTopico_comAutorInexistente_deveRejeitarChaveEstrangeira() {
        var topico = new Topico();
        topico.setTitulo("Invalido");
        topico.setDescricao("Sem autor existente");
        topico.setIdAutor(999999L);
        topico.setCriadoEm(base);
        assertThatThrownBy(() -> topicos.saveAndFlush(topico)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    private Topico topico(String titulo, int minuto) {
        var topico = new Topico();
        topico.setTitulo(titulo);
        topico.setDescricao("Descricao " + titulo);
        topico.setIdAutor(autor.getId());
        topico.setUsuarioAutor(autor);
        topico.setCriadoEm(base.plusMinutes(minuto));
        return topicos.save(topico);
    }

    private Comentario comentario(Topico topico, boolean ativo, int minuto) {
        var comentario = new Comentario();
        comentario.setConteudo("Resposta " + minuto);
        comentario.setIdAutor(autor.getId());
        comentario.setUsuarioAutor(autor);
        comentario.setTopico(topico);
        comentario.setAtivo(ativo);
        comentario.setCriadoEm(base.plusMinutes(minuto));
        return comentarios.save(comentario);
    }
}
