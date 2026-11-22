package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.dto.VideoInicioRequest;
import com.cursovalido.backend.aulas.dto.VideoInicioResponse;
import com.cursovalido.backend.aulas.entity.VideoInicio;
import com.cursovalido.backend.aulas.repository.VideoInicioRepository;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VideoInicioService {
    private static final Short ID_CONFIGURACAO = (short) 1;
    private final VideoInicioRepository videos;
    private final VimeoService vimeo;
    private final PrivilegiosAdministrativosService privilegios;

    public VideoInicioResponse consultar() {
        return videos.findById(ID_CONFIGURACAO)
                .map(video -> new VideoInicioResponse(video.getVimeoUrl()))
                .orElseGet(() -> new VideoInicioResponse(null));
    }

    @Transactional
    public VideoInicioResponse atualizar(Long idAdministrador, VideoInicioRequest dados) {
        privilegios.verificarPrincipal(idAdministrador);
        String url = vimeo.validarVideo(dados.vimeoUrl());
        VideoInicio video = videos.findById(ID_CONFIGURACAO)
                .orElseGet(() -> new VideoInicio(ID_CONFIGURACAO, url));
        video.atualizarUrl(url);
        videos.save(video);
        return new VideoInicioResponse(url);
    }
}
