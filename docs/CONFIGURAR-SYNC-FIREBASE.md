# Firebase do painel Quadrinheiro — autenticação persistente e dados somente na nuvem

## Configuração atual (09/11/2026)

O painel está habilitado para Firebase Authentication (Google) e Firestore no projeto existente do app Android.

| Item | Configuração |
| --- | --- |
| Projeto Firebase | `gen-lang-client-0375936605` |
| Aplicativo Web | `docs/cloud-config.json` |
| Domínio autorizado | `felipemloiola.github.io` |
| Método de entrada | Google, com endereço autorizado e verificado |
| Firestore | Banco nomeado `ai-studio-android-quadrinh-d0141987-7d2c-491d-b885-fc910c102b71` |
| Documento principal | `dashboardUsers/{uid}` |
| Fotos | `dashboardUsers/{uid}/photos/{id}/pieces/{n}` |
| Versões antigas | `dashboardUsers/{uid}/legacyBackups/{id}` (migração única) |

As regras de segurança do painel foram publicadas no Firebase Console; o repositório contém uma cópia no arquivo `firestore.rules`. Atualizar esse arquivo no GitHub **não publica automaticamente novas regras no Firebase**.

## Comportamento esperado

1. Abra o [painel](https://felipemloiola.github.io/Quadrinheiro/).
2. Faça login com a conta Google autorizada. A sessão fica persistente por meio do **Firebase Auth LOCAL**, até que você clique em **Sair da conta**. Após F5, o Auth restaura a sessão e o site busca os dados no Firestore.
3. O documento principal é carregado **diretamente do Firestore** (sem cache local persistente) e disponibilizado assim que confirmado. Fotos, backup antigo e consultas auxiliares não impedem a abertura do painel.
4. Tarefas, notas, decisões, métricas, preferências de tema e barra lateral são gravadas no documento privado. Fotos são enviadas diretamente a subcoleções privadas, antes de aparecerem como anexadas.
5. Em outro dispositivo, entre com a mesma conta Google para acessar a mesma versão na nuvem.

**Exceção indispensável:** o Firebase Authentication guarda **apenas a sessão de autenticação no próprio navegador**, para você não precisar entrar novamente depois de F5. Isso não é um backup do conteúdo do projeto. Navegadores em modo privado ou que bloqueiam o armazenamento podem não preservar a sessão.

**Sem modo offline de dados:** enquanto uma alteração não for confirmada pelo Firestore, o site a mantém apenas na memória RAM da aba. Ao fechar a página antes de sincronizar, ela poderá ser perdida. O site avisa sobre salvamentos pendentes.

## Migração segura dos registros antigos

Versões anteriores usavam `localStorage` para tarefas/notas e `IndexedDB` para fotos. Ao entrar, o painel verifica se existe esse material antigo associado à conta autenticada.

- Quando não existe documento remoto, transfere os dados e as fotos antigas para o Firestore e **só depois** apaga as cópias antigas.
- Quando já existe um documento remoto, guarda a versão antiga em `legacyBackups`, sem substituir o conteúdo atual.
- Se o upload, a leitura de uma imagem antiga ou a limpeza falhar, a cópia antiga **não é apagada silenciosamente**. A tela informa que a migração precisa ser concluída.

O código novo não grava registros de trabalho em `localStorage` nem fotos em `IndexedDB`. A exportação JSON ou Markdown é uma ação manual e opcional, para o usuário guardar um arquivo de backup onde preferir.

## Problemas de autenticação

- **Sai após F5:** limpe apenas a sessão anterior pelo botão **Sair da conta**, entre novamente e teste em uma janela comum (não anônima), sem bloqueio de armazenamento do site. O Auth usa `browserLocalPersistence`.
- **Domínio não autorizado:** confira `felipemloiola.github.io` em Firebase Authentication → Settings → Authorized domains.
- **Conta não autorizada:** use o e-mail Google definido em `allowedEmails` e nas regras Firestore.
- **Permissão negada:** confirme o banco Firestore nomeado e a publicação das regras `dashboardUsers/{uid}`.
- **Falha de rede:** o site não oferece cópia offline dos registros; aguarde a conexão.
- **Versão anterior no GitHub Pages:** force a atualização com Ctrl + Shift + R.

Documentação oficial: [persistência de sessão Firebase Auth](https://firebase.google.com/docs/auth/web/auth-state-persistence) · [Firestore Web](https://firebase.google.com/docs/firestore/manage-data/enable-offline) · [regras Firestore](https://firebase.google.com/docs/firestore/security/get-started).

## Se a tela de carregamento não terminar

A partir de 09/11/2026, a inicialização possui limite de espera para a leitura principal do Firestore (**16 segundos**), exibindo o erro e **Repetir conexão** em vez de manter o splash sem fim. As preferências possuem limite próprio (**5,5 segundos**); se sua consulta falhar, o painel utiliza temporariamente os valores encontrados no documento principal. Fotos e migração do banco local antigo são executadas sem bloquear o acesso a uma conta que já tem dados remotos.

Caso o erro continue, a mensagem na tela indica qual etapa falhou. Recarregue a página e verifique a conexão, o banco nomeado e as regras publicadas no Firebase. Se houver dados antigos no navegador, eles não serão excluídos sem confirmação de backup remoto.
