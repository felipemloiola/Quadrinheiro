# Painel Web Quadrinheiro

O centro de trabalho do Quadrinheiro é publicado em [felipemloiola.github.io/Quadrinheiro](https://felipemloiola.github.io/Quadrinheiro/) a partir da pasta `docs/` na branch `main`.

## Arquitetura

O site é um projeto estático com HTML, CSS e JavaScript. A interface usa a tipografia e os temas inspirados no aplicativo Android, um menu lateral expansível e a logo colorida na tela de entrada. A palavra **“continuar.”** apresenta uma onda diagonal (135°) com gradiente repetitivo de período exato de 200 px, atravessando o texto a cada 2,65 segundos, sem salto na repetição, e respeitando `prefers-reduced-motion`.

O painel exige login Google com a conta autorizada. O acesso aos registros depende das regras publicadas no Firestore. Apesar da tela de entrada, os arquivos públicos do GitHub Pages continuam publicamente acessíveis; **somente os dados protegidos pelo backend são privados**.

### Dados somente na nuvem

- **Firestore:** documento `dashboardUsers/{uid}` para tarefas, backlog, checklists, notas, decisões, métricas e próximos passos. O painel mantém uma cópia compatível de `uiSettings`, mas a fonte autoritativa do tema e da cor é `dashboardUsers/{uid}/preferences/appearance`.
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

## Splash e transição da navegação (09/11/2026)

A primeira tela exibida pelo HTML é o **splash com a marca colorida** e uma barra de progresso indeterminada. Os estados `checking` e `loading` do Firebase ocultam totalmente o formulário, enquanto a sessão Google é restaurada e o documento privado é lido do Firestore. O formulário aparece apenas com `signed-out`, `setup` ou `error`; o painel aparece somente com `authenticated`. O texto do splash acompanha os estágios reais do carregamento. As animações respeitam a opção de reduzir movimento.

Com a barra **recolhida**, o `Q` é o próprio botão para expandir. Com a barra **aberta**, o `Q` permanece estático e o botão de recolher aparece em uma coluna própria do cabeçalho. Nenhum dos dois controles é desenhado por cima do outro. Os ícones de navegação permanecem ancorados; apenas os textos aparecem/desaparecem. `aria-expanded` e os rótulos acessíveis acompanham cada estado.

**Ajustes** e **Sair da conta** ficam em um rodapé próprio, fixo no fim da barra nos dois modos, não dentro da lista rolável de páginas. No celular, Ajustes continua sendo a oitava opção da barra inferior, e Sair da conta permanece nos Ajustes. Ao sair, o Firebase mostra o estado de encerramento imediatamente; uma alteração ainda pendente exige confirmação explícita antes de descartar o que não foi sincronizado.

## Favicon e preferências por conta (09/11/2026)

- Enquanto a sessão estiver em **carregamento**, **login** ou **logout**, a guia usa a marca **colorida oficial** `quadrinheiro-login-logo.svg`.
- Após a autenticação (`data-auth="authenticated"`), o favicon passa ao **Q personalizado** com a cor selecionada. Sair da conta restaura a marca colorida.
- Temas e cores são gravados explicitamente em `dashboardUsers/{uid}/preferences/appearance` no Firestore. Gravações consecutivas são serializadas para que a última escolha prevaleça.
- Na entrada ou após F5, o site recupera essas preferências diretamente do servidor, aplicando-as antes de mostrar o painel. Se o documento ainda não existir, importa as preferências do documento principal e cria a configuração dedicada.
- Uma falha de gravação é mostrada ao usuário; selecionar uma cor não significa, por si só, que o servidor confirmou a persistência.

## Interface responsiva para smartphones (09/11/2026)

A interface móvel reutiliza os **mesmos componentes e tokens de tema da versão desktop**, com distribuição adaptada para telas estreitas:

- **Dock com cinco destinos:** Visão geral, Backlog, Notas, Ajustes e Mais. O menu **Mais** abre Repositório, Integrações, Performance e Decisões, preservando todas as oito áreas do site.
- **Cartões de dados:** KPIs em duas colunas, áreas do backlog em cartões, tabelas de tarefas e medições apresentadas como itens legíveis com indicador de edição e sem rolagem lateral forçada.
- **Anotações e configurações:** notas em coluna única, filtros responsivos, seletores de cor e tema com áreas de toque maiores, formulários com texto de 16 px para reduzir zoom involuntário no iOS.
- **Ações contextuais:** barra superior mais compacta, modais como folhas inferiores adaptadas às áreas seguras dos aparelhos e navegação por toque e teclado.
- **Acessibilidade:** seletor de seções com `aria-expanded`, fechamento por Escape, retorno do foco ao gatilho e respeito à configuração de movimento reduzido.

Implementação: `docs/mobile.css`, `docs/mobile.js` e os pontos de montagem em `docs/index.html`. Os dados continuam no Firestore e a navegação móvel usa o roteador existente, sem criar uma segunda instância de estado.

**Validação:** as verificações automatizadas de sintaxe e o teste simulado de abertura, seleção, fechamento e estado do menu passaram. O layout ainda deve ser conferido visualmente em aparelhos reais de diferentes larguras antes de ser considerado validado em produção.
