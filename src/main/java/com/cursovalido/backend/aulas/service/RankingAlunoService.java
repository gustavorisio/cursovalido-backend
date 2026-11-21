package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.dto.RankingAlunoResponse;
import com.cursovalido.backend.aulas.repository.RankingAlunoRepository;
import com.cursovalido.backend.authentication.security.AnonimizacaoDados;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RankingAlunoService {
    private final RankingAlunoRepository ranking;

    public List<RankingAlunoResponse> listar() {
        return ranking.listar().stream()
                .map(item -> new RankingAlunoResponse(item.posicao(), AnonimizacaoDados.abreviarNome(item.nome()),
                        item.nivel(), item.comentarios(), item.aulasConcluidas()))
                .toList();
    }
}
