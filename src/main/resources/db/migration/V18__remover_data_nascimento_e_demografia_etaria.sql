ALTER TABLE tb_usuarios DROP CONSTRAINT IF EXISTS ck_tb_usuarios_dados_cadastro;
ALTER TABLE tb_usuarios DROP CONSTRAINT IF EXISTS ck_tb_usuarios_idade_somente_aluno;

ALTER TABLE tb_usuarios DROP COLUMN IF EXISTS data_nascimento;

ALTER TABLE tb_usuarios ADD CONSTRAINT ck_tb_usuarios_dados_cadastro CHECK (
    excluido_em IS NOT NULL OR perfil = 'ADMINISTRADOR' OR (
        data_hora_aceite IS NOT NULL AND versao_termos IS NOT NULL
    )
);
