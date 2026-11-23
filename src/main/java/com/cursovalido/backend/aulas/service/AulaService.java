package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.dto.AulaRequest;
import com.cursovalido.backend.aulas.entity.Aula;
import com.cursovalido.backend.aulas.repository.AulaRepository;
import com.cursovalido.backend.aulas.repository.ProgressoAulaRepository;
import com.cursovalido.backend.authentication.service.LogAuditoriaService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AulaService {
    private final AulaRepository repositorio;
    private final ModuloService modulos;
    private final YoutubeService youtube;
    private final VimeoService vimeo;
    private final LogAuditoriaService auditoria;
    private final ProgressoAulaRepository progressos;

    public List<Aula> listar(Long idModulo, Long idProfessor) {
        modulos.buscarAtivo(idModulo, idProfessor);
        return repositorio.findByIdModuloAndAtivoTrueOrderByOrdemAscIdAsc(idModulo);
    }

    public Aula buscarAtivo(Long id, Long idProfessor) {
        Aula aula = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula nao encontrada"));
        if (!aula.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula inativa");
        }
        modulos.buscarAtivo(aula.getIdModulo(), idProfessor);
        return aula;
    }

    @Transactional
    public Aula criar(Long idModulo, AulaRequest dados, Long idProfessor) {
        modulos.buscarAtivo(idModulo, idProfessor);
        Aula aula = new Aula();
        aula.setIdModulo(idModulo);
        aula.setCriadoEm(LocalDateTime.now());
        preencher(aula, dados);
        repositorio.save(aula);
        auditoria.registrar(idProfessor, "CADASTRO_AULA", "Aula " + aula.getId(), null);
        return aula;
    }

    @Transactional
    public Aula editar(Long id, AulaRequest dados, Long idProfessor) {
        Aula aula = buscarAtivo(id, idProfessor);
        String youtubeAnterior = aula.getYoutubeVideoId();
        String vimeoAnterior = aula.getVimeoVideoUrl();
        preencher(aula, dados);
        if (!Objects.equals(youtubeAnterior, aula.getYoutubeVideoId())
                || !Objects.equals(vimeoAnterior, aula.getVimeoVideoUrl())) {
            progressos.deleteByIdAula(id);
        }
        repositorio.save(aula);
        auditoria.registrar(idProfessor, "EDICAO_AULA", "Aula " + id, null);
        return aula;
    }

    @Transactional
    public void apagar(Long id, Long idProfessor) {
        Aula aula = buscarAtivo(id, idProfessor);
        aula.setAtivo(false);
        repositorio.save(aula);
        auditoria.registrar(idProfessor, "EXCLUSAO_AULA", "Aula " + id, null);
    }

    private void preencher(Aula aula, AulaRequest dados) {
        String vimeoUrl = vimeo.validarVideo(dados.vimeoUrl());
        String videoId = dados.youtubeUrl() == null || dados.youtubeUrl().isBlank()
                ? null : youtube.validarVideo(dados.youtubeUrl());
        if (vimeoUrl == null || videoId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe um vídeo válido para o Server 1 (Vimeo) e para o Server 2 (YouTube).");
        }
        aula.setTitulo(dados.titulo().trim());
        aula.setDescricao(dados.descricao().trim());
        aula.setOrdem(dados.ordem());
        aula.setYoutubeVideoId(videoId);
        aula.setVimeoVideoUrl(vimeoUrl);
    }
}
