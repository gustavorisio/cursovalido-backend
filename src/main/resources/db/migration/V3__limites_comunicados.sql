CREATE TABLE tb_publicacoes_comunicados (
    autor_id BIGINT PRIMARY KEY REFERENCES tb_usuarios(id) ON DELETE CASCADE,
    ultima_publicacao_em TIMESTAMP NOT NULL
);

INSERT INTO tb_publicacoes_comunicados (autor_id, ultima_publicacao_em)
SELECT autor_id, MAX(criado_em) FROM tb_comunicados
WHERE autor_id IS NOT NULL GROUP BY autor_id;
