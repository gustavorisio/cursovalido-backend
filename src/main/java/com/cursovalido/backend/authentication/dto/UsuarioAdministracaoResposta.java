package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.entity.Perfil;
import java.util.List;

public record UsuarioAdministracaoResposta(Long id, String nome, String gamertag, Perfil perfil,
        boolean root, boolean ativo, boolean podeModerar, boolean podeExcluir,
        List<SuspensaoResponse> suspensoes) {
}
