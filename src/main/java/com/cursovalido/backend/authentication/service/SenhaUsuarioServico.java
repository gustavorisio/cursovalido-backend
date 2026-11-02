package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.TokenEmail;
import com.cursovalido.backend.authentication.repository.SessaoRepositorio;
import com.cursovalido.backend.authentication.repository.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SenhaUsuarioServico {
    private final UsuarioRepositorio repositorio;
    private final PasswordEncoder codificadorSenha;
    private final LogAuditoriaServico auditoria;
    private final TokenEmailServico tokensEmail;
    private final EmailServico emails;
    private final SessaoRepositorio sessoes;

    // Envia o link para alterar a senha.
    public void solicitarAlteracaoSenha(String email) {
        auditoria.registrar(null, "RECUPERACAO_SOLICITADA", "Solicitacao recebida", null);
        repositorio.findByEmail(email).filter(usuario -> usuario.isAtivo()).ifPresent(usuario -> {
            String token = tokensEmail.criarAlteracaoSenha(usuario);
            emails.enviarRedefinicaoSenha(usuario, token);
        });
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    // Valida o token e salva a nova senha do usuario.
    public void alterarSenha(String token, String novaSenha, String confirmacaoSenha) {
        if (!novaSenha.equals(confirmacaoSenha)) {
            auditoria.registrar(null, "RECUPERACAO_FALHOU", "Senhas diferentes", null);
            throw new IllegalArgumentException("As senhas nao conferem");
        }
        TokenEmail registro;
        try {
            registro = tokensEmail.validar(token, tokensEmail.finalidadeAlteracaoSenha());
        } catch (IllegalArgumentException erro) {
            auditoria.registrar(null, "RECUPERACAO_FALHOU", "Token invalido, usado ou expirado", null);
            throw erro;
        }
        var usuario = registro.getUsuario();
        usuario.setSenha(codificadorSenha.encode(novaSenha));
        repositorio.save(usuario);
        sessoes.revogarPorUsuario(usuario.getId());
        tokensEmail.invalidarAcessos(usuario);
        auditoria.registrar(usuario.getId(), "ALTERACAO_SENHA", "Senha alterada", null);
    }
}
