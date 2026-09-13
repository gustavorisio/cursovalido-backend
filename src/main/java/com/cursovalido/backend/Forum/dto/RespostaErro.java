package com.cursovalido.backend.Forum.dto;

import java.util.Map;

public record RespostaErro(String erro, Map<String, String> detalhes) {
}