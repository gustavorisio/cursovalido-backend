package com.cursovalido.backend.authentication.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_logs_auditoria")
public class LogAuditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "usuario_id")
    private Long usuarioId;
    @Column(name = "tipo_acao", nullable = false)
    private String tipoAcao;
    private String detalhes;
    @Column(name = "endereco_ip")
    private String enderecoIp;
    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;
    @Column(name = "hash_anterior", length = 64)
    private String hashAnterior;
    @Column(length = 64)
    private String assinatura;

    protected LogAuditoria() {
    }

    public LogAuditoria(Long usuarioId, String tipoAcao, String detalhes, String enderecoIp,
            String hashAnterior, String assinatura) {
        this.usuarioId = usuarioId;
        this.tipoAcao = tipoAcao;
        this.detalhes = detalhes;
        this.enderecoIp = enderecoIp;
        this.dataHora = LocalDateTime.now();
        this.hashAnterior = hashAnterior;
        this.assinatura = assinatura;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getTipoAcao() {
        return tipoAcao;
    }

    public String getDetalhes() {
        return detalhes;
    }

    public String getEnderecoIp() {
        return enderecoIp;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getHashAnterior() {
        return hashAnterior;
    }

    public String getAssinatura() {
        return assinatura;
    }

    public void proteger(String hashAnterior, String assinatura) {
        this.hashAnterior = hashAnterior;
        this.assinatura = assinatura;
    }
}