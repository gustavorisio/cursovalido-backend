package com.cursovalido.backend.authentication.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_aceites_termos_arquivados")
public class AceiteTermoArquivado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id_original", nullable = false)
    private Long usuarioIdOriginal;

    @Column(nullable = false, length = 30)
    private String versao;

    @Column(name = "data_hora_aceite", nullable = false)
    private LocalDateTime dataHoraAceite;

    @Column(name = "data_hora_arquivamento", nullable = false)
    private LocalDateTime dataHoraArquivamento;

    @Column(nullable = false, length = 40)
    private String motivo;

    protected AceiteTermoArquivado() {
    }

    public AceiteTermoArquivado(Long usuarioIdOriginal, String versao,
            LocalDateTime dataHoraAceite, String motivo) {
        this.usuarioIdOriginal = usuarioIdOriginal;
        this.versao = versao;
        this.dataHoraAceite = dataHoraAceite;
        this.dataHoraArquivamento = LocalDateTime.now();
        this.motivo = motivo;
    }
}
