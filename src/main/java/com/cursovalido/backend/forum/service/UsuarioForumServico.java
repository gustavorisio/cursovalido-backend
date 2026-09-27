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

    // Busca um usuario ativo para as operacoes do forum.
    public UsuarioForum buscarAtivo(Long idUsuario) {
        if (idUsuario == null) {
            throw RequisicaoInvalidaException.usuarioNaoInformado();
        }
        return repositorioUsuarios.findById(idUsuario).filter(UsuarioForum::isAtivo)
                .orElseThrow(RecursoNaoEncontradoException::usuarioNaoEncontrado);
    }

    // Lista os usuarios ativos que aparecem no forum.
    public List<UsuarioForum> listarAtivos() {
        return repositorioUsuarios.findByAtivoTrueOrderByIdAsc();
    }

    // Atualiza no forum os dados do usuario principal.
    public void sincronizar(Usuario usuario) {
        repositorioUsuarios.sincronizar(usuario.getId(), usuario.getNomeCompleto(), usuario.getEmail(),
                usuario.getPerfil().name(), usuario.isAtivo());
    }

    // Verifica se o usuario possui o perfil de administrador.
    public boolean ehAdministrador(UsuarioForum usuario) {
        return "ADMINISTRADOR".equalsIgnoreCase(usuario.getPerfil());
    }
}