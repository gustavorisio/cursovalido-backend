package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.AceiteTermoArquivado;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.exception.TermosNaoDisponiveisException;
import com.cursovalido.backend.authentication.repository.AceiteTermoArquivadoRepository;
import com.cursovalido.backend.authentication.repository.SessaoRepository;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.forum.service.UsuarioForumServico;
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
    private final UsuarioForumServico usuariosForum;
    private final PoliticaService politicas;
    private final LogAuditoriaService auditoria;
    private final SessaoRepository sessoes;
    private final TokenEmailService tokensEmail;

    @Transactional
    // Bloqueia a conta quando o usuario revoga os termos.
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
    // Registra o aceite de uma nova versao dos termos.
    public void aceitarTermosNovamente(Long idUsuario, String versaoTermos) {
        if (!politicas.versaoAtiva(versaoTermos))
            throw new TermosNaoDisponiveisException();
        Usuario usuario = buscarUsuario(idUsuario);
        if (possuiAceiteRegistrado(usuario)) {
            aceitesArquivados.save(new AceiteTermoArquivado(idUsuario, usuario.getVersaoTermos(),
                usuario.getDataHoraAceite(), "NOVA_VERSAO_DOCUMENTOS"));
        }
        usuario.setAceitouTermosLgpd(true);
        usuario.setVersaoTermos(versaoTermos);
        usuario.setDataHoraAceite(LocalDateTime.now());
        repositorio.save(usuario);
        usuariosForum.sincronizar(usuario);
        auditoria.registrar(idUsuario, "ACEITE_TERMOS_NOVAMENTE", "Termos aceitos novamente", null);
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
        return repositorio.findById(idUsuario)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));
    }
}
