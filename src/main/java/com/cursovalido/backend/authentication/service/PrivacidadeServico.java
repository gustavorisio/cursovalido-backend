package com.cursovalido.backend.authentication.service;
import com.cursovalido.backend.authentication.dto.PoliticaRequisicao;
import com.cursovalido.backend.authentication.dto.PoliticaResposta;
import com.cursovalido.backend.authentication.entity.PoliticaPrivacidade;
import com.cursovalido.backend.authentication.repository.PrivacidadeRepositorio;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class PrivacidadeServico {
    private final PrivacidadeRepositorio repositorio;
    private final PrivilegiosAdministrativosServico privilegios;
    private final LogAuditoriaServico auditoria;

    public PoliticaResposta buscarAtiva() {
        return repositorio.findByAtivaTrueOrderByDataPublicacaoDesc().stream()
            .findFirst().map(this::converter).orElse(null);
    }

    public boolean atualizadaDesde(LocalDateTime aceite) {
        var atual = buscarAtiva();
        return atual != null && (aceite == null || atual.dataPublicacao().isAfter(aceite));
    }

    @Transactional
    public PoliticaResposta publicar(PoliticaRequisicao dados, Long administradorId) {
        privilegios.verificarPrincipal(administradorId);
        if (repositorio.findByVersao(dados.versao().trim()).isPresent())
            throw new IllegalArgumentException("A versao informada ja existe");
        repositorio.findByAtivaTrueOrderByDataPublicacaoDesc().forEach(politica -> {
            politica.setAtiva(false);
            repositorio.save(politica);
        });
        var publicada = repositorio.save(new PoliticaPrivacidade(dados.nome().trim(), dados.versao().trim(), dados.conteudo().trim()));
        auditoria.registrar(administradorId, "PRIVACIDADE_PUBLICADA", publicada.getVersao(), null);
        return converter(publicada);
    }

    private PoliticaResposta converter(PoliticaPrivacidade politica) {
        return new PoliticaResposta(politica.getNome(), politica.getVersao(), politica.getConteudo(), politica.getDataPublicacao());
    }
}
