package com.cursovalido.backend.forum.dto;

import java.util.List;
import java.util.Map;

public record ErroResponse(String erro, Map<String, List<String>> detalhes) {
}