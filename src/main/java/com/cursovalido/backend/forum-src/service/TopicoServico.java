package com.cursovalido.backend.forum.service;

import com.cursovalido.backend.forum.entity.UsuarioForum;
import com.cursovalido.backend.forum.entity.Topico;
import com.cursovalido.backend.forum.dto.TopicoResumoDTO;
import com.cursovalido.backend.forum.repository.TopicoRepositorio;
import com.cursovalido.backend.authentication.service.LogAuditoriaServico;
import com.cursovalido.backend.forum.exception.AcessoNegadoException;
import com.cursovalido.backend.forum.exception.RecursoNaoEncontradoException;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TopicoServico {
    @Autowired
    private TopicoRepositorio repositorioTopicos;
    @Autowired
    private UsuarioForumServico servicoUsuarios;
    @Autowired
    private LogAuditoriaServico auditoria;

    // Lista os topicos mais recentes.
    public List<TopicoResumoDTO> listarRecentes(int pagina, int tamanho) {
        return listarRecentes(pagina, tamanho, "");
    }

    // Lista os topicos recentes filtrando pelo texto informado.
    public List<TopicoResumoDTO> listarRecentes(int pagina, int tamanho, String busca) {
        return listarRecentes(pagina, tamanho, busca, null);
    }

    // Lista os topicos e registra a consulta quando houver usuario.
    public List<TopicoResumoDTO> listarRecentes(int pagina, int tamanho, String busca, Long idUsuario) {
        String termo = busca == null ? "" : busca.trim();
        registrar(idUsuario, "CONSULTA_TOPICOS", termo);
        return repositorioTopicos.listarRecentes(PageRequest.of(pagina, tamanho), termo);
    }

    // Cria um novo topico para o usuario informado.
    public Topico criar(Topico topico, Long idUsuario) {
        UsuarioForum usuario = servicoUsuarios.buscarAtivo(idUsuario);
        topico.setIdAutor(usuario.getId());
        topico.setUsuarioAutor(usuario);
        topico.setCriadoEm(LocalDateTime.now());
        topico.setAtivo(true);
        topico.setFechado(false);
        Topico topicoSalvo = repositorioTopicos.save(topico);
        topicoSalvo.setQuantidadeRespostas(0);
        registrar(idUsuario, "CADASTRO_TOPICO", "Topico " + topicoSalvo.getId());
        return topicoSalvo;
    }

    // Edita um topico pelo autor ou administrador.
    public Topico editar(Long idTopico, Topico dadosAtualizados, Long idUsuario) {
        UsuarioForum usuario = servicoUsuarios.buscarAtivo(idUsuario);
        Topico topico = buscarAtivo(idTopico);
        verificarAutoria(topico, usuario);
        topico.setTitulo(dadosAtualizados.getTitulo());
        topico.setDescricao(dadosAtualizados.getDescricao());
        Topico topicoSalvo = repositorioTopicos.save(topico);
        registrar(idUsuario, "EDICAO_TOPICO", "Topico " + idTopico);
        return topicoSalvo;
    }

    // Arquiva um topico sem apagar seus dados.
    public void arquivar(Long idTopico, Long idUsuario) {
        UsuarioForum usuario = servicoUsuarios.buscarAtivo(idUsuario);
        Topico topico = buscarAtivo(idTopico);
        verificarAutoriaOuAdministrador(topico, usuario);
        topico.setAtivo(false);
        repositorioTopicos.save(topico);
        registrar(idUsuario, "ARQUIVAMENTO_TOPICO", "Topico " + idTopico);
    }

    // Apaga logicamente um topico como administrador.
    public void apagar(Long idTopico, Long idUsuario) {
        UsuarioForum usuario = servicoUsuarios.buscarAtivo(idUsuario);
        verificarAdministrador(usuario);
        Topico topico = buscarAtivo(idTopico);
        topico.setAtivo(false);
        repositorioTopicos.save(topico);
        registrar(idUsuario, "EXCLUSAO_TOPICO", "Topico " + idTopico);
    }

    // Fecha um topico para novos comentarios.
    public void fechar(Long idTopico, Long idUsuario) {
        UsuarioForum usuario = servicoUsuarios.buscarAtivo(idUsuario);
        Topico topico = buscarAtivo(idTopico);
        verificarAutoriaOuAdministrador(topico, usuario);
        topico.setFechado(true);
        repositorioTopicos.save(topico);
        registrar(idUsuario, "FECHAMENTO_TOPICO", "Topico " + idTopico);
    }

    // Busca um topico que ainda esta ativo.
    public Topico buscarAtivo(Long idTopico) {
        Topico topico = repositorioTopicos.findById(idTopico)
                .orElseThrow(RecursoNaoEncontradoException::topicoNaoEncontrado);
        if (!topico.isAtivo()) {
            throw RecursoNaoEncontradoException.topicoArquivado();
        }
        return topico;
    }

    private void verificarAutoriaOuAdministrador(Topico topico, UsuarioForum usuario) {
        boolean ehAutor = topico.getIdAutor() != null && topico.getIdAutor().equals(usuario.getId());
        if (!ehAutor && !servicoUsuarios.ehAdministrador(usuario)) {
            throw AcessoNegadoException.autorOuAdministradorAlterarTopico();
        }
    }

    private void verificarAutoria(Topico topico, UsuarioForum usuario) {
        if (topico.getIdAutor() == null || !topico.getIdAutor().equals(usuario.getId())) {
            throw AcessoNegadoException.autorEditarTopico();
        }
    }

    private void verificarAdministrador(UsuarioForum usuario) {
        if (!servicoUsuarios.ehAdministrador(usuario)) {
            throw AcessoNegadoException.administradorApagarTopico();
        }
    }

    private void registrar(Long idUsuario, String tipo, String detalhes) {
        if (auditoria != null)
            auditoria.registrar(idUsuario, tipo, detalhes, null);
    }
}