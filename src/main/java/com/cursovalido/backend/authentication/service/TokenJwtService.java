package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.Sessao;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.repository.SessaoRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenJwtService {
    @Value("${seguranca.jwt.chave}")
    private String chaveTexto;
    @Value("${seguranca.jwt.expiracao-ms:86400000}")
    private long expiracaoMs;
    private Key chave;
    private final SessaoRepository sessoes;

    @PostConstruct
    // Prepara a chave usada para assinar os tokens.
    public void prepararChave() {
        chave = Keys.hmacShaKeyFor(chaveTexto.getBytes(StandardCharsets.UTF_8));
    }

    // Gera um JWT com os dados basicos do usuario.
    public String gerar(Usuario usuario) {
        return criar(usuario, "ACESSO", expiracaoMs);
    }

    public String gerarTermos(Usuario usuario) {
        return criar(usuario, "TERMOS", 300_000);
    }

    private String criar(Usuario usuario, String finalidade, long duracao) {
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

    public Long extrairIdUsuario(String token) {
        return validar(token).get("idUsuario", Long.class);
    }

    private Claims validar(String token) {
        var dados = Jwts.parser().verifyWith((javax.crypto.SecretKey) chave).build()
                .parseSignedClaims(token).getPayload();
        var sessao = sessoes.findById(dados.getId())
                .orElseThrow(() -> new IllegalArgumentException("Sessao invalida"));
        if (sessao.isRevogada())
            throw new IllegalArgumentException("Sessao encerrada");
        if (!sessao.getExpiraEm().isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("Sessao expirada");
        if (!java.util.Objects.equals(sessao.getUsuarioId(), dados.get("idUsuario", Long.class)))
            throw new IllegalArgumentException("Sessao de outro usuario");
        return dados;
    }

    public boolean apenasTermos(String token) {
        return "TERMOS".equals(validar(token).get("finalidade", String.class));
    }

    public void encerrar(String token) {
        var dados = validar(token);
        var sessao = sessoes.findById(dados.getId()).orElseThrow();
        sessao.revogar();
        sessoes.save(sessao);
    }
}
