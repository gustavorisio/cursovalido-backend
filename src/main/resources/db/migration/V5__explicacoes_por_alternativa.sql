CREATE TABLE tb_explicacoes_alternativa (
    questao_id BIGINT NOT NULL REFERENCES tb_questoes(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    explicacao VARCHAR(2000) NOT NULL,
    PRIMARY KEY (questao_id, ordem)
);

INSERT INTO tb_explicacoes_alternativa (questao_id, ordem, explicacao)
SELECT a.questao_id, a.ordem, CASE WHEN a.ordem = q.alternativa_correta THEN '' ELSE q.explicacao END
FROM tb_alternativas_questao a JOIN tb_questoes q ON q.id = a.questao_id;

ALTER TABLE tb_questoes DROP COLUMN explicacao;

-- Mantém o histórico das avaliações antigas, sem disponibilizá-las fora dos módulos.
UPDATE tb_questionarios SET publicado = FALSE WHERE modulo_id IS NULL;
