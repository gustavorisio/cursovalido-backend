package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.dto.AceiteTermosRequest;
import com.cursovalido.backend.authentication.dto.AutenticacaoResponse;
import com.cursovalido.backend.authentication.entity.AceiteTermoArquivado;
import com.cursovalido.backend.authentication.exception.CredenciaisInvalidasException;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.AceiteTermoArquivadoRepository;
import com.cursovalido.backend.authentication.repository.SessaoRepository;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.validation.termos.TermosValidation;
import com.cursovalido.backend.forum.service.UsuarioForumService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TermosUsuarioService {
    private final UsuarioRepository repositorio;
    private final AceiteTermoArquivadoRepository aceitesArquivados;
    private final UsuarioForumService usuariosForum;
    private final PoliticaService politicas;
    private final ResponsabilidadeService responsabilidade;
    private final LogAuditoriaService auditoria;
    private final SessaoRepository sessoes;
    private final TokenEmailService tokensEmail;
    private final TokenJwtService tokens;

    @Transactional
    // Bloqueia a conta quando o usuário revoga os termos.
    public void revogarTermos(Long idUsuario) {
        Usuario usuario = buscarUsuario(idUsuario);
        if (possuiAceiteRegistrado(usuario))
            aceitesArquivados.save(new AceiteTermoArquivado(idUsuario, usuario.getVersaoTermos(),
                    usuario.getDataHoraAceite(), "REVOGACAO_TERMOS"));
        usuario.setAceitouTermosLgpd(false);
        sessoes.revogarPorUsuario(idUsuario);
        tokensEmail.invalidarAcessos(usuario);
        repositorio.save(usuario);
        usuariosForum.sincronizar(usuario);
        auditoria.registrar(idUsuario, "REVOGACAO_TERMOS", "Aceite revogado e acesso bloqueado", null);
    }

    @Transactional
    public void encerrarPorDecisao(Long idUsuario, String token, boolean aceitou, String enderecoIp) {
        buscarUsuario(idUsuario);
        if (tokens.apenasTermos(token) && aceitou) {
            throw new IllegalArgumentException("Informe a versao dos termos para confirmar o aceite.");
        }
        tokens.encerrar(token);
        auditoria.registrar(idUsuario, aceitou ? "TERMOS_REAUTENTICACAO" : "TERMOS_RECUSADOS",
                aceitou ? "Nova autenticacao solicitada" : "Sessao encerrada apos recusa dos termos", enderecoIp);
    }

    @Transactional
    public AutenticacaoResponse confirmarAceite(Long idUsuario, String token, AceiteTermosRequest requisicao,
            String enderecoIp) {
        Usuario usuario = buscarUsuario(idUsuario);
        if (!tokens.apenasTermos(token)) {
            throw new CredenciaisInvalidasException();
        }
        registrarAceite(usuario, requisicao.versaoTermos(), requisicao.aceitouResponsabilidade(),
                requisicao.versaoResponsabilidade(), enderecoIp);
        return new AutenticacaoResponse(tokens.gerar(usuario), usuario.getId(), usuario.getNomeCompleto(),
                usuario.getPerfil(), false);
    }

    @Transactional
    public void aceitarTermosNovamente(Long idUsuario, String versaoTermos, Boolean aceitouResponsabilidade,
            String versaoResponsabilidade, String enderecoIp) {
        registrarAceite(buscarUsuario(idUsuario), versaoTermos, aceitouResponsabilidade,
                versaoResponsabilidade, enderecoIp);
    }

    private void registrarAceite(Usuario usuario, String versaoTermos, Boolean aceitouResponsabilidade,
            String versaoResponsabilidade, String enderecoIp) {
        TermosValidation.validarVersao(versaoTermos, politicas);
        Long idUsuario = usuario.getId();
        responsabilidade.registrar(usuario, aceitouResponsabilidade, versaoResponsabilidade, enderecoIp);
        if (possuiAceiteRegistrado(usuario)) {
            aceitesArquivados.save(new AceiteTermoArquivado(idUsuario, usuario.getVersaoTermos(),
                    usuario.getDataHoraAceite(), "NOVA_VERSAO_DOCUMENTOS"));
        }
        usuario.setAceitouTermosLgpd(true);
        usuario.setVersaoTermos(versaoTermos);
        usuario.setDataHoraAceite(LocalDateTime.now());
        repositorio.save(usuario);
        usuariosForum.sincronizar(usuario);
        auditoria.registrar(idUsuario, "ACEITE_TERMOS_NOVAMENTE", versaoTermos, enderecoIp);
    }

    private boolean possuiAceiteRegistrado(Usuario usuario) {
        if (!Boolean.TRUE.equals(usuario.getAceitouTermosLgpd())) {
            return false;
        }
        if (usuario.getVersaoTermos() == null) {
            return false;
        }
        return usuario.getDataHoraAceite() != null;
    }

    private Usuario buscarUsuario(Long idUsuario) {
        return repositorio.buscarParaAtualizar(idUsuario)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));
    }
}
