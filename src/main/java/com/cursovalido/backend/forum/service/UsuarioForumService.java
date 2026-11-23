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

    // Busca um usuário ativo para as operações do fórum.
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

    // Lista os usuários ativos que aparecem no fórum.
    public List<UsuarioForum> listarAtivos() {
        return repositorioUsuarios.findByAtivoTrueOrderByIdAsc();
    }

    // Sincroniza a autoria com nome abreviado para alunos e e-mail mascarado.
    public void sincronizar(Usuario usuario) {
        String nome = usuario.getPerfil() == Perfil.ALUNO
                ? AnonimizacaoDados.abreviarNome(usuario.getNomeCompleto())
                : usuario.getNomeCompleto();
        repositorioUsuarios.sincronizar(usuario.getId(), nome, VisibilidadeEmail.mascarar(usuario.getEmail()),
                usuario.getPerfil().name(), usuario.isAtivo(), usuario.getGamertag());
    }

    // Verifica se o usuário possui o perfil de administrador.
    public boolean ehAdministrador(UsuarioForum usuario) {
        return "ADMINISTRADOR".equalsIgnoreCase(usuario.getPerfil());
    }
}
