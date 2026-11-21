package com.cursovalido.backend.forum.service;

import lombok.RequiredArgsConstructor;

import com.cursovalido.backend.forum.entity.UsuarioForum;
import com.cursovalido.backend.forum.repository.UsuarioForumRepository;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.security.AnonimizacaoDados;
import com.cursovalido.backend.authentication.security.VisibilidadeEmail;
import com.cursovalido.backend.forum.exception.RecursoNaoEncontradoException;
import com.cursovalido.backend.forum.exception.RequisicaoInvalidaException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioForumService {
    private final UsuarioForumRepository repositorioUsuarios;

    // Busca um usuario ativo para as operacoes do forum.
    public UsuarioForum buscarAtivo(Long idUsuario) {
        if (idUsuario == null) {
            throw RequisicaoInvalidaException.usuarioNaoInformado();
        }
        return repositorioUsuarios.findById(idUsuario).filter(UsuarioForum::isAtivo)
                .orElseThrow(RecursoNaoEncontradoException::usuarioNaoEncontrado);
    }

    public UsuarioForum bloquearAtivo(Long idUsuario) {
        if (idUsuario == null) {
            throw RequisicaoInvalidaException.usuarioNaoInformado();
        }
        return repositorioUsuarios.bloquearPorId(idUsuario).filter(UsuarioForum::isAtivo)
                .orElseThrow(RecursoNaoEncontradoException::usuarioNaoEncontrado);
    }

    // Lista os usuarios ativos que aparecem no forum.
    public List<UsuarioForum> listarAtivos() {
        return repositorioUsuarios.findByAtivoTrueOrderByIdAsc();
    }

    // Atualiza no forum somente o necessario para exibir a autoria: alunos aparecem com
    // nome abreviado e o e-mail fica mascarado.
    public void sincronizar(Usuario usuario) {
        String nome = usuario.getPerfil() == Perfil.ALUNO
                ? AnonimizacaoDados.abreviarNome(usuario.getNomeCompleto())
                : usuario.getNomeCompleto();
        repositorioUsuarios.sincronizar(usuario.getId(), nome, VisibilidadeEmail.mascarar(usuario.getEmail()),
                usuario.getPerfil().name(), usuario.isAtivo());
    }

    // Verifica se o usuario possui o perfil de administrador.
    public boolean ehAdministrador(UsuarioForum usuario) {
        return "ADMINISTRADOR".equalsIgnoreCase(usuario.getPerfil());
    }
}
