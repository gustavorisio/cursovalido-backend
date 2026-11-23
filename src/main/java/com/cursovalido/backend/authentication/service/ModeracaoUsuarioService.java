package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.*;
import com.cursovalido.backend.authentication.entity.*;
import com.cursovalido.backend.authentication.repository.*;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import com.cursovalido.backend.authentication.validation.moderacao.ModeracaoValidation;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    @Transactional(readOnly = true)
    public List<UsuarioAdministracaoResposta> listarUsuarios(Long idAdministrador, Boolean restrito) {
        Usuario administrador = privilegios.verificarAdministrador(idAdministrador);
        boolean administradorPrincipal = privilegios.ehPrincipal(administrador);
        return usuarios.findByExcluidoEmIsNull().stream()
                .map(usuario -> resumirUsuario(administrador, administradorPrincipal, usuario))
                .filter(usuario -> restrito == null || restrito == !usuario.suspensoes().isEmpty())
                .toList();
    }

    public List<SuspensaoUsuario> vigentes(Long idUsuario) {
        return vigentes(idUsuario, usuarios.findById(idUsuario).orElse(null));
    }

    public boolean restrito(Long idUsuario, EscopoSuspensao escopo) {
        return vigentes(idUsuario).stream().anyMatch(item -> item.getEscopo() == escopo);
    }

    public String mensagemBanimento(Long idUsuario, EscopoSuspensao escopo) {
        var aplicaveis = vigentes(idUsuario).stream().filter(item -> item.getEscopo() == escopo).toList();
        String acesso = escopo == EscopoSuspensao.CONTA ? " da plataforma"
                : " do fórum";
        if (aplicaveis.stream().anyMatch(item -> item.getExpiraEm() == null)) {
            return "Você foi banido permanentemente" + acesso + ".";
        }
        var fim = aplicaveis.stream().map(SuspensaoUsuario::getExpiraEm).max(Instant::compareTo).orElse(null);
        if (fim == null)
            return "Seu acesso está restrito.";
        String data = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")
                .withZone(ZoneId.of("America/Sao_Paulo")).format(fim);
        return "Você foi banido temporariamente" + acesso + " até " + data + " (horário de Brasília).";
    }

    @Transactional
    public SuspensaoResponse aplicar(Long idUsuario, SuspensaoRequest dados, Long idAdministrador) {
        if (dados.escopo() != EscopoSuspensao.CONTA && dados.escopo() != EscopoSuspensao.FORUM) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A restrição deve ser para conta ou fórum");
        }
        Usuario alvo = ModeracaoValidation.verificarAlvo(idUsuario, idAdministrador, privilegios, usuarios);
        ModeracaoValidation.validarExpiracao(dados);
        for (var existente : vigentes(idUsuario, alvo)) {
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
        tokensEmail.invalidarAcessos(alvo);
        auditoria.registrar(idAdministrador, "SUSPENSAO_APLICADA", "Usuario " + idUsuario + ": " + dados.escopo(),
                null);
        return SuspensaoResponse.de(suspensao);
    }

    @Transactional
    public void revogar(Long idUsuario, Long idSuspensao, Long idAdministrador) {
        ModeracaoValidation.verificarAlvo(idUsuario, idAdministrador, privilegios, usuarios);
        var suspensao = suspensoes.findById(idSuspensao)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Suspensao nao encontrada"));
        ModeracaoValidation.validarVinculo(suspensaoPertenceAoUsuario(suspensao, idUsuario));
        if (suspensao.getRevogadaEm() != null)
            return;
        suspensao.setRevogadaEm(Instant.now());
        auditoria.registrar(idAdministrador, "SUSPENSAO_REVOGADA", "Usuario " + idUsuario + ": " + idSuspensao, null);
    }

    private UsuarioAdministracaoResposta resumirUsuario(Usuario administrador, boolean administradorPrincipal,
            Usuario usuario) {
        boolean podeModerar = ModeracaoValidation.podeModerar(administrador, usuario, administradorPrincipal,
                privilegios);
        boolean podeExcluir = podeModerar && administradorPrincipal;
        return new UsuarioAdministracaoResposta(usuario.getId(), usuario.getNomeCompleto(), usuario.getGamertag(),
                usuario.getPerfil(), privilegios.ehPrincipal(usuario), usuario.isAtivo(), podeModerar, podeExcluir,
                vigentes(usuario.getId(), usuario).stream().map(SuspensaoResponse::de).toList());
    }

    private List<SuspensaoUsuario> vigentes(Long idUsuario, Usuario usuario) {
        Instant agora = Instant.now();
        var ids = new ArrayList<Long>();
        ids.add(idUsuario);
        if (usuario != null) {
            ids.addAll(identidades.anteriores(usuario.getEmail()));
        }
        return suspensoes.findByIdUsuarioInAndRevogadaEmIsNullOrderByIdDesc(ids).stream()
                .filter(item -> item.vigente(agora)).toList();
    }

    private boolean substituiSuspensao(EscopoSuspensao novo, EscopoSuspensao anterior) {
        return novo == anterior;
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
}
