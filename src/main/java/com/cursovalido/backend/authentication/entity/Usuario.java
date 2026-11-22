package com.cursovalido.backend.authentication.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.security.SecureRandom;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_usuarios")
@Getter
@NoArgsConstructor
@Setter
public class Usuario {
    private static final char[] CARACTERES_GAMERTAG = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM_GAMERTAG = new SecureRandom();
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;
    @Column(name = "nome_completo", nullable = false, length = 120)
    private String nomeCompleto;
    @Column(nullable = false, unique = true, length = 254)
    private String email;
    @Column(name = "gamertag", nullable = false, unique = true, length = 15)
    private String gamertag;
    @Column(nullable = false, length = 60)
    private String senha;
    @Column(nullable = false, unique = true, length = 50)
    private String matricula;
    @Column(name = "declarou_maior_idade", nullable = false)
    private Boolean declarouMaiorIdade;
    @Column(name = "aceitou_termos_lgpd", nullable = false)
    private Boolean aceitouTermosLgpd;
    @Column(name = "data_hora_aceite")
    private LocalDateTime dataHoraAceite;
    @Column(name = "versao_termos", length = 30)
    private String versaoTermos;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Perfil perfil;
    @Column(nullable = false)
    private boolean ativo = true;
    @Column(name = "excluido_em")
    private LocalDateTime excluidoEm;
    @Column(name = "email_confirmado", nullable = false)
    private boolean emailConfirmado = false;
    @Column(name = "dois_fatores_ativos", nullable = false)
    private boolean doisFatoresAtivos = false;
    @Enumerated(EnumType.STRING)
    @Column(name = "status_aprovacao", nullable = false)
    private StatusAprovacao statusAprovacao = StatusAprovacao.APROVADO;

    @PrePersist
    private void gerarGamertagSeAusente() {
        if (gamertag != null && !gamertag.isBlank()) return;
        StringBuilder tag = new StringBuilder("CV-");
        for (int i = 0; i < 12; i++) tag.append(CARACTERES_GAMERTAG[RANDOM_GAMERTAG.nextInt(CARACTERES_GAMERTAG.length)]);
        gamertag = tag.toString();
    }
}
