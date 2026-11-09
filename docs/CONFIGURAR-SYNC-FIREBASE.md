# Ativar salvamento privado do painel em qualquer dispositivo

O Quadrinheiro tem um painel estático publicado no GitHub Pages. A navegação e os dados públicos podem ser vistos sem login, mas **as notas, imagens e alterações pessoais podem ficar privadas no Firestore por conta Google** após a ativação abaixo.

> **Estado atual:** a integração está implementada no código, mas não está ativa enquanto `docs/cloud-config.json` contiver `"enabled": false`. Não existe um serviço de sincronização que funcione sem configurar um backend e autenticação.

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
  "databaseId": "ai-studio-android-quadrinh-d0141987-7d2c-491d-b885-fc910c102b71"
}
```

Altere os campos de exemplo pelos valores reais do Firebase Web. **Não cole service accounts, tokens privados, senhas ou chaves de API do GitHub.**

Faça o commit na branch `main`; o GitHub Pages publicará a configuração. Como é a mesma configuração Web em todos os aparelhos, você não terá de preencher essas informações novamente no celular.

### Nota sobre privacidade

O GitHub Pages deste repositório é público: não é possível impedir que outras pessoas acessem o HTML do painel apenas usando essas regras. O que fica privado são os dados sincronizados no Firebase, separados por conta Google. A chave Web Firebase não protege os dados por si só; a proteção depende das regras acima.

## 4. Sincronizar os registros

1. Abra [Ajustes](https://felipemloiola.github.io/Quadrinheiro/#settings).
2. Na seção **Sincronização entre dispositivos**, clique em **Entrar com Google** e use a mesma conta em todos os aparelhos.
3. **Na primeira vez**, clique em **Enviar dados deste aparelho** para criar sua versão privada na nuvem.
4. No celular ou outro computador, entre com a mesma conta. Escolha **Carregar da nuvem** se houver dados locais diferentes. Se o navegador for novo, a versão remota é carregada automaticamente.
5. Depois da primeira conexão, as alterações em tarefas, notas, métricas, decisões, preferências e fotos são salvas automaticamente. Outra aba autenticada recebe atualizações pela API do Firestore.

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
