package com.cursovalido.backend.Forum.servico;

import com.cursovalido.backend.Forum.entidade.UsuarioForum;
import com.cursovalido.backend.Forum.entidade.Topico;
import com.cursovalido.backend.Forum.repositorio.ComentarioRepositorio;
import com.cursovalido.backend.Forum.repositorio.TopicoRepositorio;
import com.cursovalido.backend.Forum.excecao.AcessoNegadoException;
import com.cursovalido.backend.Forum.excecao.RecursoNaoEncontradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
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
    @Autowired
    private HistoricoForumServico servicoHistorico;

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
        servicoHistorico.registrar(idUsuario, "CRIAR_TOPICO", "Topico " + topicoSalvo.getId());
        return topicoSalvo;
    }

    public void apagar(Long idTopico, Long idUsuario) {
        UsuarioForum usuario = servicoUsuarios.buscarAtivo(idUsuario);
        verificarAdministrador(usuario);
        Topico topico = buscarAtivo(idTopico);
        topico.setAtivo(false);
        repositorioTopicos.save(topico);
        servicoHistorico.registrar(idUsuario, "APAGAR_TOPICO", "Topico " + idTopico);
    }

    public Topico buscarAtivo(Long idTopico) {
        Topico topico = repositorioTopicos.findById(idTopico)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Topico nao encontrado"));
        if (!topico.isAtivo()) {
            throw new RecursoNaoEncontradoException("Topico arquivado");
        }
        return topico;
    }


    private void verificarAdministrador(UsuarioForum usuario) {
        if (!servicoUsuarios.ehAdministrador(usuario)) {
            throw new AcessoNegadoException(
                    "Somente o administrador pode apagar o topico");
        }
    }
}