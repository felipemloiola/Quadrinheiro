# Ativar login Google obrigatório e salvamento privado do Quadrinheiro

O painel agora tem uma **tela de entrada própria** e só abre a área de trabalho depois de autenticação via Google **e conexão com a versão privada dos dados no Firestore**. O site continua publicado no GitHub Pages, mas a interface fica bloqueada antes do login.

**Estado atual:** o código está pronto no GitHub. A autenticação não entra em funcionamento enquanto `docs/cloud-config.json` permanecer com `"enabled": false`, ou enquanto login Google e regras do Firestore não tiverem sido configurados no Firebase Console. Nesse estado, o site mostra a tela de entrada informando a configuração pendente. Os dados que estavam no navegador não são apagados.

O conector Firebase pode estar instalado no ChatGPT, mas a instalação de um conector não ativa automaticamente um app Firebase Web, domínios OAuth ou regras. Eles precisam ser configurados e implantados no projeto correto.

## 1. Preparar Firebase Web

1. Entre no [Firebase Console](https://console.firebase.google.com/) e abra o projeto Firebase utilizado pelo aplicativo Quadrinheiro.
2. Em **Visão geral do projeto → Adicionar app → Web (</>)**, registre um app chamado `Quadrinheiro Painel`. Se já existir um app Web, use a configuração dele.
3. Copie os valores **apiKey**, **authDomain**, **projectId** e **appId**. Esses valores compõem a **configuração pública de cliente Firebase**; não são chaves administrativas nem service account JSON.
4. Em **Authentication → Sign-in method**, habilite **Google**.
5. Em **Authentication → Settings → Authorized domains**, adicione `felipemloiola.github.io` (sem `https://`, sem `/Quadrinheiro`). Não é necessário habilitar login anônimo.

## 2. Habilitar acesso seguro ao Firestore

A regra isolada `match /projectDashboards/{uid}` já foi adicionada ao arquivo `firestore.rules` no repositório, **sem modificar as regras de coleções do app Android**.

Publique as regras para o banco utilizado pelo painel. O projeto usa um banco nomeado em `firebase.json`:

`ai-studio-android-quadrinh-d0141987-7d2c-491d-b885-fc910c102b71`

Você pode abrir **Firebase Console → Firestore Database**, selecionar esse banco, abrir a aba **Rules** e publicar o conteúdo completo do arquivo `firestore.rules` do repositório. Verifique com cuidado se esse é o banco utilizado pelo app.

Alternativamente, com o Firebase CLI autenticado no mesmo projeto, publique as regras do `firebase.json` (não execute isso em outro projeto).

**Segurança:** os documentos só permitem leitura/escrita quando `request.auth.uid` coincide com o identificador do documento; consultas globais são negadas. Fotos são armazenadas em partes privadas por UID, protegidas pelas mesmas condições. Nunca publique uma regra `allow read, write: if true`.

## 3. Configurar o site (uma única vez)

Abra [docs/cloud-config.json](https://github.com/felipemloiola/Quadrinheiro/blob/main/docs/cloud-config.json) e clique em **Edit this file**.

Preencha com os dados do aplicativo Web Firebase:

```json
{
  "enabled": true,
  "apiKey": "VALOR_WEB_FIREBASE",
  "authDomain": "ID_DO_PROJETO.firebaseapp.com",
  "projectId": "ID_DO_PROJETO",
  "appId": "1:...:web:...",
  "allowedEmails": ["seu-email@gmail.com"],
  "databaseId": "ai-studio-android-quadrinh-d0141987-7d2c-491d-b885-fc910c102b71"
}
```

Altere os campos de exemplo pelos valores reais do Firebase Web. **Não cole service accounts, tokens privados, senhas ou chaves de API do GitHub.**

Preencha **allowedEmails** com os endereços Google que podem entrar (por exemplo, somente o seu). Essa verificação melhora a experiência de acesso no navegador, mas **não é um controle de segurança do backend**: se deseja impedir que outras contas Google usem o serviço, também restrinja o e-mail/UID nas regras Firestore publicadas e teste as regras. Para a privacidade das suas próprias notas, a regra atual já separa os documentos por UID.

Faça o commit na branch `main`; o GitHub Pages publicará a configuração. Como é a mesma configuração Web em todos os aparelhos, você não terá de preencher essas informações novamente no celular.

### Nota sobre privacidade

O GitHub Pages deste repositório é público: não é possível impedir que outras pessoas baixem o HTML/JavaScript público apenas com uma tela de login. A interface bloqueia a navegação até a autenticação e o Firestore protege os dados de cada UID. Para **restringir até a existência de contas** a um único usuário, é necessário também implementar/validar uma restrição de UID ou e-mail nas regras do servidor, não apenas no JavaScript. A chave Web Firebase não protege os dados por si só; a proteção depende das regras acima.

## 4. Sincronizar os registros

1. Abra [o painel](https://felipemloiola.github.io/Quadrinheiro/). A tela de login aparecerá antes de qualquer seção.
2. Clique em **Continuar com Google** e utilize o endereço autorizado no `cloud-config.json`.
3. No primeiro acesso, se não houver versão remota, o painel envia seus registros locais para sua conta automaticamente. Dados legados são vinculados apenas à primeira conta autenticada nesse navegador.
4. Em outro dispositivo, entre com **a mesma conta Google**. Se ele estiver vazio, os dados da nuvem serão carregados; se já contiver registros diferentes, o painel apresentará opções antes de substituir a versão.
5. Após ativar, alterações em tarefas, notas, métricas, decisões, preferências e fotos são salvas e recebidas pelo Firestore. É possível encerrar a sessão em **Ajustes**.
6. Se ocorrer erro de conexão ou falta de regras, a área de trabalho permanece bloqueada, em vez de permitir acesso sem autenticação.

**Conflitos:** se houver dados locais diferentes na primeira conexão, o painel pede que você escolha qual versão manter. Em edições simultâneas no mesmo conteúdo, a última gravação pode prevalecer; não use edição colaborativa simultânea. Faça backups JSON antes de sobrescrever uma versão.

## 5. Fotos e limites

O painel usa Firestore para dados e fotos, dividindo imagens em partes menores de aproximadamente 150 KB. Isso evita depender do Cloud Storage for Firebase, que exige o plano Blaze para funcionar. Fotos não são expostas por URLs públicas.

O Firestore possui quotas sem cobrança para projetos elegíveis, mas **uso excessivo pode consumir essas quotas**. Muitas fotos ou backups grandes podem exigir uma arquitetura específica de mídia. O limite de documento do Firestore é de 1 MiB; o painel limita o conteúdo textual antes de enviar.

Os dados locais continuam no navegador se a rede falhar. A interface informa erros de permissão, autenticação e sincronização. Mantenha o backup JSON como segurança adicional.

## Solução de problemas

- **Configuração pendente:** `enabled` ainda é `false` ou faltam valores reais em `cloud-config.json`.
- **Domínio não autorizado:** configure `felipemloiola.github.io` em Authorized domains.
- **Acesso negado:** publique as regras do banco correto, usando o mesmo projeto que aparece na configuração Web.
- **Popup bloqueado:** permita a janela do Google no navegador.
- **Fotos ausentes:** carregue primeiro a versão da nuvem no aparelho que ainda não possui os anexos.
- **Erro de capacidade:** exporte o JSON e reduza imagens/quantidade de dados.

Fontes oficiais: [Firebase Web](https://firebase.google.com/docs/web/setup), [Google Sign-in](https://firebase.google.com/docs/auth/web/google-signin), [Firestore Rules](https://firebase.google.com/docs/firestore/security/get-started), [Quotas Firestore](https://firebase.google.com/docs/firestore/quotas).

## Sobre o modo exclusivo do Google

As regras em `firestore.rules` exigem que os documentos do painel sejam acessados pelo mesmo UID autenticado, com provedor `google.com` e e-mail verificado. A configuração `allowedEmails` restringe as contas aceitas pela interface, mas qualquer lista de e-mails publicada em código estático é visível e não deve ser tratada como segredo. A configuração Web do Firebase é **pública por design**; nunca coloque um service-account JSON nela.

O projeto guarda os dados no navegador em uma chave separada por UID; o conteúdo legado da instalação anterior é migrado somente para a primeira conta Google usada nesse navegador. Para migrar outra conta, exporte/importa um backup JSON conscientemente. A saída da conta recolhe a área de trabalho e não apaga o backup anterior do dispositivo.
