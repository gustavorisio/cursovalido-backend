package com.cursovalido.backend.authentication.dto;

public record AutenticacaoResposta(String token, Long idUsuario, String nome, String perfil,
		boolean requerCodigo2fa) {
}