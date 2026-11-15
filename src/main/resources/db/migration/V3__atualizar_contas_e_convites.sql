-- Incremental sobre a V2: preserva os dados e os checksums do historico existente.
ALTER TABLE tb_usuarios ADD COLUMN excluido_em TIMESTAMP;
ALTER TABLE tb_usuarios DROP CONSTRAINT ck_tb_usuarios_dados_cadastro;
ALTER TABLE tb_usuarios ADD CONSTRAINT ck_tb_usuarios_dados_cadastro CHECK (
    excluido_em IS NOT NULL OR perfil = 'ADMINISTRADOR' OR (
        data_nascimento IS NOT NULL AND data_hora_aceite IS NOT NULL AND versao_termos IS NOT NULL
    )
);

-- Mantem os prazos legados, mas novas exclusoes nao dependem mais desse campo.
ALTER TABLE tb_contas_excluidas ALTER COLUMN bloqueada_ate DROP NOT NULL;

ALTER TABLE tb_convites_acesso ADD COLUMN criado_por BIGINT REFERENCES tb_usuarios(id);
ALTER TABLE tb_convites_acesso DROP CONSTRAINT tb_convites_acesso_email_key;
CREATE INDEX idx_convites_email ON tb_convites_acesso(email, id DESC);

CREATE TABLE tb_aceites_responsabilidade (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES tb_usuarios(id),
    perfil VARCHAR(30) NOT NULL CHECK (perfil IN ('PROFESSOR', 'ADMINISTRADOR')),
    termo_version VARCHAR(30) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    accepted_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_aceites_responsabilidade_usuario ON tb_aceites_responsabilidade(user_id, perfil, termo_version);
