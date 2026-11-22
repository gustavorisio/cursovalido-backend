package com.cursovalido.backend.aulas.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VimeoConsultaResponse(String uri, Usuario user) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usuario(String uri) {}
}
