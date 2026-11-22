package com.cursovalido.backend.forum.service;

import lombok.RequiredArgsConstructor;

import com.cursovalido.backend.forum.entity.Comentario;
import com.cursovalido.backend.forum.entity.UsuarioForum;
import com.cursovalido.backend.forum.entity.Topico;
import com.cursovalido.backend.forum.repository.ComentarioRepository;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import com.cursovalido.backend.forum.exception.AcessoNegadoException;
import com.cursovalido.backend.forum.exception.ConflitoException;
import com.cursovalido.backend.forum.exception.RecursoNaoEncontradoException;
import com.cursovalido.backend.forum.exception.RequisicaoInvalidaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComentarioService {

    private final ComentarioRepository repositorioComentarios;
    private final TopicoService servicoTopicos;
    private final UsuarioForumService servicoUsuarios;
    private final LogAuditoriaService auditoria;
    private final FiltroConteudoOfensivo filtroConteudo = new FiltroConteudoOfensivo();

    // Lista os comentarios de um topico, incluindo os excluidos logicamente.
    public List<Comentario> listarPorTopico(Long idTopico) {
        return listarPorTopico(idTopico, null);
    }

    // Lista comentarios e registra a consulta do usuario.
    public List<Comentario> listarPorTopico(Long idTopico, Long idUsuario) {
        servicoTopicos.buscarAtivo(idTopico);
        registrar(idUsuario, "CONSULTA_COMENTARIOS", "Topico " + idTopico);
        return repositorioComentarios.listarPorTopicoAtivo(idTopico);
    }

    public org.springframework.data.domain.Page<Comentario> listarPaginaPorTopico(Long idTopico, Long idUsuario,
            int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 50) {
            throw new RequisicaoInvalidaException("Página deve ser maior ou igual a zero e tamanho deve estar entre 1 e 50.");
        }
        servicoTopicos.buscarAtivo(idTopico);
        registrar(idUsuario, "CONSULTA_COMENTARIOS", "Topico " + idTopico);
        return repositorioComentarios.listarPorTopicoAtivo(idTopico,
                org.springframework.data.domain.PageRequest.of(pagina, tamanho));
    }

    // Cria um comentario em um topico aberto.
    @Transactional
    public Comentario criar(Long idTopico, Comentario comentario, Long idUsuario) {
        UsuarioForum usuario = servicoUsuarios.bloquearAtivo(idUsuario);
        Topico topico = servicoTopicos.buscarAtivo(idTopico);

        if (topico.isFechado()) {
            throw ConflitoException.topicoFechado();
        }
        LocalDateTime agora = LocalDateTime.now(ZoneOffset.UTC);
        if (repositorioComentarios.existsByIdAutorAndCriadoEmAfter(usuario.getId(), agora.minusSeconds(20))) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                    "Você já enviou um comentário recentemente. Aguarde 20 segundos entre comentários, mesmo em tópicos diferentes.");
        }
        comentario.setConteudo(censurarConteudo(idUsuario, comentario.getConteudo()));

        comentario.setIdAutor(usuario.getId());
        comentario.setUsuarioAutor(usuario);
        comentario.setTopico(topico);
        comentario.setCriadoEm(agora);
        comentario.setAtivo(true);

        Comentario comentarioSalvo = repositorioComentarios.save(comentario);
        registrar(idUsuario, "CADASTRO_COMENTARIO", "Comentario " + comentarioSalvo.getId());

        return comentarioSalvo;
    }

    // Edita um comentario somente pelo seu autor.
    @Transactional
    public Comentario editar(Long idComentario, Comentario dadosAtualizados, Long idUsuario) {
        UsuarioForum usuario = servicoUsuarios.buscarAtivo(idUsuario);
        Comentario comentario = buscar(idComentario);

        if (!usuario.getId().equals(comentario.getIdAutor())) {
            throw AcessoNegadoException.autorEditarComentario();
        }
        if (!comentario.isAtivo()) {
            throw RequisicaoInvalidaException.comentarioApagado();
        }
        dadosAtualizados.setConteudo(censurarConteudo(idUsuario, dadosAtualizados.getConteudo()));

        comentario.setConteudo(dadosAtualizados.getConteudo());
        Comentario comentarioSalvo = repositorioComentarios.save(comentario);
        registrar(idUsuario, "EDICAO_COMENTARIO", "Comentario " + idComentario);

        return comentarioSalvo;
    }

    // Desativa um comentario pelo autor ou administrador.
    @Transactional
    public void apagar(Long idComentario, Long idUsuario) {
        UsuarioForum usuario = servicoUsuarios.buscarAtivo(idUsuario);
        Comentario comentario = buscar(idComentario);

        boolean ehAutor = comentario.getIdAutor() != null && comentario.getIdAutor().equals(usuario.getId());

        if (!ehAutor && !servicoUsuarios.ehAdministrador(usuario)) {
            throw AcessoNegadoException.autorOuAdministradorApagarComentario();
        }

        comentario.setAtivo(false);
        repositorioComentarios.save(comentario);
        registrar(idUsuario, "EXCLUSAO_COMENTARIO", "Comentario " + idComentario);
    }

    // Confere o topico e desativa o comentario informado.
    @Transactional
    public void apagarDoTopico(Long idTopico, Long idComentario, Long idUsuario) {
        Comentario comentario = buscar(idComentario);
        if (comentario.getTopico() == null || !idTopico.equals(comentario.getTopico().getId())) {
            throw RecursoNaoEncontradoException.comentarioForaDoTopico();
        }
        apagar(idComentario, idUsuario);
    }

    private Comentario buscar(Long idComentario) {
        return repositorioComentarios.findById(idComentario)
                .orElseThrow(RecursoNaoEncontradoException::comentarioNaoEncontrado);
    }

    private String censurarConteudo(Long idUsuario, String texto) {
        String censurado = filtroConteudo.censurar(texto);
        if (!java.util.Objects.equals(texto, censurado)) {
            registrar(idUsuario, "CONTEUDO_OFENSIVO_OCULTADO", "Comentario");
        }
        return censurado;
    }

    private void registrar(Long idUsuario, String tipo, String detalhes) {
        if (auditoria != null)
            auditoria.registrar(idUsuario, tipo, detalhes, null);
    }
}
