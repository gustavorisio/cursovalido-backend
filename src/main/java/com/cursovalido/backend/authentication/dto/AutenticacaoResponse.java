package com.cursovalido.backend.authentication.dto;

import com.cursovalido.backend.authentication.entity.Perfil;

public record AutenticacaoResponse(String token, Long idUsuario, String nome, Perfil perfil,
		boolean requerCodigo2fa, String finalidadeCodigo) {
	public AutenticacaoResponse(String token, Long idUsuario, String nome, Perfil perfil,
			boolean requerCodigo2fa) {
		this(token, idUsuario, nome, perfil, requerCodigo2fa, null);
	}
}
