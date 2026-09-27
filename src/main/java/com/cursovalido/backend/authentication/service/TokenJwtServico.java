package com.cursovalido.backend.authentication.service;

import com.cursovalido.backend.authentication.entity.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenJwtServico {
    @Value("${seguranca.jwt.chave}")
    private String chaveTexto;
    @Value("${seguranca.jwt.expiracao-ms:86400000}")
    private long expiracaoMs;
    private Key chave;

    @jakarta.annotation.PostConstruct
    // Prepara a chave usada para assinar os tokens.
    public void prepararChave() {
        chave = Keys.hmacShaKeyFor(chaveTexto.getBytes(StandardCharsets.UTF_8));
    }

    // Gera um JWT com os dados basicos do usuario.
    public String gerar(Usuario usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("idUsuario", usuario.getId())
                .claim("perfil", usuario.getPerfil().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusMillis(expiracaoMs)))
                .signWith(chave)
                .compact();
    }

    // Valida o JWT e retorna o e-mail salvo nele.
    public String extrairEmail(String token) {
        return Jwts.parser().verifyWith((javax.crypto.SecretKey) chave).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }
}