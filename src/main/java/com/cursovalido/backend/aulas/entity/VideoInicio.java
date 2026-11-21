package com.cursovalido.backend.aulas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_video_inicio")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoInicio {
    @Id
    private Short id;

    @Column(name = "vimeo_url", length = 2048)
    private String vimeoUrl;

    public VideoInicio(Short id, String vimeoUrl) {
        this.id = id;
        this.vimeoUrl = vimeoUrl;
    }

    public void atualizarUrl(String vimeoUrl) {
        this.vimeoUrl = vimeoUrl;
    }
}
