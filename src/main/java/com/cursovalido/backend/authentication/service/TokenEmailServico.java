package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.TokenEmail;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.TokenEmailRepositorio;
import com.cursovalido.backend.authentication.exception.Codigo2faExpiradoException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class TokenEmailServico {
    private static final String CONFIRMACAO_CADASTRO = "CONFIRMACAO_CADASTRO";
    private static final String ALTERACAO_SENHA = "ALTERACAO_SENHA";
    private static final String CODIGO_2FA = "CODIGO_2FA";

    @Autowired
    private TokenEmailRepositorio repositorio;
    private final SecureRandom aleatorio = new SecureRandom();

    @Autowired
    public TokenEmailServico() {
    }

    public TokenEmailServico(TokenEmailRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    // Cria o token de confirmacao do cadastro.
    public String criarConfirmacao(Usuario usuario) {
        return criar(usuario, CONFIRMACAO_CADASTRO, 24);
    }

    @Transactional
    // Cria um novo token e invalida o anterior.
    public String criarNovaConfirmacao(Usuario usuario) {
        repositorio.invalidarTokensAtivos(usuario.getId(), CONFIRMACAO_CADASTRO);
        return criar(usuario, CONFIRMACAO_CADASTRO, 24);
    }

    // Verifica o limite de reenvios de confirmacao.
    public boolean excedeuLimiteConfirmacao(Usuario usuario) {
        return repositorio.countByUsuarioIdAndFinalidadeAndCriadoEmAfter(
                usuario.getId(), CONFIRMACAO_CADASTRO, LocalDateTime.now().minusMinutes(15)) >= 3;
    }

    @Transactional
    // Gera um novo codigo de 2FA e invalida o anterior.
    public String criarCodigo2fa(Usuario usuario) {
        repositorio.invalidarTokensAtivos(usuario.getId(), CODIGO_2FA);
        String codigo = String.format("%06d", aleatorio.nextInt(1_000_000));
        repositorio.save(new TokenEmail(hash(codigo), usuario, CODIGO_2FA,
                LocalDateTime.now().plusMinutes(10)));
        return codigo;
    }

    @Transactional
    // Confere o codigo 2FA e marca o token como usado.
    public Usuario validarCodigo2fa(String email, String codigo) {
        TokenEmail registro = repositorio.findByTokenHashAndFinalidade(hash(codigo), CODIGO_2FA)
                .filter(token -> token.getUsuario().getEmail().equalsIgnoreCase(email))
                .orElseThrow(() -> new IllegalArgumentException("Codigo invalido ou expirado"));
        if (registro.isUtilizado())
            throw new IllegalArgumentException("Codigo ja utilizado");
        if (registro.getExpiraEm().isBefore(LocalDateTime.now())) {
            throw new Codigo2faExpiradoException();
        }
        registro.setUtilizado(true);
        repositorio.save(registro);
        return registro.getUsuario();
    }

    @Transactional
    // Cria o token usado na alteracao de senha.
    public String criarAlteracaoSenha(Usuario usuario) {
        return criar(usuario, ALTERACAO_SENHA, 0.5);
    }

    @Transactional
    // Valida um token de e-mail e marca seu uso.
    public TokenEmail validar(String token, String finalidade) {
        TokenEmail registro = repositorio.findByTokenHashAndFinalidade(hash(token), finalidade)
                .orElseThrow(() -> new IllegalArgumentException("Token invalido"));
        if (registro.isUtilizado())
            throw new IllegalArgumentException("Token ja utilizado");
        if (registro.getExpiraEm().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Token expirado");
        registro.setUtilizado(true);
        return repositorio.save(registro);
    }

    private String criar(Usuario usuario, String finalidade, double horas) {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime validade = LocalDateTime.now().plusMinutes((long) (horas * 60));
        repositorio.save(new TokenEmail(hash(token), usuario, finalidade, validade));
        return token;
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception excecao) {
            throw new IllegalStateException("Nao foi possivel proteger o token", excecao);
        }
    }

    // Retorna o identificador do token de confirmacao.
    public String finalidadeConfirmacao() {
        return CONFIRMACAO_CADASTRO;
    }

    // Retorna o identificador do token de alteracao de senha.
    public String finalidadeAlteracaoSenha() {
        return ALTERACAO_SENHA;
    }
}