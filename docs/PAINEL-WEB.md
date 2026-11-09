# Painel Web Quadrinheiro

O centro de trabalho do Quadrinheiro é publicado em [felipemloiola.github.io/Quadrinheiro](https://felipemloiola.github.io/Quadrinheiro/) a partir da pasta `docs/` na branch `main`.

## Arquitetura

O site é um projeto estático com HTML, CSS e JavaScript. A interface usa a tipografia e os temas inspirados no aplicativo Android, um menu lateral expansível e a logo colorida na tela de entrada. A palavra **“continuar.”** apresenta um gradiente animado da direita para a esquerda, respeitando `prefers-reduced-motion`.

O painel exige login Google com a conta autorizada. O acesso aos registros depende das regras publicadas no Firestore. Apesar da tela de entrada, os arquivos públicos do GitHub Pages continuam publicamente acessíveis; **somente os dados protegidos pelo backend são privados**.

### Dados somente na nuvem

- **Firestore:** documento `dashboardUsers/{uid}` para tarefas, backlog, checklists, notas, decisões, métricas, próximos passos e preferências.
- **Fotos:** documentos sob `dashboardUsers/{uid}/photos/{id}/pieces/{n}`. O upload da imagem termina no servidor antes de ela ser referenciada pela nota.
- **Sem armazenamento local do conteúdo:** não há gravações de registros em `localStorage` e não há novas fotos salvas em `IndexedDB`. Uma cópia temporária em **RAM** é utilizada durante a sessão e descartada após recarregar ou sair.
- **Autenticação persistente:** o Firebase Authentication usa `browserLocalPersistence` **somente para manter a sessão Google ativa** após F5. Isso é necessário para não precisar fazer login sempre; não armazena as notas, fotos ou o backlog.
- **Somente respostas confirmadas:** dados do painel são obtidos do servidor Firestore. Se não há rede, a área de trabalho não abre com um backup local. Edições em andamento precisam ser enviadas à nuvem antes de sair.

### Migração de versões anteriores

O código oferece **migração única** de arquivos locais antigos, quando presentes, para o Firestore. Se um documento remoto já existe, os dados antigos vão para `dashboardUsers/{uid}/legacyBackups` sem sobrescrever o conteúdo principal; caso contrário, são usados para criar a primeira versão remota. Fotos legadas são transferidas antes de apagar o antigo IndexedDB. Se a transferência falhar, os arquivos locais antigos são preservados para evitar perda de dados e uma mensagem de alerta é exibida.

Backups JSON e downloads Markdown são opções manuais. Não são usados para manter a sincronização entre dispositivos.

## Seções

- **Visão geral:** indicadores, atividade pública do GitHub, progresso e registros recentes.
- **Repositório:** organização do app Android, do serviço de catálogo e das automações.
- **Backlog:** prioridades, status, responsáveis, checklists e evidências.
- **Integrações:** acompanhamento de Apple skills, Apple Design Skill e Prismal.
- **Performance:** métricas medidas e referências de otimização.
- **Decisões:** histórico de escolhas e próximos passos.
- **Notas:** ideias, rascunhos, anexos e exportação Markdown individual.
- **Ajustes:** temas/cor de destaque, acessibilidade, backup manual, status da nuvem e **Sair da conta**.

A sincronização automática dos metadados públicos do repositório usa a API GitHub em modo leitura; não modifica o código Android.

## Arquivos

| Arquivo | Papel |
| --- | --- |
| `docs/index.html` | Navegação, dados do projeto em RAM, edição e interface principal |
| `docs/cloud.js` | Autenticação Google durável, leitura/gravação do Firestore, migração e anexos |
| `docs/cloud-config.json` | Configuração pública do Firebase Web e do banco nomeado |
| `docs/notes.js` | Notas, fotos com cache temporário em memória e download Markdown |
| `docs/settings.js` | Temas e preferências sincronizadas pelo Firestore |
| `docs/sidebar.js` | Expansão/recolhimento; preferência salva no Firestore |
| `docs/login.css` | Tela de login e animação de identidade visual |
| `firestore.rules` | Espelho das regras de acesso implementadas no backend |

Mais detalhes: [CONFIGURAR-SYNC-FIREBASE.md](CONFIGURAR-SYNC-FIREBASE.md).

## Testes recomendados

1. Entre com sua conta Google e crie uma nota.
2. Aguarde o status **Tudo salvo na nuvem** e pressione **F5**.
3. Confirme que você **permanece autenticado** e que a nota volta do Firestore.
4. Abra o site em outro dispositivo com a mesma conta e compare a nota.
5. Use **Sair da conta**; a área de trabalho deve ser bloqueada imediatamente.
6. Teste com rede indisponível: o site não deve afirmar que dados ainda não enviados foram salvos.
7. Teste anexos e a exportação Markdown/JSON.
