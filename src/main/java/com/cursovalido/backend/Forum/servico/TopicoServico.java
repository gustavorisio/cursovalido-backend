package com.cursovalido.backend.Forum.servico;

import com.cursovalido.backend.Forum.entidade.UsuarioForum;
import com.cursovalido.backend.Forum.entidade.Topico;
import com.cursovalido.backend.Forum.repositorio.ComentarioRepositorio;
import com.cursovalido.backend.Forum.repositorio.TopicoRepositorio;
import com.cursovalido.backend.Forum.excecao.AcessoNegadoException;
import com.cursovalido.backend.Forum.excecao.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TopicoServico {
    @Autowired
    private TopicoRepositorio repositorioTopicos;
    @Autowired
    private ComentarioRepositorio repositorioComentarios;
    @Autowired
    private UsuarioForumServico servicoUsuarios;

    public List<Topico> listarRecentes(int pagina, int tamanho) {
        List<Topico> topicos = repositorioTopicos.listarRecentes(PageRequest.of(pagina, tamanho));
        Map<Long, Long> quantidades = new HashMap<>();
        if (!topicos.isEmpty()) {
            repositorioComentarios.contarPorTopicos(topicos.stream()
                    .map(Topico::getId)
                    .collect(Collectors.toSet()))
                    .forEach(contagem -> quantidades.put(contagem.getIdTopico(), contagem.getQuantidade()));
        }

        topicos.forEach(topico -> {
            topico.setQuantidadeRespostas(quantidades.getOrDefault(topico.getId(), 0L));
        });
        return topicos;
    }

    public Topico criar(Topico topico, Long idUsuario) {
        UsuarioForum usuario = servicoUsuarios.buscarAtivo(idUsuario);
        topico.setIdAutor(usuario.getId());
        topico.setUsuarioAutor(usuario);
        topico.setCriadoEm(LocalDateTime.now());
        topico.setAtivo(true);
        topico.setFechado(false);
        Topico topicoSalvo = repositorioTopicos.save(topico);
        topicoSalvo.setQuantidadeRespostas(0);
        return topicoSalvo;
    }

    public Topico editar(Long idTopico, Topico dadosAtualizados, Long idUsuario) {
        UsuarioForum usuario = servicoUsuarios.buscarAtivo(idUsuario);
        Topico topico = buscarAtivo(idTopico);
        verificarAutoria(topico, usuario);
        topico.setTitulo(dadosAtualizados.getTitulo());
        topico.setDescricao(dadosAtualizados.getDescricao());
        Topico topicoSalvo = repositorioTopicos.save(topico);
        return topicoSalvo;
    }

    public void arquivar(Long idTopico, Long idUsuario) {
        UsuarioForum usuario = servicoUsuarios.buscarAtivo(idUsuario);
        Topico topico = buscarAtivo(idTopico);
        verificarAutoriaOuAdministrador(topico, usuario);
        topico.setAtivo(false);
        repositorioTopicos.save(topico);
    }

    public void apagar(Long idTopico, Long idUsuario) {
        UsuarioForum usuario = servicoUsuarios.buscarAtivo(idUsuario);
        verificarAdministrador(usuario);
        Topico topico = buscarAtivo(idTopico);
        topico.setAtivo(false);
        repositorioTopicos.save(topico);
    }

    public void fechar(Long idTopico, Long idUsuario) {
        UsuarioForum usuario = servicoUsuarios.buscarAtivo(idUsuario);
        Topico topico = buscarAtivo(idTopico);
        verificarAutoriaOuAdministrador(topico, usuario);
        topico.setFechado(true);
        repositorioTopicos.save(topico);
    }

    public Topico buscarAtivo(Long idTopico) {
        Topico topico = repositorioTopicos.findById(idTopico)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Topico nao encontrado"));
        if (!topico.isAtivo()) {
            throw new RecursoNaoEncontradoException("Topico arquivado");
        }
        return topico;
    }

    private void verificarAutoriaOuAdministrador(Topico topico, UsuarioForum usuario) {
        boolean ehAutor = topico.getIdAutor() != null && topico.getIdAutor().equals(usuario.getId());
        if (!ehAutor && !servicoUsuarios.ehAdministrador(usuario)) {
            throw new AcessoNegadoException(
                    "Somente o autor ou administrador pode alterar o topico");
        }
    }

    private void verificarAutoria(Topico topico, UsuarioForum usuario) {
        if (topico.getIdAutor() == null || !topico.getIdAutor().equals(usuario.getId())) {
            throw new AcessoNegadoException(
                    "Somente o autor pode editar o topico");
        }
    }

    private void verificarAdministrador(UsuarioForum usuario) {
        if (!servicoUsuarios.ehAdministrador(usuario)) {
            throw new AcessoNegadoException(
                    "Somente o administrador pode apagar o topico");
        }
    }
}