-- Remove os textos legais demonstrativos das migrações históricas em um banco novo.
-- Em bancos com usuários, preserva os documentos e aceites já publicados.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM tb_usuarios) THEN
        DELETE FROM tb_politicas;
        DELETE FROM tb_politicas_privacidade;
        UPDATE tb_versoes_documentos SET numero = 0 WHERE tipo IN ('TERMOS', 'PRIVACIDADE');
    END IF;
END;
$$;
