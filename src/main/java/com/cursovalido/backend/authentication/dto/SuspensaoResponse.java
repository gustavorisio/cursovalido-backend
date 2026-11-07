package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.entity.EscopoSuspensao;
import com.cursovalido.backend.authentication.entity.SuspensaoUsuario;
import java.time.Instant;

public record SuspensaoResponse(Long id, EscopoSuspensao escopo, String motivo, Instant criadaEm, Instant expiraEm) {
    public static SuspensaoResponse de(SuspensaoUsuario suspensao) {
        return new SuspensaoResponse(suspensao.getId(), suspensao.getEscopo(), suspensao.getMotivo(),
                suspensao.getCriadaEm(), suspensao.getExpiraEm());
    }
}
