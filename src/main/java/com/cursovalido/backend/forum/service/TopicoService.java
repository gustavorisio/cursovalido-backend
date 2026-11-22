package com.cursovalido.backend.forum.service;

import lombok.RequiredArgsConstructor;

import com.cursovalido.backend.forum.entity.UsuarioForum;
import com.cursovalido.backend.forum.entity.Topico;
import com.cursovalido.backend.forum.dto.TopicoResumoDTO;
import com.cursovalido.backend.forum.repository.TopicoRepository;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import com.cursovalido.backend.forum.exception.AcessoNegadoException;
import com.cursovalido.backend.forum.exception.RecursoNaoEncontradoException;
import com.cursovalido.backend.forum.exception.RequisicaoInvalidaException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TopicoService {
    private final TopicoRepository repositorioTopicos;
    private final UsuarioForumService servicoUsuarios;
    private final LogAuditoriaService auditoria;
    private final FiltroConteudoOfensivo filtroConteudo = new FiltroConteudoOfensivo();

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
        if (pagina < 0 || tamanho < 1 || tamanho > 100) {
            throw new RequisicaoInvalidaException(
                    "Pagina deve ser maior ou igual a zero e tamanho deve estar entre 1 e 100");
        }
        String termo = busca == null ? "" : busca.trim();
        registrar(idUsuario, "CONSULTA_TOPICOS", termo);
        return repositorioTopicos.listarRecentes(PageRequest.of(pagina, tamanho), termo);
    }

    @Transactional
    public Page<TopicoResumoDTO> listarMeus(int pagina, int tamanho, String busca, Long idUsuario) {
        servicoUsuarios.buscarAtivo(idUsuario);
        if (pagina < 0 || tamanho < 1 || tamanho > 100) {
            throw new RequisicaoInvalidaException(
                    "Pagina deve ser maior ou igual a zero e tamanho deve estar entre 1 e 100");
        }
        String termo = busca == null ? "" : busca.trim();
        registrar(idUsuario, "CONSULTA_MEUS_TOPICOS", termo);
        return repositorioTopicos.listarMeus(PageRequest.of(pagina, tamanho), termo, idUsuario);
    }

    // Cria um novo topico para o usuario informado.
    @Transactional
    public Topico criar(Topico topico, Long idUsuario) {
        UsuarioForum usuario = servicoUsuarios.bloquearAtivo(idUsuario);
        LocalDateTime agora = LocalDateTime.now(ZoneOffset.UTC);
        if (repositorioTopicos.existsByIdAutorAndCriadoEmAfter(usuario.getId(), agora.minusMinutes(3))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Você já criou um tópico recentemente. Aguarde 3 minutos antes de criar outro.");
        }
        censurarConteudo(topico, idUsuario);
        topico.setIdAutor(usuario.getId());
        topico.setUsuarioAutor(usuario);
        topico.setCriadoEm(agora);
        topico.setAtivo(true);
        topico.setFechado(false);
        Topico topicoSalvo = repositorioTopicos.save(topico);
        topicoSalvo.setQuantidadeRespostas(0);
        registrar(idUsuario, "CADASTRO_TOPICO", "Topico " + topicoSalvo.getId());
        return topicoSalvo;
    }

    // Edita um topico pelo autor ou administrador.
    @Transactional
    public Topico editar(Long idTopico, Topico dadosAtualizados, Long idUsuario) {
        UsuarioForum usuario = servicoUsuarios.buscarAtivo(idUsuario);
        Topico topico = buscarAtivo(idTopico);
        verificarAutoria(topico, usuario);
        censurarConteudo(dadosAtualizados, idUsuario);
        topico.setTitulo(dadosAtualizados.getTitulo());
        topico.setDescricao(dadosAtualizados.getDescricao());
        Topico topicoSalvo = repositorioTopicos.save(topico);
        registrar(idUsuario, "EDICAO_TOPICO", "Topico " + idTopico);
        return topicoSalvo;
    }

    // Arquiva um topico sem apagar seus dados.
    @Transactional
    public void arquivar(Long idTopico, Long idUsuario) {
        UsuarioForum usuario = servicoUsuarios.buscarAtivo(idUsuario);
        Topico topico = buscarAtivo(idTopico);
        verificarAutoriaOuAdministrador(topico, usuario);
        topico.setAtivo(false);
        repositorioTopicos.save(topico);
        registrar(idUsuario, "ARQUIVAMENTO_TOPICO", "Topico " + idTopico);
    }

    // Fecha um topico para novos comentarios.
    @Transactional
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
        if ("ALUNO".equalsIgnoreCase(usuario.getPerfil())) {
            throw new AcessoNegadoException("Alunos não podem fechar nem arquivar tópicos.");
        }
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

    private void censurarConteudo(Topico topico, Long idUsuario) {
        String titulo = filtroConteudo.censurar(topico.getTitulo());
        String descricao = filtroConteudo.censurar(topico.getDescricao());
        if (!java.util.Objects.equals(titulo, topico.getTitulo())
                || !java.util.Objects.equals(descricao, topico.getDescricao())) {
            registrar(idUsuario, "CONTEUDO_OFENSIVO_OCULTADO", "Topico");
        }
        topico.setTitulo(titulo);
        topico.setDescricao(descricao);
    }

    private void registrar(Long idUsuario, String tipo, String detalhes) {
        if (auditoria != null)
            auditoria.registrar(idUsuario, tipo, detalhes, null);
    }
}
