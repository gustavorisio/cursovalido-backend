package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.entity.EscopoSuspensao;
import com.cursovalido.backend.authentication.entity.SuspensaoUsuario;
import java.time.Instant;

public record SuspensaoResposta(Long id, EscopoSuspensao escopo, String motivo, Instant criadaEm, Instant expiraEm) {
    public static SuspensaoResposta de(SuspensaoUsuario suspensao) {
        return new SuspensaoResposta(suspensao.getId(), suspensao.getEscopo(), suspensao.getMotivo(),
                suspensao.getCriadaEm(), suspensao.getExpiraEm());
    }
}
