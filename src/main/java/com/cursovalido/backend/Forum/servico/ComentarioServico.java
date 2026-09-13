package com.cursovalido.backend.Forum.servico;

import com.cursovalido.backend.Forum.entidade.Comentario;
import com.cursovalido.backend.Forum.entidade.UsuarioForum;
import com.cursovalido.backend.Forum.entidade.Topico;
import com.cursovalido.backend.Forum.repositorio.ComentarioRepositorio;
import com.cursovalido.backend.Forum.excecao.AcessoNegadoException;
import com.cursovalido.backend.Forum.excecao.ConflitoException;
import com.cursovalido.backend.Forum.excecao.RecursoNaoEncontradoException;
import com.cursovalido.backend.Forum.excecao.RequisicaoInvalidaException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ComentarioServico {

    private final ComentarioRepositorio repositorioComentarios;
    private final TopicoServico servicoTopicos;
    private final UsuarioForumServico servicoUsuarios;

    public ComentarioServico(ComentarioRepositorio repositorioComentarios,
            TopicoServico servicoTopicos, UsuarioForumServico servicoUsuarios) {
        this.repositorioComentarios = repositorioComentarios;
        this.servicoTopicos = servicoTopicos;
        this.servicoUsuarios = servicoUsuarios;
    }

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

        return comentarioSalvo;
    }

    public Comentario editar(Long idComentario, Comentario dadosAtualizados, Long idUsuario) {
        UsuarioForum usuario = servicoUsuarios.buscarAtivo(idUsuario);
        Comentario comentario = buscar(idComentario);

        if (!usuario.getId().equals(comentario.getIdAutor())) {
            throw new AcessoNegadoException("Somente o autor pode editar o comentario");
        }
        if (!comentario.isAtivo()) {
            throw new RequisicaoInvalidaException("Comentario apagado nao pode ser editado");
        }

        comentario.setConteudo(dadosAtualizados.getConteudo());
        Comentario comentarioSalvo = repositorioComentarios.save(comentario);

        return comentarioSalvo;
    }

    public void apagar(Long idComentario, Long idUsuario) {
        UsuarioForum usuario = servicoUsuarios.buscarAtivo(idUsuario);
        Comentario comentario = buscar(idComentario);

        boolean ehAutor = comentario.getIdAutor() != null && comentario.getIdAutor().equals(usuario.getId());

        if (!ehAutor && !servicoUsuarios.ehAdministrador(usuario)) {
            throw new AcessoNegadoException("Somente o autor ou administrador pode apagar");
        }

        comentario.setAtivo(false);
        repositorioComentarios.save(comentario);
    }

    public void apagarDoTopico(Long idTopico, Long idComentario, Long idUsuario) {
        Comentario comentario = buscar(idComentario);
        if (comentario.getTopico() == null || !idTopico.equals(comentario.getTopico().getId())) {
            throw new RecursoNaoEncontradoException("Comentario nao pertence ao topico");
        }
        apagar(idComentario, idUsuario);
    }

    private Comentario buscar(Long idComentario) {
        return repositorioComentarios.findById(idComentario)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Comentario nao encontrado"));
    }
}