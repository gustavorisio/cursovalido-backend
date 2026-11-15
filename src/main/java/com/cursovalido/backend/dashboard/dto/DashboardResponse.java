package com.cursovalido.backend.dashboard.dto;

public final class DashboardResponse {
    private DashboardResponse() {
    }

    public record Resumo(long alunosAtivos, long professoresAtivos, long cursosAtivos,
            long aulasAtivas, long matriculas, long topicosAtivos, long noticiasAtivas,
            Double mediaIdadeAlunos, long alunosComNascimentoInformado) {
    }

    public record MatriculasMes(String mes, long matriculas) {
    }

    public record CursoPopular(long idCurso, String titulo, long matriculas) {
    }

    public record UsuariosPerfil(String perfil, long quantidade) {
    }
}
