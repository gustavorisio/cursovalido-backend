CREATE TABLE tb_recompensas_questionarios (
    id BIGSERIAL PRIMARY KEY,
    aluno_id BIGINT NOT NULL REFERENCES tb_usuarios(id) ON DELETE CASCADE,
    questionario_id BIGINT NOT NULL REFERENCES tb_questionarios(id) ON DELETE CASCADE,
    xp_concedido INTEGER NOT NULL CHECK (xp_concedido > 0),
    concedido_em TIMESTAMP NOT NULL,
    CONSTRAINT uk_recompensa_aluno_questionario UNIQUE (aluno_id, questionario_id)
);

-- O saldo e o histórico anteriores permanecem intactos. Apenas novas aprovações
-- de questionários passam a conceder XP, uma única vez por aluno e questionário.
