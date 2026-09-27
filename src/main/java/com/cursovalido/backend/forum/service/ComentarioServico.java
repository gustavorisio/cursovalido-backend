package com.cursovalido.backend.forum.service;

import com.cursovalido.backend.forum.entity.Comentario;
import com.cursovalido.backend.forum.entity.UsuarioForum;
import com.cursovalido.backend.forum.entity.Topico;
import com.cursovalido.backend.forum.repository.ComentarioRepositorio;
import com.cursovalido.backend.authentication.service.LogAuditoriaServico;
import com.cursovalido.backend.forum.exception.AcessoNegadoException;
import com.cursovalido.backend.forum.exception.ConflitoException;
import com.cursovalido.backend.forum.exception.RecursoNaoEncontradoException;
import com.cursovalido.backend.forum.exception.RequisicaoInvalidaException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ComentarioServico {

    @Autowired
    private ComentarioRepositorio repositorioComentarios;
    @Autowired
    private TopicoServico servicoTopicos;
    @Autowired
    private UsuarioForumServico servicoUsuarios;
    @Autowired
    private LogAuditoriaServico auditoria;

    public List<Comentario> listarPorTopico(Long idTopico) {
        return listarPorTopico(idTopico, null);
    }

    public List<Comentario> listarPorTopico(Long idTopico, Long idUsuario) {
        servicoTopicos.buscarAtivo(idTopico);
        registrar(idUsuario, "CONSULTA_COMENTARIOS", "Topico " + idTopico);
        return repositorioComentarios.listarPorTopicoAtivo(idTopico);
    }

    public Comentario criar(Long idTopico, Comentario comentario, Long idUsuario) {
        UsuarioForum usuario = servicoUsuarios.buscarAtivo(idUsuario);
        Topico topico = servicoTopicos.buscarAtivo(idTopico);

        if (topico.isFechado()) {
            throw ConflitoException.topicoFechado();
        }

        comentario.setIdAutor(usuario.getId());
        comentario.setUsuarioAutor(usuario);
        comentario.setTopico(topico);
        comentario.setCriadoEm(LocalDateTime.now());
        comentario.setAtivo(true);

        Comentario comentarioSalvo = repositorioComentarios.save(comentario);
        registrar(idUsuario, "CADASTRO_COMENTARIO", "Comentario " + comentarioSalvo.getId());

        return comentarioSalvo;
    }

    public Comentario editar(Long idComentario, Comentario dadosAtualizados, Long idUsuario) {
        UsuarioForum usuario = servicoUsuarios.buscarAtivo(idUsuario);
        Comentario comentario = buscar(idComentario);

        if (!usuario.getId().equals(comentario.getIdAutor())) {
            throw AcessoNegadoException.autorEditarComentario();
        }
        if (!comentario.isAtivo()) {
            throw RequisicaoInvalidaException.comentarioApagado();
        }

        comentario.setConteudo(dadosAtualizados.getConteudo());
        Comentario comentarioSalvo = repositorioComentarios.save(comentario);
        registrar(idUsuario, "EDICAO_COMENTARIO", "Comentario " + idComentario);

        return comentarioSalvo;
    }

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

    private void registrar(Long idUsuario, String tipo, String detalhes) {
        if (auditoria != null)
            auditoria.registrar(idUsuario, tipo, detalhes, null);
    }
}