package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.AceiteTermoArquivado;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.AceiteTermoArquivadoRepositorio;
import com.cursovalido.backend.authentication.repository.LogAuditoriaRepositorio;
import com.cursovalido.backend.authentication.repository.TokenEmailRepositorio;
import com.cursovalido.backend.authentication.repository.UsuarioRepositorio;
import com.cursovalido.backend.forum.repository.ComentarioRepositorio;
import com.cursovalido.backend.forum.repository.TopicoRepositorio;
import com.cursovalido.backend.forum.repository.UsuarioForumRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContaUsuarioServico {
    @Autowired
    private UsuarioRepositorio repositorio;
    @Autowired
    private TokenEmailRepositorio repositorioTokens;
    @Autowired
    private LogAuditoriaRepositorio repositorioAuditoria;
    @Autowired
    private AceiteTermoArquivadoRepositorio repositorioAceitesArquivados;
    @Autowired
    private ComentarioRepositorio comentarios;
    @Autowired
    private TopicoRepositorio topicos;
    @Autowired
    private UsuarioForumRepositorio usuariosForumRepositorio;

    @Transactional
    // Remove a conta e trata os registros ligados ao usuario.
    public void excluirConta(Long idUsuario) {
        Usuario usuario = repositorio.findById(idUsuario)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));
        var topicosDoUsuario = topicos.findByIdAutor(idUsuario);
        for (var topico : topicosDoUsuario)
            comentarios.deleteByTopicoId(topico.getId());
        comentarios.deleteByIdAutor(idUsuario);
        topicosDoUsuario.forEach(topico -> topicos.deleteById(topico.getId()));
        repositorioTokens.deleteByUsuarioId(idUsuario);
        repositorioAceitesArquivados.save(new AceiteTermoArquivado(
                usuario.getId(), usuario.getVersaoTermos(), usuario.getDataHoraAceite(), "EXCLUSAO_CONTA"));
        repositorioAuditoria.anonimizarPorUsuarioId(idUsuario);
        usuariosForumRepositorio.deleteById(idUsuario);
        repositorio.deleteById(idUsuario);
    }
}