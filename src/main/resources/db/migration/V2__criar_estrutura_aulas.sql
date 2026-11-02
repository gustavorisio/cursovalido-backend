CREATE TABLE tb_cursos (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao VARCHAR(5000) NOT NULL,
    professor_id BIGINT REFERENCES tb_usuarios(id) ON DELETE SET NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL
);
CREATE TABLE tb_modulos (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao VARCHAR(5000) NOT NULL,
    curso_id BIGINT NOT NULL REFERENCES tb_cursos(id),
    ordem INTEGER NOT NULL CHECK (ordem > 0),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL
);
CREATE TABLE tb_aulas (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao VARCHAR(5000) NOT NULL,
    modulo_id BIGINT NOT NULL REFERENCES tb_modulos(id),
    ordem INTEGER NOT NULL CHECK (ordem > 0),
    youtube_video_id VARCHAR(11) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL
);
CREATE INDEX idx_cursos_professor ON tb_cursos(professor_id);
CREATE INDEX idx_modulos_curso_ordem ON tb_modulos(curso_id, ordem, id);
CREATE INDEX idx_aulas_modulo_ordem ON tb_aulas(modulo_id, ordem, id);

CREATE TABLE tb_matriculas (
    id BIGSERIAL PRIMARY KEY,
    aluno_id BIGINT NOT NULL REFERENCES tb_usuarios(id) ON DELETE CASCADE,
    curso_id BIGINT NOT NULL REFERENCES tb_cursos(id),
    matriculado_em TIMESTAMP NOT NULL,
    CONSTRAINT uk_matricula_aluno_curso UNIQUE (aluno_id, curso_id)
);
CREATE INDEX idx_matriculas_curso ON tb_matriculas(curso_id);
