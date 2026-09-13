package com.cursovalido.backend.Forum.servico;

import com.cursovalido.backend.Forum.entidade.UsuarioForum;
import com.cursovalido.backend.Forum.repositorio.UsuarioForumRepositorio;
import com.cursovalido.backend.Forum.excecao.RecursoNaoEncontradoException;
import com.cursovalido.backend.Forum.excecao.RequisicaoInvalidaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioForumServico {
    @Autowired
    private UsuarioForumRepositorio repositorioUsuarios;

    public UsuarioForum buscarAtivo(Long idUsuario) {
        if (idUsuario == null) {
            throw new RequisicaoInvalidaException("Informe o usuario no header X-User-Id");
        }
        return repositorioUsuarios.findById(idUsuario).filter(UsuarioForum::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
    }

    public List<UsuarioForum> listarAtivos() {
        return repositorioUsuarios.findByAtivoTrueOrderByIdAsc();
    }

    public boolean ehAdministrador(UsuarioForum usuario) {
        return "ADMINISTRADOR".equalsIgnoreCase(usuario.getPerfil());
    }
}