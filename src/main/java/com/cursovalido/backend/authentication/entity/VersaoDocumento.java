package com.cursovalido.backend.authentication.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_versoes_documentos")
@Getter
@NoArgsConstructor
public class VersaoDocumento {
    @Id
    @Column(length = 30)
    private String tipo;
    @Column(nullable = false)
    private long numero;

    public VersaoDocumento(String tipo, long numero) {
        this.tipo = tipo;
        this.numero = numero;
    }

    public String incrementar() {
        numero = Math.incrementExact(numero);
        if ("TERMOS".equals(tipo)) {
            long decimos = Math.addExact(10, Math.multiplyExact(numero - 1, 2));
            return "v" + decimos / 10 + "." + decimos % 10;
        }
        return numero + ".0";
    }
}
