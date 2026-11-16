CREATE TABLE tb_cadastro_vagas (
    chave VARCHAR(48) PRIMARY KEY
);

INSERT INTO tb_cadastro_vagas (chave)
SELECT endereco_ip || ':' || CAST(vaga AS VARCHAR)
FROM (
    SELECT endereco_ip, COUNT(DISTINCT usuario_id) AS total
    FROM tb_logs_auditoria
    WHERE tipo_acao IN ('CADASTRO_USUARIO', 'CADASTRO_PENDENTE_APROVACAO')
      AND endereco_ip IS NOT NULL AND endereco_ip <> ''
    GROUP BY endereco_ip
) cadastros
CROSS JOIN (VALUES (1), (2)) AS vagas(vaga)
WHERE cadastros.total >= vagas.vaga;
