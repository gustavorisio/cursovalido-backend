package com.cursovalido.backend.aulas.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YoutubeConsultaResponse(List<Video> items) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Video(String id, Status status) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Status(Boolean embeddable, String privacyStatus, String uploadStatus) {
    }
}
