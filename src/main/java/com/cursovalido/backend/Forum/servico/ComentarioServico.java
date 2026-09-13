package com.cursovalido.backend.Forum.servico;

import com.cursovalido.backend.Forum.entidade.Comentario;
import com.cursovalido.backend.Forum.entidade.UsuarioForum;
import com.cursovalido.backend.Forum.entidade.Topico;
import com.cursovalido.backend.Forum.repositorio.ComentarioRepositorio;
import com.cursovalido.backend.Forum.excecao.AcessoNegadoException;
import com.cursovalido.backend.Forum.excecao.ConflitoException;
import com.cursovalido.backend.Forum.excecao.RecursoNaoEncontradoException;
import com.cursovalido.backend.Forum.excecao.RequisicaoInvalidaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private HistoricoForumServico servicoHistorico;

    public List<Comentario> listarPorTopico(Long idTopico) {
        servicoTopicos.buscarAtivo(idTopico);
        return repositorioComentarios.listarPorTopicoAtivo(idTopico);
    }

    public Comentario criar(Long idTopico, Comentario comentario, Long idUsuario) {
        UsuarioForum usuario = servicoUsuarios.buscarAtivo(idUsuario);
        Topico topico = servicoTopicos.buscarAtivo(idTopico);

        if (topico.isFechado()) {
            throw new ConflitoException("Topico fechado para novos comentarios");
        }

        comentario.setIdAutor(usuario.getId());
        comentario.setUsuarioAutor(usuario);
        comentario.setTopico(topico);
        comentario.setCriadoEm(LocalDateTime.now());
        comentario.setAtivo(true);

        Comentario comentarioSalvo = repositorioComentarios.save(comentario);
        servicoHistorico.registrar(idUsuario, "CRIAR_COMENTARIO", "Comentario " + comentarioSalvo.getId());

        return comentarioSalvo;
    }
}