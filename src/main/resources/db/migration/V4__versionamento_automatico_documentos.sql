-- Preserva os documentos e aceites existentes; inicializa contadores por tipo.
CREATE TABLE tb_versoes_documentos (
    tipo VARCHAR(30) PRIMARY KEY,
    numero BIGINT NOT NULL CHECK (numero >= 0)
);

-- Versoes numericas legadas (inclusive 2.3) avancam para o proximo inteiro (3.0).
-- Rotulos nao numericos sao preservados, mas nao participam da sequencia numerica.
INSERT INTO tb_versoes_documentos (tipo, numero)
SELECT 'TERMOS', COALESCE(MAX(substring(versao FROM '^([0-9]+)([.][0-9]+)?$')::BIGINT), 0)
FROM tb_politicas;
INSERT INTO tb_versoes_documentos (tipo, numero)
SELECT 'PRIVACIDADE', COALESCE(MAX(substring(versao FROM '^([0-9]+)([.][0-9]+)?$')::BIGINT), 0)
FROM tb_politicas_privacidade;

-- Nao reescrever duplicatas legadas: isso alteraria a referencia de aceites.
-- Se houver duplicatas nos termos, a migracao falha para permitir investigacao.
ALTER TABLE tb_politicas ADD CONSTRAINT uk_politicas_versao UNIQUE (versao);
