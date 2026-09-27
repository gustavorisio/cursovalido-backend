package com.cursovalido.backend.forum.dto;

import java.util.List;
import java.util.Map;

public record RespostaErro(String erro, Map<String, List<String>> detalhes) {
}