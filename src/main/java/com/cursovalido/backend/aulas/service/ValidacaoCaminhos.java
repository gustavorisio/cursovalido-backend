package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.dto.CaminhosRequest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class ValidacaoCaminhos {
    private ValidacaoCaminhos() {}

    public static void validar(CaminhosRequest dados) {
        Map<String, CaminhosRequest.Pergunta> perguntas = new LinkedHashMap<>();
        for (var pergunta : dados.perguntas()) {
            if (perguntas.put(pergunta.chave(), pergunta) != null) falha("Identificador de pergunta repetido: " + pergunta.chave());
        }
        Set<String> resultados = new HashSet<>();
        for (var resultado : dados.resultados()) {
            if (!resultados.add(resultado.chave())) falha("Identificador de resultado repetido: " + resultado.chave());
        }
        if (dados.perguntaInicial() != null && !perguntas.containsKey(dados.perguntaInicial())) falha("A pergunta inicial nao existe");
        for (var pergunta : dados.perguntas()) {
            for (var opcao : pergunta.opcoes()) {
                if (opcao.proximaPergunta() != null && opcao.resultado() != null) falha("Escolha apenas um destino para cada alternativa");
                if (opcao.proximaPergunta() != null && !perguntas.containsKey(opcao.proximaPergunta())) falha("Pergunta de destino inexistente: " + opcao.proximaPergunta());
                if (opcao.resultado() != null && !resultados.contains(opcao.resultado())) falha("Resultado de destino inexistente: " + opcao.resultado());
                if (dados.publicado() && opcao.proximaPergunta() == null && opcao.resultado() == null) falha("Defina o destino de todas as alternativas antes de publicar");
            }
        }
        if (!dados.publicado()) return;
        if (dados.perguntaInicial() == null || perguntas.isEmpty() || resultados.isEmpty()) falha("Defina a pergunta inicial, as perguntas e pelo menos um resultado");
        Set<String> visitadas = new HashSet<>();
        percorrer(dados.perguntaInicial(), perguntas, new HashSet<>(), visitadas);
        if (visitadas.size() != perguntas.size()) falha("Todas as perguntas devem ser alcancadas a partir da pergunta inicial");
    }

    private static void percorrer(String chave, Map<String, CaminhosRequest.Pergunta> perguntas, Set<String> pilha, Set<String> visitadas) {
        if (pilha.contains(chave)) falha("O caminho possui um ciclo na pergunta: " + chave);
        if (visitadas.contains(chave)) return;
        pilha.add(chave);
        for (var opcao : perguntas.get(chave).opcoes()) {
            if (opcao.proximaPergunta() != null) percorrer(opcao.proximaPergunta(), perguntas, pilha, visitadas);
        }
        pilha.remove(chave);
        visitadas.add(chave);
    }

    private static void falha(String mensagem) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
}
