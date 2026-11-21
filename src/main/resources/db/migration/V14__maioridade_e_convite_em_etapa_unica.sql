-- A plataforma volta a aceitar somente pessoas com 18 anos completos.
-- Supervisao, verificacao de idade e controle de uso deixam de existir.

-- Contas de responsavel: acessos revogados e dados pessoais desidentificados.
DELETE FROM tb_tokens_email
WHERE usuario_id IN (SELECT id FROM tb_usuarios WHERE perfil = 'RESPONSAVEL');

UPDATE tb_sessoes SET revogada = TRUE
WHERE usuario_id IN (SELECT id FROM tb_usuarios WHERE perfil = 'RESPONSAVEL');

UPDATE tb_usuarios
SET perfil = 'ALUNO',
    ativo = FALSE,
    email_confirmado = FALSE,
    excluido_em = COALESCE(excluido_em, CURRENT_TIMESTAMP),
    nome_completo = 'Usuario excluido',
    email = 'excluido-' || id || '@conta.invalid',
    senha = '',
    data_nascimento = NULL,
    matricula = 'excluido-' || id
WHERE perfil = 'RESPONSAVEL';

-- Cadastros que aguardavam o responsavel nao podem mais ser liberados.
UPDATE tb_usuarios SET status_aprovacao = 'REJEITADO', ativo = FALSE
WHERE status_aprovacao = 'PENDENTE_RESPONSAVEL';

-- Alunos ativos com menos de 18 anos: acesso suspenso e sessoes revogadas.
UPDATE tb_sessoes SET revogada = TRUE
WHERE usuario_id IN (
    SELECT id FROM tb_usuarios
    WHERE perfil = 'ALUNO' AND ativo AND excluido_em IS NULL
      AND data_nascimento > current_date - interval '18 years'
);

INSERT INTO tb_suspensoes_usuarios (id_usuario, escopo, motivo, criada_em)
SELECT u.id, 'CONTA', 'O Curso Válido é destinado somente a pessoas com 18 anos completos ou mais.', CURRENT_TIMESTAMP
FROM tb_usuarios u
WHERE u.perfil = 'ALUNO' AND u.ativo AND u.excluido_em IS NULL
  AND u.data_nascimento > current_date - interval '18 years'
  AND NOT EXISTS (
      SELECT 1 FROM tb_suspensoes_usuarios s
      WHERE s.id_usuario = u.id AND s.escopo = 'CONTA' AND s.expira_em IS NULL AND s.revogada_em IS NULL
  );

DROP TABLE IF EXISTS tb_uso_diario;
DROP TABLE IF EXISTS tb_configuracoes_supervisao;
DROP TABLE IF EXISTS tb_verificacoes_idade;
DROP TABLE IF EXISTS tb_vinculos_responsaveis;

-- Convites: controle de envios e no maximo um convite pendente por e-mail.
ALTER TABLE tb_convites_acesso ADD COLUMN enviado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tb_convites_acesso ADD COLUMN quantidade_envios INTEGER NOT NULL DEFAULT 1;
UPDATE tb_convites_acesso SET enviado_em = criado_em;
ALTER TABLE tb_convites_acesso ADD CONSTRAINT ck_convites_quantidade_envios CHECK (quantidade_envios >= 1);

-- Convites pendentes duplicados (gerados por cliques repetidos): fica somente o mais recente.
UPDATE tb_convites_acesso c SET status = 'CANCELADO'
WHERE c.status = 'PENDENTE'
  AND EXISTS (
      SELECT 1 FROM tb_convites_acesso mais_recente
      WHERE mais_recente.email = c.email AND mais_recente.status = 'PENDENTE' AND mais_recente.id > c.id
  );

CREATE UNIQUE INDEX uk_convites_pendente_por_email ON tb_convites_acesso(email) WHERE status = 'PENDENTE';
CREATE INDEX idx_convites_criado_por ON tb_convites_acesso(criado_por, criado_em);
