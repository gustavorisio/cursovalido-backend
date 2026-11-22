-- Atualiza contas existentes e preserva a unicidade mesmo em caso de colisão.
DO $$
DECLARE
    usuario_id BIGINT;
    nova_tag TEXT;
    alfabeto CONSTANT TEXT := 'abcdefghijklmnopqrstuvwxyz0123456789';
BEGIN
    FOR usuario_id IN SELECT id FROM tb_usuarios WHERE gamertag !~ '^#[a-z0-9]{8}$'
        OR gamertag !~ '[a-z]' OR gamertag !~ '[0-9]'
    LOOP
        LOOP
            SELECT '#' || string_agg(substr(alfabeto, 1 + floor(random() * 36)::integer, 1), '' ORDER BY n)
            INTO nova_tag FROM generate_series(1, 8) AS n;
            IF nova_tag !~ '[a-z]' OR nova_tag !~ '[0-9]' THEN CONTINUE; END IF;
            BEGIN
                UPDATE tb_usuarios SET gamertag = nova_tag WHERE id = usuario_id;
                EXIT;
            EXCEPTION WHEN unique_violation THEN
                -- Sorteia novamente sem alterar a identidade ou as publicações.
                NULL;
            END;
        END LOOP;
    END LOOP;
END $$;

ALTER TABLE tb_forum_users ADD COLUMN gamertag VARCHAR(15);
UPDATE tb_forum_users f SET gamertag = u.gamertag FROM tb_usuarios u WHERE u.id = f.id;
