package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.entity.AceiteTermoArquivado;
import com.cursovalido.backend.authentication.exception.TermosNaoDisponiveisException;
import com.cursovalido.backend.authentication.repository.AceiteTermoArquivadoRepositorio;
import com.cursovalido.backend.authentication.repository.UsuarioRepositorio;
import com.cursovalido.backend.forum.service.UsuarioForumServico;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TermosUsuarioServico {
    @Autowired
    private UsuarioRepositorio repositorio;
    @Autowired
    private UsuarioForumServico usuariosForum;
    @Autowired
    private PoliticaServico politicas;
    @Autowired
    private LogAuditoriaServico auditoria;

    @Transactional
    // Bloqueia a conta quando o usuario revoga os termos.
    public void revogarTermos(Long idUsuario) {
        Usuario usuario = buscarUsuario(idUsuario);
        usuario.setAceitouTermosLgpd(false);
        usuario.setAtivo(false);
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
        usuario.setAceitouTermosLgpd(true);
        usuario.setAtivo(true);
        usuario.setVersaoTermos(versaoTermos);
        usuario.setDataHoraAceite(java.time.LocalDateTime.now());
        repositorio.save(usuario);
        usuariosForum.sincronizar(usuario);
        auditoria.registrar(idUsuario, "ACEITE_TERMOS_NOVAMENTE", "Termos aceitos novamente", null);
    }

    private Usuario buscarUsuario(Long idUsuario) {
        return repositorio.findById(idUsuario)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));
    }
}