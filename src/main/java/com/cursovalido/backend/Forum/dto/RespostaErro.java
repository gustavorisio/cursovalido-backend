package com.cursovalido.backend.Forum.dto;

import java.util.List;
import java.util.Map;

public record RespostaErro(String erro, Map<String, List<String>> detalhes) {
}