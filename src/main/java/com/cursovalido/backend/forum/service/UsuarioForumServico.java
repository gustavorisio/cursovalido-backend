package com.cursovalido.backend.forum.service;

import com.cursovalido.backend.forum.entity.UsuarioForum;
import com.cursovalido.backend.forum.repository.UsuarioForumRepositorio;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.forum.exception.RecursoNaoEncontradoException;
import com.cursovalido.backend.forum.exception.RequisicaoInvalidaException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class UsuarioForumServico {
    @Autowired
    private UsuarioForumRepositorio repositorioUsuarios;

    public UsuarioForumServico() {
    }

    public UsuarioForum buscarAtivo(Long idUsuario) {
        if (idUsuario == null) {
            throw RequisicaoInvalidaException.usuarioNaoInformado();
        }
        return repositorioUsuarios.findById(idUsuario).filter(UsuarioForum::isAtivo)
                .orElseThrow(RecursoNaoEncontradoException::usuarioNaoEncontrado);
    }

    public List<UsuarioForum> listarAtivos() {
        return repositorioUsuarios.findByAtivoTrueOrderByIdAsc();
    }

    public void sincronizar(Usuario usuario) {
        repositorioUsuarios.sincronizar(usuario.getId(), usuario.getNomeCompleto(), usuario.getEmail(),
                usuario.getPerfil().name(), usuario.isAtivo());
    }

    public boolean ehAdministrador(UsuarioForum usuario) {
        return "ADMINISTRADOR".equalsIgnoreCase(usuario.getPerfil());
    }
}