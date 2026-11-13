-- Atualizacao estrutural de bancos na V1 e de bancos novos (V1 seguida de V2).
-- Sem carga de dados ou atualizacao de termos e politicas; a gestao do conteudo e manual.

CREATE TABLE tb_temas (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL
);

CREATE UNIQUE INDEX uk_temas_nome ON tb_temas (LOWER(nome));

ALTER TABLE tb_cursos ADD COLUMN tema_id BIGINT REFERENCES tb_temas(id) ON DELETE SET NULL;
CREATE INDEX idx_cursos_tema ON tb_cursos(tema_id);

CREATE TABLE tb_areas_tecnologia (
    id BIGINT PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(1000) NOT NULL,
    passos VARCHAR(1500) NOT NULL,
    area_pai_id BIGINT REFERENCES tb_areas_tecnologia(id),
    ordem INTEGER NOT NULL
);

ALTER TABLE tb_cursos ADD COLUMN area_id BIGINT REFERENCES tb_areas_tecnologia(id);
CREATE INDEX idx_cursos_area ON tb_cursos(area_id);

CREATE TABLE tb_questionarios (
    id BIGSERIAL PRIMARY KEY,
    curso_id BIGINT NOT NULL REFERENCES tb_cursos(id),
    modulo_id BIGINT REFERENCES tb_modulos(id),
    titulo VARCHAR(255) NOT NULL,
    publicado BOOLEAN NOT NULL DEFAULT FALSE,
    versao INTEGER NOT NULL DEFAULT 1
);
CREATE UNIQUE INDEX uk_diagnostico_curso ON tb_questionarios(curso_id) WHERE modulo_id IS NULL;
CREATE UNIQUE INDEX uk_questionario_modulo ON tb_questionarios(modulo_id) WHERE modulo_id IS NOT NULL;

CREATE TABLE tb_questoes (
    id BIGSERIAL PRIMARY KEY,
    questionario_id BIGINT NOT NULL REFERENCES tb_questionarios(id) ON DELETE CASCADE,
    enunciado VARCHAR(1000) NOT NULL,
    explicacao VARCHAR(2000) NOT NULL,
    alternativa_correta INTEGER NOT NULL,
    aula_revisao_id BIGINT NOT NULL REFERENCES tb_aulas(id),
    ordem INTEGER NOT NULL
);
CREATE TABLE tb_alternativas_questao (
    questao_id BIGINT NOT NULL REFERENCES tb_questoes(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    texto VARCHAR(500) NOT NULL,
    PRIMARY KEY (questao_id, ordem)
);

CREATE TABLE tb_tentativas_questionario (
    id BIGSERIAL PRIMARY KEY,
    aluno_id BIGINT NOT NULL REFERENCES tb_usuarios(id) ON DELETE CASCADE,
    questionario_id BIGINT NOT NULL REFERENCES tb_questionarios(id),
    versao INTEGER NOT NULL,
    acertos INTEGER NOT NULL,
    total INTEGER NOT NULL,
    criado_em TIMESTAMP NOT NULL
);
CREATE INDEX idx_tentativas_aluno_questionario ON tb_tentativas_questionario(aluno_id, questionario_id, versao, id);
CREATE TABLE tb_respostas_questionario (
    tentativa_id BIGINT NOT NULL REFERENCES tb_tentativas_questionario(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    alternativa INTEGER NOT NULL,
    PRIMARY KEY (tentativa_id, ordem)
);

CREATE TABLE tb_caminhos_questionarios (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao VARCHAR(2000) NOT NULL,
    pergunta_inicial VARCHAR(50),
    versao INTEGER NOT NULL DEFAULT 1,
    publicado BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE tb_caminhos_perguntas (
    id BIGSERIAL PRIMARY KEY,
    questionario_id BIGINT NOT NULL REFERENCES tb_caminhos_questionarios(id) ON DELETE CASCADE,
    chave VARCHAR(50) NOT NULL,
    enunciado VARCHAR(1000) NOT NULL,
    ordem INTEGER NOT NULL
);
CREATE TABLE tb_caminhos_opcoes (
    pergunta_id BIGINT NOT NULL REFERENCES tb_caminhos_perguntas(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    texto VARCHAR(500) NOT NULL,
    proxima_pergunta VARCHAR(50),
    resultado VARCHAR(50),
    PRIMARY KEY (pergunta_id, ordem)
);
CREATE TABLE tb_caminhos_resultados (
    id BIGSERIAL PRIMARY KEY,
    questionario_id BIGINT NOT NULL REFERENCES tb_caminhos_questionarios(id) ON DELETE CASCADE,
    chave VARCHAR(50) NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    orientacao VARCHAR(5000) NOT NULL,
    ordem INTEGER NOT NULL
);
CREATE TABLE tb_caminhos_temas_resultado (
    resultado_id BIGINT NOT NULL REFERENCES tb_caminhos_resultados(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    tema VARCHAR(150) NOT NULL,
    PRIMARY KEY (resultado_id, ordem)
);
CREATE TABLE tb_caminhos_respostas_aluno (
    id BIGSERIAL PRIMARY KEY,
    aluno_id BIGINT NOT NULL REFERENCES tb_usuarios(id) ON DELETE CASCADE,
    questionario_id BIGINT NOT NULL REFERENCES tb_caminhos_questionarios(id),
    versao INTEGER NOT NULL,
    resultado VARCHAR(50) NOT NULL,
    atualizado_em TIMESTAMP NOT NULL,
    CONSTRAINT uk_caminhos_resposta UNIQUE (aluno_id, questionario_id)
);
CREATE TABLE tb_caminhos_escolhas_aluno (
    resposta_id BIGINT NOT NULL REFERENCES tb_caminhos_respostas_aluno(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    pergunta VARCHAR(50) NOT NULL,
    opcao INTEGER NOT NULL,
    PRIMARY KEY (resposta_id, ordem)
);
CREATE TABLE tb_caminhos_secoes (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    conteudo VARCHAR(20000) NOT NULL,
    ordem INTEGER NOT NULL DEFAULT 1,
    publicado BOOLEAN NOT NULL DEFAULT FALSE
);
-- Atualiza o cadastro legado sem alterar a V1 nem os aceites registrados.
ALTER TABLE tb_usuarios DROP CONSTRAINT ck_tb_usuarios_dados_cadastro;
ALTER TABLE tb_usuarios DROP COLUMN cpf;
ALTER TABLE tb_usuarios DROP COLUMN cpf_hash;
ALTER TABLE tb_usuarios ADD CONSTRAINT ck_tb_usuarios_dados_cadastro CHECK (
    perfil = 'ADMINISTRADOR' OR (
        data_nascimento IS NOT NULL AND data_hora_aceite IS NOT NULL AND versao_termos IS NOT NULL
    )
);
ALTER TABLE tb_contas_excluidas DROP COLUMN cpf_hash;
