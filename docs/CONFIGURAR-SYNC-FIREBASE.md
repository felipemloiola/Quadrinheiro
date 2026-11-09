# Firebase do painel Quadrinheiro

## Situação atual — 09/11/2026

O aplicativo Web Firebase do painel está **configurado e habilitado** na branch `main`. O usuário concluiu no Firebase Console o cadastro Web, habilitou o provedor Google, autorizou o domínio e publicou as regras de segurança para a coleção `dashboardUsers/{uid}` no banco de dados já usado pelo app Android. A configuração pública foi incorporada ao site.

| Item | Valor |
| --- | --- |
| Projeto | `gen-lang-client-0375936605` |
| Domínio autorizado | `felipemloiola.github.io` |
| Aplicativo Web | `docs/cloud-config.json` |
| Provedor | Firebase Authentication · Google |
| Banco Firestore | `ai-studio-android-quadrinh-d0141987-7d2c-491d-b885-fc910c102b71` |
| Registro privado | `dashboardUsers/{uid}` |
| Anexos | `dashboardUsers/{uid}/photos/{photoId}/pieces/{index}` |
| Acesso | Conta Google autorizada com e-mail verificado e UID correspondente |
| Site | https://felipemloiola.github.io/Quadrinheiro/ |

**O código e as regras estarem configurados não garante que a autenticação e a sincronização real já foram testadas.** Isso requer abrir o site num navegador com acesso à conta Google.

## Testar a primeira entrada

1. Abra o [painel Quadrinheiro](https://felipemloiola.github.io/Quadrinheiro/).
2. Clique em **Continuar com Google** e selecione o e-mail autorizado.
3. O Firebase Authentication confirma a conta. Em seguida, o site procura `dashboardUsers/{uid}` no Firestore nomeado.
4. Caso ainda não exista documento na nuvem, o painel cria o primeiro registro usando os dados desse navegador.
5. Se já existir versão remota e os dados locais forem diferentes, o painel oferecerá **Usar dados da nuvem** ou **Enviar dados locais**.
6. Depois da sincronização, crie uma nota, feche o navegador e entre num segundo dispositivo com a **mesma conta Google** para confirmar a persistência.

Se o login não avançar, verifique o estado descrito na tela. Não desative as regras de segurança para contornar um erro.

## Onde estão os arquivos

- `docs/cloud-config.json`: configuração pública do aplicativo Web, com `enabled: true`, projeto, banco nomeado e conta autorizada.
- `docs/cloud.js`: login Google, proteção da área de trabalho, sincronização e recuperação de alterações pendentes.
- `docs/index.html` e `docs/login.css`: interface de login e painel.
- `docs/quadrinheiro-login-logo.svg`: marca colorida oficial, utilizada **somente** na entrada.
- `firestore.rules`: regras do aplicativo Android preservadas, além do bloco `dashboardUsers/{userId}`, espelhando a versão publicada pelo usuário.

### Privacidade

O GitHub Pages serve os arquivos HTML, CSS e JavaScript publicamente. A tela de login impede o uso regular do painel antes da autenticação, mas **não torna os arquivos do site privados**. Os documentos do Firestore são protegidos pelas regras implantadas no servidor. A lista de e-mails no JSON é um controle de interface, não um segredo.

As regras do painel publicadas exigem UID correto, provedor Google, e-mail verificado e o e-mail explicitamente autorizado. Não existe razão para desativar a autenticação, modificar as regras de outras coleções ou contratar um plano pago apenas para realizar o primeiro teste.

### Sincronização e fotos

O Firestore salva tarefas, notas, decisões, métricas e preferências. As fotos são divididas em pequenas partes e salvas em subcoleções privadas da mesma conta. Isso evita usar o Cloud Storage em um projeto Spark, mas é adequado apenas a uma quantidade moderada de imagens.

Os dados também ficam localmente salvos no navegador autenticado, por UID. Atualizações são enviadas quando o usuário modifica o conteúdo; alterações remotas são recebidas em tempo real. Se uma gravação falhar, a interface indica o problema e uma nova tentativa ocorre quando a conexão retorna. Para evitar conflitos, não edite a mesma nota simultaneamente em vários aparelhos. Faça backups JSON regularmente.

### Solução de problemas

- **Domínio não autorizado:** confirme `felipemloiola.github.io` em Firebase Authentication → Settings → Authorized domains.
- **Conta não autorizada:** entre com a conta que aparece em `allowedEmails` e nas regras do Firestore.
- **Permissão negada:** confirme que a versão das regras com `dashboardUsers` foi publicada no **banco nomeado correto**.
- **Nuvem indisponível:** verifique quotas, rede e se o Google Authentication está habilitado.
- **Imagens não sincronizam:** mantenha a aba aberta até a mensagem de salvamento concluído e confirme que todas as partes da imagem foram gravadas.
- **GitHub Pages mostra versão anterior:** atualize a página sem cache e aguarde o deploy da branch `main`.

Documentação oficial: [Firebase Auth](https://firebase.google.com/docs/auth/web/google-signin) · [Firestore named databases](https://firebase.google.com/docs/firestore/manage-databases) · [Security Rules](https://firebase.google.com/docs/firestore/security/get-started).
