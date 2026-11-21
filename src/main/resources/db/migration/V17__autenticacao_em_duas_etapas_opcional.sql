-- Mantem o 2FA ativo para contas existentes; novas contas passam a iniciar desativadas.
ALTER TABLE tb_usuarios ADD COLUMN dois_fatores_ativos BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE tb_usuarios ALTER COLUMN dois_fatores_ativos SET DEFAULT FALSE;
