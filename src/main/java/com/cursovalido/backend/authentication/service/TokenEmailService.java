package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.TokenEmail;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.TokenEmailRepository;
import com.cursovalido.backend.authentication.validation.token.TokenEmailValidation;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenEmailService {
    private static final String ALTERACAO_SENHA = "ALTERACAO_SENHA";
    private static final String CODIGO_2FA = "CODIGO_2FA";

    private final TokenEmailRepository repositorio;
    private final SecureRandom aleatorio = new SecureRandom();

    @Transactional
    // Gera um novo codigo de 2FA e invalida o anterior.
    public String criarCodigo2fa(Usuario usuario) {
        verificarLimite(usuario, CODIGO_2FA);
        repositorio.invalidarTokensAtivos(usuario.getId(), CODIGO_2FA);
        String codigo;
        String hashCodigo;
        do {
            codigo = String.format(Locale.ROOT, "%06d", aleatorio.nextInt(1_000_000));
            hashCodigo = hash(usuario.getEmail().toLowerCase(Locale.ROOT) + ":" + codigo);
        } while (repositorio.existsByTokenHash(hashCodigo));
        repositorio.save(new TokenEmail(hashCodigo, usuario, CODIGO_2FA,
                LocalDateTime.now().plusMinutes(10)));
        return codigo;
    }

    @Transactional
    // Confere o codigo 2FA e marca o token como usado.
    public Usuario validarCodigo2fa(String email, String codigo) {
        TokenEmail registro = localizarCodigo2fa(email, codigo);
        registro.setUtilizado(true);
        repositorio.save(registro);
        return registro.getUsuario();
    }

    // Confere o codigo sem consumi-lo e devolve o identificador do usuario dono do codigo.
    @Transactional
    public Long conferirCodigo2fa(String email, String codigo) {
        return localizarCodigo2fa(email, codigo).getUsuario().getId();
    }

    private TokenEmail localizarCodigo2fa(String email, String codigo) {
        String endereco = email.trim().toLowerCase(Locale.ROOT);
        TokenEmail registro = repositorio.findByTokenHashAndFinalidade(hash(endereco + ":" + codigo), CODIGO_2FA)
                .filter(token -> token.getUsuario().getEmail().equalsIgnoreCase(endereco))
                .orElseThrow(() -> new IllegalArgumentException(
                        "O código está incorreto ou venceu. Confira o código mais recente enviado ao seu e-mail ou peça um novo."));
        TokenEmailValidation.validarCodigo2fa(registro);
        return registro;
    }

    @Transactional
    // Cria o token usado na alteracao de senha.
    public String criarAlteracaoSenha(Usuario usuario) {
        verificarLimite(usuario, ALTERACAO_SENHA);
        repositorio.invalidarTokensAtivos(usuario.getId(), ALTERACAO_SENHA);
        return criar(usuario, ALTERACAO_SENHA, 0.5);
    }

    public Optional<TokenEmail> ultimoCodigo2fa(Usuario usuario) {
        return repositorio.findFirstByUsuarioIdAndFinalidadeOrderByCriadoEmDescIdDesc(usuario.getId(), CODIGO_2FA);
    }

    @Transactional
    public String criarExclusao(Usuario usuario) {
        verificarLimite(usuario, "EXCLUSAO_CONTA");
        repositorio.invalidarTokensAtivos(usuario.getId(), "EXCLUSAO_CONTA");
        String codigo;
        String protegido;
        do {
            codigo = String.format(Locale.ROOT, "%06d", aleatorio.nextInt(1_000_000));
            protegido = hash("EXCLUSAO_CONTA:" + usuario.getId() + ":" + codigo);
        } while (repositorio.existsByTokenHash(protegido));
        repositorio.save(new TokenEmail(protegido, usuario, "EXCLUSAO_CONTA", LocalDateTime.now().plusMinutes(10)));
        return codigo;
    }

    @Transactional
    public void validarExclusao(Long idUsuario, String codigo) {
        var registro = repositorio.findByTokenHashAndFinalidade(
                hash("EXCLUSAO_CONTA:" + idUsuario + ":" + codigo), "EXCLUSAO_CONTA")
                .filter(t -> t.getUsuario().getId().equals(idUsuario))
                .filter(t -> !t.isUtilizado())
                .filter(t -> t.getExpiraEm().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "O código está incorreto ou venceu. Confira o código mais recente enviado ao seu e-mail ou peça um novo."));
        registro.setUtilizado(true);
        repositorio.save(registro);
    }

    private void verificarLimite(Usuario usuario, String finalidade) {
        TokenEmailValidation.verificarLimite(usuario, finalidade, repositorio);
    }

    @Transactional
    public void invalidarAcessos(Usuario usuario) {
        repositorio.invalidarTokensAtivos(usuario.getId(), CODIGO_2FA);
        repositorio.invalidarTokensAtivos(usuario.getId(), ALTERACAO_SENHA);
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    // Valida um token de e-mail e marca seu uso.
    public TokenEmail validar(String token, String finalidade) {
        TokenEmail registro = repositorio.findByTokenHashAndFinalidade(hash(token), finalidade)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Este link não é válido ou já venceu. Solicite um novo link para continuar."));
        TokenEmailValidation.validarToken(registro);
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

    // Retorna o identificador do token de alteracao de senha.
    public String finalidadeAlteracaoSenha() {
        return ALTERACAO_SENHA;
    }
}
