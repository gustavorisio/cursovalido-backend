package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import java.time.LocalDate;

public record ConviteAcessoResposta(Long id, String email, Perfil perfil, StatusAprovacao status,
                String nomeCompleto, String cpf, LocalDate dataNascimento, String telefone) {
}
