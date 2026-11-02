package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.repository.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SenhaUsuarioServico {
    @Autowired
    private UsuarioRepositorio repositorio;
    @Autowired
    private PasswordEncoder codificadorSenha;
    @Autowired
    private LogAuditoriaServico auditoria;
    @Autowired
    private TokenEmailServico tokensEmail;
    @Autowired
    private EmailServico emails;

    // Envia o link para alterar a senha.
    public void solicitarAlteracaoSenha(String email) {
        repositorio.findByEmail(email).filter(usuario -> usuario.isAtivo()).ifPresent(usuario -> {
            String token = tokensEmail.criarAlteracaoSenha(usuario);
            emails.enviarRedefinicaoSenha(usuario, token);
        });
    }

    @Transactional
    // Valida o token e salva a nova senha do usuario.
    public void alterarSenha(String token, String novaSenha, String confirmacaoSenha) {
        if (!novaSenha.equals(confirmacaoSenha))
            throw new IllegalArgumentException("As senhas nao conferem");
        var registro = tokensEmail.validar(token, tokensEmail.finalidadeAlteracaoSenha());
        var usuario = registro.getUsuario();
        usuario.setSenha(codificadorSenha.encode(novaSenha));
        repositorio.save(usuario);
        auditoria.registrar(usuario.getId(), "ALTERACAO_SENHA", "Senha alterada", null);
    }
}