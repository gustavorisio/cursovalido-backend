-- Publica os documentos atualizados, sem coleta ou análise de dados de nascimento.
-- A nova versão faz com que o aceite dos documentos atualizados seja solicitado.
WITH proxima AS (
    UPDATE tb_versoes_documentos
       SET numero = numero + 1
     WHERE tipo = 'TERMOS'
 RETURNING format('v%s.%s', (10 + 2 * (numero - 1)) / 10,
                  mod(10 + 2 * (numero - 1), 10)) AS versao
), arquivadas AS (
    UPDATE tb_politicas SET ativa = FALSE WHERE ativa = TRUE RETURNING id
)
INSERT INTO tb_politicas (nome, versao, conteudo, ativa, data_publicacao)
SELECT 'Termo de Aceite', proxima.versao, $curso_valido_termos$
<!DOCTYPE html>
<html lang="pt-BR">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Termos de Aceite - Curso Valido</title>
    <style>
        body {
            font-family: Inter, "Segoe UI", Arial, sans-serif;
            line-height: 1.6;
            margin: 0;
            color: #333333;
            background: #f2f2f2;
        }

        main {
            max-width: 860px;
            margin: 32px auto;
            padding: 32px;
            background: #fff;
        }

        h1 {
            color: #333333;
        }

        h2 {
            margin-top: 28px;
            color: #005e9e;
        }

        a {
            color: #0076c5;
        }

        .atualizacao {
            color: #595959;
        }

        .navegacao {
            display: flex;
            gap: 16px;
            flex-wrap: wrap;
            margin-bottom: 28px;
        }

        .botao {
            border: 1px solid #0076c5;
            border-radius: 9999px;
            padding: 10px 16px;
            color: #0076c5;
            background: #fff;
            cursor: pointer;
            font-size: 1rem;
        }

        .botao:hover,
        .botao:focus {
            color: #fff;
            background: #0076c5;
        }
            a:focus-visible, button:focus-visible { outline: 3px solid #0076c5; outline-offset: 3px; }
        .botao { display: inline-flex; align-items: center; justify-content: center; min-height: 44px; box-sizing: border-box; text-decoration: none; }
        @media (max-width: 640px) { main { margin: 0; padding: 20px; } }
    </style>
</head>

<body>
    <main>
        <nav class="navegacao" aria-label="Navegacao do documento">
            <button class="botao" type="button" onclick="voltar()">Voltar</button>
            <a class="botao" href="/politica-privacidade.html">Politica de Privacidade</a>
        </nav>
        <h1>Termo de Aceite do Curso Valido</h1>
        <p class="atualizacao"><strong>Versao 2.0</strong> | Documento disponivel permanentemente em <a
                href="/termos-aceite.html">Termo de Aceite</a>.</p>

        <h2>1. Objeto</h2>
        <p>Este termo define as regras para uso da plataforma educacional Curso Valido, incluindo cadastro,
            autenticacao, controle de acesso, forum, comunicacoes por e-mail e auditoria. O aceite e necessario para
            criar e utilizar uma conta.</p>

        <h2>2. Plataforma gratuita e elegibilidade</h2>
        <p>O Curso Valido e uma plataforma totalmente gratuita. Nao ha venda de cursos, assinatura, mensalidade,
            cobranca, pagamento ou reembolso associado ao uso descrito neste termo. O acesso gratuito nao elimina as
            regras de seguranca, os requisitos de cadastro ou as responsabilidades do usuario.</p>
        <p>A plataforma e destinada exclusivamente a pessoas com 18 anos completos ou mais. No cadastro, cada usuario
            deve confirmar que atende a esse requisito.</p>
        <p>Professores e administradores ingressam somente por convite enviado ao e-mail pela administracao. O link
            e pessoal, vale por 48 horas e pode ser usado uma unica vez. No cadastro, tambem confirmam que tem 18
            anos ou mais.</p>

        <h2>3. Declaracoes do usuario</h2>
        <p>Ao criar ou utilizar uma conta, o usuario declara que forneceu informacoes verdadeiras, completas e atuais,
            que respeita as regras de idade descritas acima e que informara a administracao sobre qualquer alteracao
            relevante em seus dados.</p>

        <h2>4. Dados e privacidade</h2>
        <p>Este Termo apresenta diretamente os dados e usos essenciais para o aceite. No cadastro, o sistema coleta:
            nome completo, e-mail, senha, declaracao de maioridade e aceite dos documentos. Durante o uso, tambem trata matricula, perfil, status da
            conta, confirmacao de e-mail, versao/data/hora do aceite, tokens de confirmacao e 2FA, IP, registros de
            auditoria e topicos e comentarios publicados no forum.</p>
        <p>Esses dados sao utilizados diretamente para criar a conta, registrar a declaracao de que o usuario tem 18 anos ou mais,
            autenticar o acesso, enviar e-mails de confirmacao, redefinicao de senha e 2FA, aplicar permissoes,
            disponibilizar o forum, proteger o sistema e cumprir obrigacoes legais.
            O Resend recebe somente o e-mail e o conteudo necessario para as mensagens transacionais. O projeto nao
            coleta dados de pagamento, nao vende dados pessoais e nao usa os dados para publicidade direcionada.</p>
        <p>As senhas e tokens nao sao armazenados em texto puro. O aceite registra a versao, a data e a hora; tokens
            possuem prazo de expiracao. Os dados sao mantidos enquanto necessarios para a conta, seguranca, auditoria
            ou obrigacao legal, podendo ser removidos ou anonimizados quando a conta for excluida. O titular pode
            solicitar confirmacao, acesso, correcao, atualizacao, anonimização, eliminacao quando aplicavel,
            informacoes sobre compartilhamento, portabilidade quando regulamentada e revogacao do consentimento pelo
            canal oficial do Curso Valido.</p>

        <h2>5. Aceite e registro</h2>
        <p>O usuario declara que leu a <a href="/politica-privacidade.html">Politica de Privacidade</a> e esta ciente
            dos dados pessoais coletados, das finalidades, do tratamento realizado, do compartilhamento com servicos
            necessarios e dos direitos do titular. O aceite registra a versao, a data e a hora no sistema.</p>

        <h2>6. Conta e seguranca</h2>
        <p>O usuario deve manter sua senha em sigilo, nao compartilhar credenciais e comunicar suspeitas de acesso
            indevido. O sistema pode utilizar confirmacao de e-mail, codigo de dupla verificacao, JWT e regras de
            permissao para proteger a conta.</p>

        <h2>7. Uso da plataforma e do forum</h2>
        <p>O usuario deve utilizar a plataforma para finalidades educacionais, respeitar os demais participantes e
            publicar somente conteudo licito, verdadeiro e relacionado ao forum. Publicacoes com palavras ofensivas,
            xingamentos ou termos pejorativos sao recusadas automaticamente. Qualquer participante pode denunciar
            topicos e comentarios; as denuncias sao analisadas pela moderacao, que pode remover o conteudo. No forum, alunos aparecem com o primeiro
            nome e a inicial do sobrenome, e o e-mail nunca e exibido aos demais participantes. E proibido tentar acessar contas,
            dados ou funcionalidades sem autorizacao, explorar falhas ou prejudicar a disponibilidade do sistema.</p>

        <h2>8. Suspensao e encerramento</h2>
        <p>O acesso pode ser suspenso ou encerrado em caso de dados falsos, violacao deste termo, uso indevido,
            rejeicao administrativa ou revogacao do aceite. A revogacao dos termos pode impedir o uso de funcionalidades
            que dependem do tratamento dos dados.</p>

        <h2>9. Atualizacao do termo</h2>
        <p>Uma nova versao pode ser publicada para refletir alteracoes no sistema, na finalidade do tratamento ou na
            legislacao. A versao aceita deve corresponder a uma politica ativa publicada pelo Curso Valido. Quando
            necessario, o sistema solicitara novo aceite antes de liberar o acesso.</p>

        <h2>10. Direitos e contato</h2>
        <p>O usuario pode exercer os direitos descritos na Politica de Privacidade e solicitar esclarecimentos a
            administracao do Curso Valido pelo canal oficial da instituicao.</p>

        <h2>Cadastro e exclusao de conta</h2>
        <p>A conta e identificada pelo e-mail. O usuario confirma que tem 18 anos ou mais e e responsavel pela
            veracidade das informacoes fornecidas no cadastro.</p>
        <p>A exclusao exige confirmacao por codigo enviado ao e-mail da conta. Apos a exclusao, um hash do e-mail,
            o identificador da conta e a data de fim do bloqueio sao mantidos para impedir novo cadastro com o mesmo
            e-mail por 15 dias. Registros de moderacao podem ser associados a uma nova conta com o mesmo e-mail
            para preservar restricoes aplicadas, pelo prazo necessario a essa finalidade e ao exercicio de direitos.</p>
    </main>
    <script>
        const caminhos = new Set(["/termos-aceite.html", "/politica-privacidade.html"]);
        const documentos = new Map([[location.pathname, document.querySelector("main").innerHTML]]);
        let atual = location.pathname;
        let carregando = false;
        const origem = document.referrer ? new URL(document.referrer) : null;
        const retorno = origem && !caminhos.has(origem.pathname) ? origem.href : "/";
        function voltar() {
            if (window.parent !== window) return;
            if (window.opener) window.close();
            else window.location.replace(retorno);
        }
        document.addEventListener("click", async event => {
            const link = event.target.closest("a[href]");
            if (!link || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
            const destino = new URL(link.href);
            if (destino.origin !== location.origin || !caminhos.has(destino.pathname)) return;
            event.preventDefault();
            if (carregando || atual === destino.pathname) return;
            carregando = true;
            const main = document.querySelector("main");
            main.setAttribute("aria-busy", "true");
            try {
                if (!documentos.has(destino.pathname)) {
                    const resposta = await fetch(destino.pathname, {credentials: "omit"});
                    if (!resposta.ok) throw new Error("Documento indisponivel");
                    const pagina = new DOMParser().parseFromString(await resposta.text(), "text/html");
                    const conteudo = pagina.querySelector("main");
                    if (!conteudo) throw new Error("Documento indisponivel");
                    documentos.set(destino.pathname, conteudo.innerHTML);
                }
                main.innerHTML = documentos.get(destino.pathname);
                atual = destino.pathname;
                document.title = main.querySelector("h1").textContent;
                window.scrollTo(0, 0);
                main.setAttribute("tabindex", "-1");
                main.focus({preventScroll: true});
            } catch (erro) {
                let aviso = document.getElementById("erro-documento");
                if (!aviso) {
                    aviso = document.createElement("p");
                    aviso.id = "erro-documento";
                    aviso.setAttribute("role", "alert");
                    main.prepend(aviso);
                }
                aviso.textContent = "Nao foi possivel abrir o documento. Tente novamente mais tarde.";
            } finally {
                carregando = false;
                main.removeAttribute("aria-busy");
            }
        });
    </script>
</body>

</html>

$curso_valido_termos$, TRUE, CURRENT_TIMESTAMP
  FROM proxima CROSS JOIN (SELECT count(*) FROM arquivadas) AS _;

WITH proxima AS (
    UPDATE tb_versoes_documentos
       SET numero = numero + 1
     WHERE tipo = 'PRIVACIDADE'
 RETURNING numero::text || '.0' AS versao
), arquivadas AS (
    UPDATE tb_politicas_privacidade SET ativa = FALSE WHERE ativa = TRUE RETURNING id
)
INSERT INTO tb_politicas_privacidade (nome, versao, conteudo, ativa, data_publicacao)
SELECT 'Política de Privacidade', proxima.versao, $curso_valido_privacidade$
<!DOCTYPE html>
<html lang="pt-BR">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Politica de Privacidade - Curso Valido</title>
    <style>
        body {
            font-family: Inter, "Segoe UI", Arial, sans-serif;
            line-height: 1.6;
            margin: 0;
            color: #333333;
            background: #f2f2f2;
        }

        main {
            max-width: 860px;
            margin: 32px auto;
            padding: 32px;
            background: #fff;
        }

        h1 {
            color: #333333;
        }

        h2 {
            margin-top: 28px;
            color: #005e9e;
        }

        a {
            color: #0076c5;
        }

        .atualizacao {
            color: #595959;
        }

        .navegacao {
            display: flex;
            gap: 16px;
            flex-wrap: wrap;
            margin-bottom: 28px;
        }

        .botao {
            border: 1px solid #0076c5;
            border-radius: 9999px;
            padding: 10px 16px;
            color: #0076c5;
            background: #fff;
            cursor: pointer;
            font-size: 1rem;
        }

        .botao:hover,
        .botao:focus {
            color: #fff;
            background: #0076c5;
        }

        table {
            width: 100%;
            border-collapse: collapse;
            margin: 16px 0;
        }

        th,
        td {
            border: 1px solid #d6d6d6;
            padding: 10px;
            text-align: left;
            vertical-align: top;
        }

        th {
            background: #edf6fc;
        }

        @media (max-width: 640px) {
            main {
                margin: 0;
                padding: 20px;
            }

            table {
                font-size: .9rem;
            }
        }
            a:focus-visible, button:focus-visible { outline: 3px solid #0076c5; outline-offset: 3px; }
        .botao { display: inline-flex; align-items: center; justify-content: center; min-height: 44px; box-sizing: border-box; text-decoration: none; }
        @media (max-width: 640px) { main { margin: 0; padding: 20px; } }
    </style>
</head>

<body>
    <main>
        <nav class="navegacao" aria-label="Navegacao do documento">
            <button class="botao" type="button" onclick="voltar()">Voltar</button>
            <a class="botao" href="/termos-aceite.html">Termo de Aceite</a>
        </nav>
        <h1>Politica de Privacidade do Curso Valido</h1>
        <p class="atualizacao"><strong>Versao 2.0</strong> | Documento disponivel permanentemente em <a
                href="/politica-privacidade.html">/politica-privacidade.html</a>.</p>

        <h2>1. Responsavel e abrangencia</h2>
        <p>Esta politica explica como a administracao do projeto educacional Curso Valido trata dados pessoais de
            alunos, professores, administradores e pessoas convidadas para criar uma conta. O canal oficial de
            atendimento da instituicao responsavel pelo curso e utilizado para pedidos de privacidade e direitos do
            titular.</p>

        <h2>2. Dados pessoais coletados</h2>
        <p><strong>Dados:</strong> nome completo e e-mail.</p>
        <p><strong>Origem e funcionalidade:</strong> formulario de cadastro, login, convite e recuperacao de conta.</p>
        <p><strong>Finalidade e acesso:</strong> identificar a conta e enviar confirmacao, redefinicao de senha e codigo
            2FA. O e-mail e acessado pela administracao autorizada e pelo servico de envio Resend.</p>
        <p><strong>Retencao:</strong> enquanto a conta existir e pelo periodo necessario para obrigacoes e auditoria.
        </p>

        <p><strong>Dados:</strong> declaracao de que o usuario tem 18 anos ou mais.</p>
        <p><strong>Origem e funcionalidade:</strong> confirmacao apresentada no formulario de cadastro.</p>
        <p><strong>Finalidade e acesso:</strong> registrar que o usuario confirmou o requisito etario da plataforma.</p>
        <p><strong>Retencao:</strong> enquanto necessaria para comprovar os dados e aceites do cadastro.</p>

        <p><strong>Dados:</strong> hash BCrypt da senha.</p>
        <p><strong>Origem e funcionalidade:</strong> cadastro e login.</p>
        <p><strong>Finalidade e acesso:</strong> autenticar o usuario. A senha original nao e armazenada; somente os
            dados criptograficos necessarios a verificacao.</p>
        <p><strong>Retencao:</strong> enquanto a conta estiver ativa, com exclusao quando deixar de ser necessaria.</p>

        <p><strong>Dados:</strong> matricula, perfil, status, conta ativa e e-mail confirmado.</p>
        <p><strong>Origem e funcionalidade:</strong> gerados ou atualizados pelo cadastro, aprovacao e administracao.
        </p>
        <p><strong>Finalidade e acesso:</strong> aplicar RBAC e liberar as funcionalidades de aluno, professor ou
            administrador.</p>
        <p><strong>Retencao:</strong> enquanto a conta e os registros de responsabilizacao forem necessarios.</p>

        <p><strong>Dados:</strong> versao, data e hora do aceite dos termos e da Politica.</p>
        <p><strong>Origem e funcionalidade:</strong> checkboxes de aceite no cadastro e aceite de nova versao.</p>
        <p><strong>Finalidade e acesso:</strong> comprovar qual documento foi apresentado e aceito. A administracao
            consulta esses registros para governanca.</p>
        <p><strong>Retencao:</strong> pelo tempo necessario para comprovar o aceite e cumprir obrigacoes.</p>

        <p><strong>Dados:</strong> tokens de e-mail e codigo 2FA.</p>
        <p><strong>Origem e funcionalidade:</strong> confirmacao de e-mail, redefinicao de senha, convite e login em
            duas etapas.</p>
        <p><strong>Finalidade e acesso:</strong> validar uma operacao especifica. O banco armazena somente hash do
            token/codigo, com expiracao e uso unico.</p>
        <p><strong>Retencao:</strong> ate expiracao, utilizacao ou invalidacao; depois deixam de ser validos.</p>

        <p><strong>Dados:</strong> IP, data, hora, tipo da operacao e detalhes da auditoria.</p>
        <p><strong>Origem e funcionalidade:</strong> requisicoes de cadastro, login, forum, politicas e operacoes
            administrativas.</p>
        <p><strong>Finalidade e acesso:</strong> prevenir fraude, investigar incidentes e demonstrar a integridade das
            operacoes. Acesso restrito a administradores.</p>
        <p><strong>Retencao:</strong> pelo prazo necessario para seguranca, auditoria e exercicio regular de direitos.
        </p>

        <p><strong>Dados:</strong> topicos, comentarios, nome, perfil e identificador do autor.</p>
        <p><strong>Origem e funcionalidade:</strong> publicacoes realizadas no forum.</p>
        <p><strong>Finalidade e acesso:</strong> disponibilizar a comunidade de aprendizagem, exibir autoria e permitir
            moderacao.</p>
        <p><strong>Retencao:</strong> enquanto a publicacao e os registros de moderacao forem necessarios; exclusoes
            podem ser logicas.</p>
        <p>Por ser gratuito, o projeto nao solicita dados de pagamento e nao usa os dados acima para venda de produtos
            ou
            publicidade direcionada.</p>

        <h2>3. Finalidades do tratamento</h2>
        <ul>
            <li>criar, identificar e administrar contas de usuarios;</li>
            <li>autenticar o acesso, enviar confirmacoes, redefinicoes de senha e codigo de dupla verificacao;</li>
            <li>aplicar perfis e permissoes de aluno, professor e administrador;</li>
            <li>verificar a maioridade obrigatoria, o aceite dos termos e os requisitos de cadastro;</li>
            <li>disponibilizar o forum e vincular publicacoes aos seus autores;</li>
            <li>prevenir uso indevido, investigar incidentes e manter auditoria das operacoes;</li>
            <li>atender obrigacoes legais e solicitacoes do titular.</li>
        </ul>

        <h2>4. Bases legais</h2>
        <p>Conforme a finalidade, o tratamento se apoia na execucao das funcionalidades educacionais solicitadas, no
            cumprimento de obrigacoes legais, no exercicio regular de direitos, na protecao da seguranca do sistema e,
            quando aplicavel, no consentimento manifestado pelo aceite. A versao, a data e a hora do aceite sao
            registradas para comprovar a escolha feita pelo usuario.</p>

        <h2>5. Como o tratamento e realizado</h2>
        <p>O sistema coleta os dados enviados nos formularios, valida as informacoes, grava os registros no banco
            PostgreSQL, atualiza o status da conta conforme as regras de aprovacao e usa os dados para executar as
            funcionalidades descritas nesta politica. A senha nao e gravada em texto puro: o sistema utiliza BCrypt
            que armazena o hash com o salt incorporado.</p>

        <h2>6. Compartilhamento e servicos externos</h2>
        <p>O sistema utiliza o Resend para enviar confirmacao, convite, redefinicao de senha e
            autenticacao 2FA; para isso, compartilha o e-mail do destinatario e o conteudo da mensagem. O Curso Valido
            nao vende dados pessoais. Compartilhamentos legais podem ocorrer quando exigidos por autoridade competente.
        </p>

        <h2>7. Retencao, seguranca e exclusao</h2>
        <p>Os dados permanecem enquanto a conta e as finalidades do sistema estiverem ativas ou enquanto houver
            necessidade legal ou de auditoria. O titular pode solicitar a exclusao da conta; quando a exclusao ocorrer,
            registros necessarios para comprovar o aceite ou a auditoria podem ser preservados ou anonimizados conforme
            a finalidade e a obrigacao aplicavel. O acesso e protegido por JWT, controle de permissoes e registros de
            auditoria. Tokens deixam de ser validos quando expiram ou sao utilizados. Dados que nao precisarem ser
            mantidos serao removidos ou anonimizados quando a conta for excluida.</p>

        <h2>8. Direitos do titular</h2>
        <p>O titular pode solicitar confirmacao da existencia de tratamento, acesso, correcao, atualizacao,
            anonimização, bloqueio ou eliminacao quando aplicavel, informacoes sobre compartilhamento, portabilidade
            quando regulamentada, revogacao do consentimento e revisao de decisoes automatizadas quando aplicavel.
            A revogacao dos termos pode bloquear o acesso a funcionalidades que dependem do aceite.</p>

        <h2>9. Como exercer os direitos</h2>
        <p>Solicitacoes devem ser encaminhadas a administracao do Curso Valido pelo canal informado pela instituicao,
            com identificacao suficiente para evitar atendimento a pessoa incorreta. A solicitacao sera analisada e
            respondida conforme a legislacao aplicavel.</p>

        <h2>10. Atualizacoes</h2>
        <p>A versao vigente permanece acessivel nesta pagina. Quando uma alteracao exigir novo aceite, o sistema
            solicitara a confirmacao antes de liberar as funcionalidades correspondentes.</p>

        <h2>11. Documento relacionado</h2>
        <p>O aceite de uso da plataforma esta descrito no <a href="/termos-aceite.html">Termo de Aceite do Curso
                Valido</a>.</p>

        <h2>Cadastro e exclusao de conta</h2>
        <p>A conta e identificada pelo e-mail. O usuario confirma que tem 18 anos ou mais e e responsavel pela
            veracidade das informacoes fornecidas no cadastro.</p>
        <p>A exclusao exige confirmacao por codigo enviado ao e-mail da conta. Apos a exclusao, um hash do e-mail,
            o identificador da conta e a data de fim do bloqueio sao mantidos para impedir novo cadastro com o mesmo
            e-mail por 15 dias. Registros de moderacao podem ser associados a uma nova conta com o mesmo e-mail
            para preservar restricoes aplicadas, pelo prazo necessario a essa finalidade e ao exercicio de direitos.</p>
    </main>
    <script>
        const caminhos = new Set(["/termos-aceite.html", "/politica-privacidade.html"]);
        const documentos = new Map([[location.pathname, document.querySelector("main").innerHTML]]);
        let atual = location.pathname;
        let carregando = false;
        const origem = document.referrer ? new URL(document.referrer) : null;
        const retorno = origem && !caminhos.has(origem.pathname) ? origem.href : "/";
        function voltar() {
            if (window.parent !== window) return;
            if (window.opener) window.close();
            else window.location.replace(retorno);
        }
        document.addEventListener("click", async event => {
            const link = event.target.closest("a[href]");
            if (!link || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
            const destino = new URL(link.href);
            if (destino.origin !== location.origin || !caminhos.has(destino.pathname)) return;
            event.preventDefault();
            if (carregando || atual === destino.pathname) return;
            carregando = true;
            const main = document.querySelector("main");
            main.setAttribute("aria-busy", "true");
            try {
                if (!documentos.has(destino.pathname)) {
                    const resposta = await fetch(destino.pathname, {credentials: "omit"});
                    if (!resposta.ok) throw new Error("Documento indisponivel");
                    const pagina = new DOMParser().parseFromString(await resposta.text(), "text/html");
                    const conteudo = pagina.querySelector("main");
                    if (!conteudo) throw new Error("Documento indisponivel");
                    documentos.set(destino.pathname, conteudo.innerHTML);
                }
                main.innerHTML = documentos.get(destino.pathname);
                atual = destino.pathname;
                document.title = main.querySelector("h1").textContent;
                window.scrollTo(0, 0);
                main.setAttribute("tabindex", "-1");
                main.focus({preventScroll: true});
            } catch (erro) {
                let aviso = document.getElementById("erro-documento");
                if (!aviso) {
                    aviso = document.createElement("p");
                    aviso.id = "erro-documento";
                    aviso.setAttribute("role", "alert");
                    main.prepend(aviso);
                }
                aviso.textContent = "Nao foi possivel abrir o documento. Tente novamente mais tarde.";
            } finally {
                carregando = false;
                main.removeAttribute("aria-busy");
            }
        });
    </script>
</body>

</html>

$curso_valido_privacidade$, TRUE, CURRENT_TIMESTAMP
  FROM proxima CROSS JOIN (SELECT count(*) FROM arquivadas) AS _;