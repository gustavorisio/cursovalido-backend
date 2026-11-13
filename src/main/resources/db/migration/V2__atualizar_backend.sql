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
