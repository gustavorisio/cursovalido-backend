package com.cursovalido.backend.authentication.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_aceites_responsabilidade")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AceiteResponsabilidade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long usuarioId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Perfil perfil;
    @Column(name = "termo_version", nullable = false, length = 30)
    private String versao;
    @Column(name = "ip_address", nullable = false, length = 45)
    private String enderecoIp;
    @Column(name = "accepted_at", nullable = false)
    private LocalDateTime aceitoEm;

    public AceiteResponsabilidade(Long usuarioId, Perfil perfil, String versao, String enderecoIp) {
        this.usuarioId = usuarioId;
        this.perfil = perfil;
        this.versao = versao;
        this.enderecoIp = enderecoIp;
        this.aceitoEm = LocalDateTime.now();
    }
}
