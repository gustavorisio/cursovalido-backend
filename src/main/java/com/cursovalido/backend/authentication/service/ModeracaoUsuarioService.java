package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.*;
import com.cursovalido.backend.authentication.entity.*;
import com.cursovalido.backend.authentication.repository.*;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ModeracaoUsuarioService {
    private final UsuarioRepository usuarios;
    private final IdentidadeExcluidaService identidades;
    private final SuspensaoUsuarioRepository suspensoes;
    private final PrivilegiosAdministrativosService privilegios;
    private final LogAuditoriaService auditoria;
    private final SessaoRepository sessoes;
    private final TokenEmailService tokensEmail;

    public List<SuspensaoUsuario> vigentes(Long idUsuario) {
        Instant agora = Instant.now();
        var ids = new java.util.ArrayList<Long>();
        ids.add(idUsuario);
        usuarios.findById(idUsuario).ifPresent(u -> ids.addAll(identidades.anteriores(u.getEmail(), u.getCpf())));
        return suspensoes.findByIdUsuarioInAndRevogadaEmIsNullOrderByIdDesc(ids).stream()
                .filter(item -> item.vigente(agora)).toList();
    }

    public boolean restrito(Long idUsuario, EscopoSuspensao escopo) {
        return vigentes(idUsuario).stream().anyMatch(item -> item.getEscopo() == escopo);
    }

    public String mensagemBanimento(Long idUsuario, EscopoSuspensao escopo) {
        var aplicaveis = vigentes(idUsuario).stream().filter(item -> {
            if (escopoDeConteudo(escopo)) {
                return escopoDeConteudo(item.getEscopo());
            }
            return item.getEscopo() == escopo;
        }).toList();
        String acesso = escopo == EscopoSuspensao.CONTA ? " da plataforma"
                : escopo == EscopoSuspensao.FORUM ? " do fórum" : " dos tópicos e comentários do fórum";
        if (aplicaveis.stream().anyMatch(item -> item.getExpiraEm() == null)) {
            return "Você foi banido permanentemente" + acesso + ".";
        }
        var fim = aplicaveis.stream().map(SuspensaoUsuario::getExpiraEm).max(Instant::compareTo).orElse(null);
        if (fim == null) return "Seu acesso está restrito.";
        String data = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")
                .withZone(java.time.ZoneId.of("America/Sao_Paulo")).format(fim);
        return "Você foi banido temporariamente" + acesso + " até " + data + " (horário de Brasília).";
    }

    @Transactional
    public SuspensaoResposta aplicar(Long idUsuario, SuspensaoRequisicao dados, Long idAdministrador) {
        verificarAlvo(idUsuario, idAdministrador);
        if (dados.expiraEm() != null) {
            if (!dados.expiraEm().isAfter(Instant.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A expiracao deve estar no futuro");
            }
        }
        for (var existente : vigentes(idUsuario)) {
            if (substituiSuspensao(dados.escopo(), existente.getEscopo())) {
                existente.setRevogadaEm(Instant.now());
            }
        }
        SuspensaoUsuario suspensao = new SuspensaoUsuario();
        suspensao.setIdUsuario(idUsuario);
        suspensao.setIdAdministrador(idAdministrador);
        suspensao.setEscopo(dados.escopo());
        suspensao.setMotivo(dados.motivo().trim());
        suspensao.setExpiraEm(dados.expiraEm());
        suspensoes.save(suspensao);
        sessoes.revogarPorUsuario(idUsuario);
        tokensEmail.invalidarAcessos(usuarios.findById(idUsuario).orElseThrow());
        auditoria.registrar(idAdministrador, "SUSPENSAO_APLICADA", "Usuario " + idUsuario + ": " + dados.escopo(), null);
        return SuspensaoResposta.de(suspensao);
    }

    @Transactional
    public void revogar(Long idUsuario, Long idSuspensao, Long idAdministrador) {
        verificarAlvo(idUsuario, idAdministrador);
        var suspensao = suspensoes.findById(idSuspensao)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Suspensao nao encontrada"));
        if (!suspensaoPertenceAoUsuario(suspensao, idUsuario)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Suspensao nao encontrada");
        }
        if (suspensao.getRevogadaEm() != null) return;
        suspensao.setRevogadaEm(Instant.now());
        auditoria.registrar(idAdministrador, "SUSPENSAO_REVOGADA", "Usuario " + idUsuario + ": " + idSuspensao, null);
    }

    private boolean escopoDeConteudo(EscopoSuspensao escopo) {
        if (escopo == EscopoSuspensao.TOPICOS) {
            return true;
        }
        return escopo == EscopoSuspensao.COMENTARIOS;
    }

    private boolean substituiSuspensao(EscopoSuspensao novo, EscopoSuspensao anterior) {
        if (novo == anterior) {
            return true;
        }
        if (!escopoDeConteudo(novo)) {
            return false;
        }
        return escopoDeConteudo(anterior);
    }

    private boolean suspensaoPertenceAoUsuario(SuspensaoUsuario suspensao, Long idUsuario) {
        if (suspensao.getIdUsuario().equals(idUsuario)) {
            return true;
        }
        for (SuspensaoUsuario vigente : vigentes(idUsuario)) {
            if (vigente.getId().equals(suspensao.getId())) {
                return true;
            }
        }
        return false;
    }

    private void verificarAlvo(Long idUsuario, Long idAdministrador) {
        var administrador = privilegios.verificarAdministrador(idAdministrador);
        var alvo = usuarios.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));
        if (idUsuario.equals(idAdministrador)) throw negado();
        if (privilegios.ehPrincipal(alvo)) throw negado();
        if (alvo.getPerfil() == Perfil.ADMINISTRADOR) {
            if (!privilegios.ehPrincipal(administrador)) throw negado();
        }
    }

    private ResponseStatusException negado() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Voce nao pode moderar esta conta");
    }
}
