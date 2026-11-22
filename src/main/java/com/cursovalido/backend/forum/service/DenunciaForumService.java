package com.cursovalido.backend.forum.service;

import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import com.cursovalido.backend.authentication.service.ModeracaoUsuarioService;
import com.cursovalido.backend.authentication.dto.SuspensaoRequest;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import com.cursovalido.backend.forum.dto.DenunciaDtos.DecisaoDenunciaRequest;
import com.cursovalido.backend.forum.dto.DenunciaDtos.DenunciaRegistradaResponse;
import com.cursovalido.backend.forum.dto.DenunciaDtos.DenunciaRequest;
import com.cursovalido.backend.forum.dto.DenunciaDtos.DenunciaResponse;
import com.cursovalido.backend.forum.entity.Comentario;
import com.cursovalido.backend.forum.entity.DenunciaForum;
import com.cursovalido.backend.forum.entity.StatusDenuncia;
import com.cursovalido.backend.forum.entity.TipoConteudoForum;
import com.cursovalido.backend.forum.entity.Topico;
import com.cursovalido.backend.forum.entity.UsuarioForum;
import com.cursovalido.backend.forum.exception.ConflitoException;
import com.cursovalido.backend.forum.exception.RecursoNaoEncontradoException;
import com.cursovalido.backend.forum.exception.RequisicaoInvalidaException;
import com.cursovalido.backend.forum.repository.ComentarioRepository;
import com.cursovalido.backend.forum.repository.DenunciaForumRepository;
import com.cursovalido.backend.forum.repository.TopicoRepository;
import java.util.List;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DenunciaForumService {
    private static final int TAMANHO_TRECHO = 300;

    private final DenunciaForumRepository denuncias;
    private final TopicoRepository topicos;
    private final ComentarioRepository comentarios;
    private final UsuarioForumService usuariosForum;
    private final PrivilegiosAdministrativosService privilegios;
    private final LogAuditoriaService auditoria;
    private final ModeracaoUsuarioService moderacao;

    @Transactional
    public DenunciaRegistradaResponse denunciar(TipoConteudoForum tipo, Long idConteudo, DenunciaRequest requisicao,
            Long idUsuario) {
        UsuarioForum denunciante = usuariosForum.bloquearAtivo(idUsuario);
        Long idAutor = tipo == TipoConteudoForum.TOPICO ? topicoAtivo(idConteudo).getIdAutor()
                : comentarioAtivo(idConteudo).getIdAutor();
        if (denunciante.getId().equals(idAutor))
            throw new RequisicaoInvalidaException("Você não pode denunciar o próprio conteúdo.");
        if (denuncias.existsByIdDenuncianteAndIdAutorConteudoAndCriadaEmAfter(idUsuario, idAutor,
                LocalDateTime.now().minusMinutes(10))) {
            throw new ConflitoException(
                    "você já denunciou este usuário, aguarde um tempo antes de denunciá-lo novamente.");
        }
        String descricao = requisicao.descricao() == null || requisicao.descricao().isBlank() ? null
                : requisicao.descricao().trim();
        DenunciaForum denuncia = denuncias.save(new DenunciaForum(tipo, idConteudo, idAutor, idUsuario,
                requisicao.motivo(), descricao));
        auditoria.registrar(idUsuario, "DENUNCIA_FORUM_REGISTRADA",
                tipo + " " + idConteudo + "; motivo " + requisicao.motivo(), null);
        return new DenunciaRegistradaResponse(denuncia.getId(), denuncia.getStatus(),
                "Denúncia enviada. A moderação vai analisar o conteúdo.");
    }

    @Transactional(readOnly = true)
    public List<DenunciaResponse> listar(Long idAdministrador, StatusDenuncia status) {
        privilegios.verificarAdministrador(idAdministrador);
        StatusDenuncia filtro = status == null ? StatusDenuncia.PENDENTE : status;
        return denuncias.findByStatusOrderByCriadaEmAscIdAsc(filtro).stream().map(this::converter).toList();
    }

    // Uma decisao com remocao encerra todas as denuncias pendentes do mesmo conteudo.
    @Transactional
    public DenunciaResponse decidir(Long idDenuncia, DecisaoDenunciaRequest decisao, Long idAdministrador) {
        privilegios.verificarAdministrador(idAdministrador);
        DenunciaForum denuncia = denuncias.findById(idDenuncia)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Denuncia nao encontrada"));
        if (denuncia.getStatus() != StatusDenuncia.PENDENTE)
            throw new ConflitoException("Esta denúncia já foi analisada.");
        boolean procedente = Boolean.TRUE.equals(decisao.procedente());
        boolean remover = procedente && Boolean.TRUE.equals(decisao.removerConteudo());
        if (Boolean.TRUE.equals(decisao.aplicarRestricao())) {
            if (!procedente) throw new RequisicaoInvalidaException("A restrição só pode ser aplicada a uma denúncia procedente.");
            if (decisao.escopoRestricao() != com.cursovalido.backend.authentication.entity.EscopoSuspensao.CONTA
                    && decisao.escopoRestricao() != com.cursovalido.backend.authentication.entity.EscopoSuspensao.FORUM) {
                throw new RequisicaoInvalidaException("Escolha acesso à conta ou ao fórum para a restrição.");
            }
            moderacao.aplicar(denuncia.getIdAutorConteudo(),
                    new SuspensaoRequest(decisao.escopoRestricao(), decisao.observacao(), decisao.expiraEm()), idAdministrador);
        }
        String observacao = decisao.observacao().trim();
        if (remover) {
            removerConteudo(denuncia);
            denuncias.findByTipoAndIdConteudoAndStatus(denuncia.getTipo(), denuncia.getIdConteudo(),
                    StatusDenuncia.PENDENTE)
                    .forEach(item -> item.decidir(true, true, observacao, idAdministrador));
        } else {
            denuncia.decidir(procedente, false, observacao, idAdministrador);
        }
        auditoria.registrar(idAdministrador, "DENUNCIA_FORUM_DECIDIDA", "Denuncia " + idDenuncia + "; "
                + (procedente ? "procedente" : "improcedente") + (remover ? "; conteudo removido" : ""), null);
        return converter(denuncia);
    }

    private void removerConteudo(DenunciaForum denuncia) {
        if (denuncia.getTipo() == TipoConteudoForum.TOPICO) {
            topicos.findById(denuncia.getIdConteudo()).ifPresent(topico -> {
                topico.setAtivo(false);
                topicos.save(topico);
            });
            return;
        }
        comentarios.findById(denuncia.getIdConteudo()).ifPresent(comentario -> {
            comentario.setAtivo(false);
            comentarios.save(comentario);
        });
    }

    private DenunciaResponse converter(DenunciaForum denuncia) {
        String trecho;
        boolean ativo;
        Long idTopico;
        String nomeAutor;
        String gamertagAutor;
        if (denuncia.getTipo() == TipoConteudoForum.TOPICO) {
            Topico topico = topicos.findById(denuncia.getIdConteudo()).orElse(null);
            trecho = topico == null ? null : resumir(topico.getTitulo() + " - " + topico.getDescricao());
            ativo = topico != null && topico.isAtivo();
            idTopico = denuncia.getIdConteudo();
            nomeAutor = topico == null ? null : topico.getNomeAutor();
            gamertagAutor = topico == null ? null : topico.getGamertagAutor();
        } else {
            Comentario comentario = comentarios.findById(denuncia.getIdConteudo()).orElse(null);
            trecho = comentario == null ? null : resumir(comentario.getConteudo());
            ativo = comentario != null && comentario.isAtivo();
            idTopico = comentario == null || comentario.getTopico() == null ? null : comentario.getTopico().getId();
            nomeAutor = comentario == null ? null : comentario.getNomeAutor();
            gamertagAutor = comentario == null ? null : comentario.getGamertagAutor();
        }
        return new DenunciaResponse(denuncia.getId(), denuncia.getTipo(), denuncia.getIdConteudo(), idTopico, trecho,
                ativo, denuncia.getIdAutorConteudo(), nomeAutor, gamertagAutor, denuncia.getMotivo(), denuncia.getDescricao(),
                denuncias.countByTipoAndIdConteudoAndStatus(denuncia.getTipo(), denuncia.getIdConteudo(),
                        StatusDenuncia.PENDENTE),
                denuncia.getStatus(), denuncia.getCriadaEm(), denuncia.getDecididaEm(),
                denuncia.getObservacaoModeracao());
    }

    private Topico topicoAtivo(Long idTopico) {
        Topico topico = topicos.findById(idTopico).orElseThrow(RecursoNaoEncontradoException::topicoNaoEncontrado);
        if (!topico.isAtivo())
            throw RecursoNaoEncontradoException.topicoArquivado();
        return topico;
    }

    private Comentario comentarioAtivo(Long idComentario) {
        Comentario comentario = comentarios.findById(idComentario)
                .orElseThrow(RecursoNaoEncontradoException::comentarioNaoEncontrado);
        if (!comentario.isAtivo())
            throw RecursoNaoEncontradoException.comentarioNaoEncontrado();
        return comentario;
    }

    private String resumir(String texto) {
        if (texto == null)
            return null;
        return texto.length() <= TAMANHO_TRECHO ? texto : texto.substring(0, TAMANHO_TRECHO) + "...";
    }
}
