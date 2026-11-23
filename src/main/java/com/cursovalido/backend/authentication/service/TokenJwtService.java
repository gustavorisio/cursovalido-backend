package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.Sessao;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.exception.CredenciaisInvalidasException;
import com.cursovalido.backend.authentication.repository.SessaoRepository;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.validation.token.TokenJwtValidation;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenJwtService {
    @Value("${seguranca.jwt.chave}")
    private String chaveTexto;
    @Value("${seguranca.jwt.expiracao-ms:86400000}")
    private long expiracaoMs;
    private Key chave;
    private final SessaoRepository sessoes;
    private final UsuarioRepository usuarios;

    @PostConstruct
    // Prepara a chave usada para assinar os tokens.
    public void prepararChave() {
        if (expiracaoMs < 1) {
            throw new IllegalStateException("JWT_EXPIRATION deve ser um valor positivo em milissegundos");
        }
        chave = Keys.hmacShaKeyFor(chaveTexto.getBytes(StandardCharsets.UTF_8));
    }

    // Gera um JWT com os dados básicos do usuário.
    @Transactional
    public String gerar(Usuario usuario) {
        return criar(usuario, "ACESSO", Math.min(expiracaoMs, 86_400_000L));
    }

    @Transactional
    public String gerarTermos(Usuario usuario) {
        return criar(usuario, "TERMOS", 300_000);
    }

    private String criar(Usuario usuario, String finalidade, long duracao) {
        usuarios.buscarParaAtualizar(usuario.getId())
                .orElseThrow(CredenciaisInvalidasException::new);
        sessoes.revogarPorUsuario(usuario.getId());
        Instant agora = Instant.now();
        String id = UUID.randomUUID().toString();
        sessoes.save(new Sessao(id, usuario.getId(),
                LocalDateTime.now().plusNanos(duracao * 1_000_000)));
        return Jwts.builder()
                .id(id)
                .claim("finalidade", finalidade)
                .subject(usuario.getEmail())
                .claim("idUsuario", usuario.getId())
                .claim("perfil", usuario.getPerfil().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusMillis(duracao)))
                .signWith(chave)
                .compact();
    }

    // Valida o JWT e retorna o e-mail salvo nele.
    public String extrairEmail(String token) {
        return validar(token).getSubject();
    }

    public DadosTokenRequisicao dadosParaRequisicao(String token) {
        Claims dados = lerClaims(token);
        return new DadosTokenRequisicao(dados.getSubject(), dados.get("idUsuario", Long.class), dados.getId(),
                dados.get("finalidade", String.class));
    }

    public record DadosTokenRequisicao(String email, Long idUsuario, String idSessao, String finalidade) {
        public boolean apenasTermos() {
            return "TERMOS".equals(finalidade);
        }
    }

    public Long extrairIdUsuario(String token) {
        return validar(token).get("idUsuario", Long.class);
    }

    private Claims validar(String token) {
        var dados = lerClaims(token);
        var sessao = sessoes.findById(dados.getId())
                .orElseThrow(() -> new IllegalArgumentException("Sessao invalida"));
        TokenJwtValidation.validarSessao(sessao, dados);
        return dados;
    }

    private Claims lerClaims(String token) {
        return Jwts.parser().verifyWith((SecretKey) chave).build()
                .parseSignedClaims(token).getPayload();
    }

    public boolean apenasTermos(String token) {
        return "TERMOS".equals(validar(token).get("finalidade", String.class));
    }

    public void encerrar(String token) {
        var dados = validar(token);
        var sessao = sessoes.findById(dados.getId())
                .orElseThrow(CredenciaisInvalidasException::new);
        sessao.revogar();
        sessoes.save(sessao);
    }
}
