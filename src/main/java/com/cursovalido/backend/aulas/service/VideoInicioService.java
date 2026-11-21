package com.cursovalido.backend.aulas.service;

import com.cursovalido.backend.aulas.dto.VideoInicioRequest;
import com.cursovalido.backend.aulas.dto.VideoInicioResponse;
import com.cursovalido.backend.aulas.entity.VideoInicio;
import com.cursovalido.backend.aulas.repository.VideoInicioRepository;
import com.cursovalido.backend.authentication.validation.administracao.PrivilegiosAdministrativosService;
import java.net.URI;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VideoInicioService {
    private static final Short ID_CONFIGURACAO = (short) 1;
    private static final Pattern ID_VIDEO = Pattern.compile("(?:^|/)(\\d+)(?:/|$)");
    private final VideoInicioRepository videos;
    private final PrivilegiosAdministrativosService privilegios;

    public VideoInicioResponse consultar() {
        return videos.findById(ID_CONFIGURACAO)
                .map(video -> new VideoInicioResponse(video.getVimeoUrl()))
                .orElseGet(() -> new VideoInicioResponse(null));
    }

    @Transactional
    public VideoInicioResponse atualizar(Long idAdministrador, VideoInicioRequest dados) {
        privilegios.verificarPrincipal(idAdministrador);
        String url = normalizarEValidar(dados.vimeoUrl());
        VideoInicio video = videos.findById(ID_CONFIGURACAO)
                .orElseGet(() -> new VideoInicio(ID_CONFIGURACAO, url));
        video.atualizarUrl(url);
        videos.save(video);
        return new VideoInicioResponse(url);
    }

    private String normalizarEValidar(String valor) {
        if (valor == null || valor.isBlank())
            return null;
        String url = valor.trim();
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null
                    || !("vimeo.com".equalsIgnoreCase(host) || "www.vimeo.com".equalsIgnoreCase(host)
                            || "player.vimeo.com".equalsIgnoreCase(host))
                    || !ID_VIDEO.matcher(uri.getPath()).find()) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException erro) {
            throw new IllegalArgumentException("Informe uma URL HTTPS válida de vídeo do Vimeo.");
        }
        return url;
    }
}
