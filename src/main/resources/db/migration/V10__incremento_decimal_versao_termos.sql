-- Converte somente o contador dos termos. Documentos e aceites mantem as versoes legadas.
UPDATE tb_versoes_documentos
SET numero = (
    SELECT GREATEST(COALESCE(MAX(FLOOR(substring(versao FROM '^v?([0-9]+[.]?[0-9]*)$')::NUMERIC * 5)), 0) - 4, 0)::BIGINT
    FROM tb_politicas
)
WHERE tipo = 'TERMOS';
