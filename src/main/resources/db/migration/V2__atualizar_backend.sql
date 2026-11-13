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
