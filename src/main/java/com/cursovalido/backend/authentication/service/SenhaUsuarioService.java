package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.TokenEmail;
import com.cursovalido.backend.authentication.repository.SessaoRepository;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SenhaUsuarioService {
    private final UsuarioRepository repositorio;
    private final PasswordEncoder codificadorSenha;
    private final LogAuditoriaService auditoria;
    private final TokenEmailService tokensEmail;
    private final EmailService emails;
    private final SessaoRepository sessoes;

    @Transactional
    public void alterarSenhaAutenticado(Long idUsuario, String senhaAtual, String novaSenha, String confirmacaoSenha) {
        var usuario = repositorio.findById(idUsuario)
                .filter(conta -> conta.isAtivo())
                .orElseThrow(() -> new IllegalArgumentException("Conta indisponivel."));
        if (!codificadorSenha.matches(senhaAtual, usuario.getSenha())) {
            throw new IllegalArgumentException("Senha atual incorreta.");
        }
        if (!novaSenha.equals(confirmacaoSenha)) {
            throw new IllegalArgumentException("As senhas nao conferem.");
        }
        if (!new com.cursovalido.backend.authentication.validation.SenhaValidaValidator().isValid(novaSenha, null)) {
            throw new IllegalArgumentException("Informe uma senha valida.");
        }
        usuario.setSenha(codificadorSenha.encode(novaSenha));
        repositorio.save(usuario);
        sessoes.revogarPorUsuario(usuario.getId());
        tokensEmail.invalidarAcessos(usuario);
        auditoria.registrar(usuario.getId(), "ALTERACAO_SENHA_AUTENTICADO", "Senha alterada", null);
    }

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
