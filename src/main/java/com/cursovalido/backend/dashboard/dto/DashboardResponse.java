package com.cursovalido.backend.dashboard.dto;

import java.time.LocalDate;

public final class DashboardResponse {
        private DashboardResponse() {
        }

        public record Resumo(long alunosAtivos, long professoresAtivos, long cursosAtivos,
                        long aulasAtivas, long matriculas, long topicosAtivos, long comentariosAtivos, long aulasConcluidas,
                        long cursosConcluidos, long alunosEngajados, double taxaConclusaoMatriculas,
                        Double nivelMedioAlunos) {
        }

        public record AtividadeDia(LocalDate data, long matriculas, long aulasConcluidas, long cursosConcluidos,
                        long cursosCriados, long aulasCriadas, long topicosCriados) {
        }

        public record MatriculasMes(String mes, long matriculas, long aulasConcluidas) {
                public MatriculasMes(String mes, long matriculas) {
                        this(mes, matriculas, 0);
                }
        }

        public record CursoPopular(long idCurso, String titulo, long matriculas, long aulasConcluidas,
                        long conclusoes, double taxaConclusao) {
        }

        public record UsuariosPerfil(String perfil, long quantidade) {
        }

}
