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
public class ModeracaoUsuarioServico {
    private final UsuarioRepositorio usuarios;
    private final IdentidadeExcluidaServico identidades;
    private final SuspensaoUsuarioRepositorio suspensoes;
    private final PrivilegiosAdministrativosServico privilegios;
    private final LogAuditoriaServico auditoria;
    private final SessaoRepositorio sessoes;
    private final TokenEmailServico tokensEmail;

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
            if (escopo == EscopoSuspensao.TOPICOS || escopo == EscopoSuspensao.COMENTARIOS) {
                return item.getEscopo() == EscopoSuspensao.TOPICOS || item.getEscopo() == EscopoSuspensao.COMENTARIOS;
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
            boolean conteudo = dados.escopo() == EscopoSuspensao.TOPICOS || dados.escopo() == EscopoSuspensao.COMENTARIOS;
            boolean conteudoExistente = existente.getEscopo() == EscopoSuspensao.TOPICOS
                    || existente.getEscopo() == EscopoSuspensao.COMENTARIOS;
            if (existente.getEscopo() == dados.escopo() || (conteudo && conteudoExistente)) {
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
        if (!suspensao.getIdUsuario().equals(idUsuario) && vigentes(idUsuario).stream().noneMatch(s -> s.getId().equals(idSuspensao))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Suspensao nao encontrada");
        }
        if (suspensao.getRevogadaEm() != null) return;
        suspensao.setRevogadaEm(Instant.now());
        auditoria.registrar(idAdministrador, "SUSPENSAO_REVOGADA", "Usuario " + idUsuario + ": " + idSuspensao, null);
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
