# Painel web do Quadrinheiro

**Site publicado:** https://felipemloiola.github.io/Quadrinheiro/

O painel é publicado automaticamente pelo GitHub Pages, a partir da branch `main` e da pasta `docs/`. Para atualizar o site, basta modificar seus arquivos em `docs/` no GitHub. Não é necessário baixar ou instalar novos arquivos.

## Acesso obrigatório por conta Google

A página inicial usa uma tela de entrada exclusiva para **Google Sign-in**. A área de trabalho permanece oculta e sem interação até o Firebase Authentication confirmar a sessão e os dados privados estarem disponíveis. Se faltar a configuração Firebase Web, o site informa a pendência em vez de ignorar o login.

**Configuração no repositório:** `docs/cloud-config.json` está ativado para o projeto `gen-lang-client-0375936605`, com login Google, domínio `felipemloiola.github.io` e o banco Firestore nomeado do Android. As regras privadas `dashboardUsers/{uid}` foram publicadas no Firebase Console e espelhadas em `firestore.rules`. O funcionamento real deve ser confirmado por login e criação de uma nota em um navegador autorizado.

Os registros locais ficam separados pelo UID do usuário Google. No primeiro login, dados do armazenamento anterior podem ser migrados uma única vez para a primeira conta autenticada. Ao sair, a área de trabalho volta à tela de login. O armazenamento remoto é feito em `dashboardUsers/{uid}` e não altera as coleções dos usuários do aplicativo Android.

**Limitação da hospedagem:** GitHub Pages continua disponibilizando arquivos HTML/CSS/JS publicamente. A tela de login protege a interface do usuário comum, enquanto a privacidade real dos registros depende das regras implantadas no Firestore. `allowedEmails` filtra a interface e as regras publicadas também restringem o acesso ao e-mail Google autorizado, com UID correspondente e e-mail verificado.

## Seções disponíveis

- **Visão geral:** indicadores de tarefas, progresso e últimos marcos.
- **Repositório:** estrutura e referências ao projeto Android.
- **Backlog:** melhorias, prioridades, responsáveis, status e evidências.
- **Integrações:** acompanhamento das integrações de design e ferramentas planejadas.
- **Performance:** registro manual de medições e avaliação de desempenho.
- **Decisões:** histórico de decisões e próximos passos.
- **Notas:** ideias, rascunhos, pesquisas e referências com fotos, busca, fixação e salvamento automático.
- **Ajustes:** tema, cor de destaque, densidade da interface, acessibilidade, sincronização e backups.

## Dados e sincronização

O botão **Sincronizar GitHub** consulta informações públicas do repositório em modo somente leitura.

As tarefas, decisões, métricas e textos das notas continuam sendo salvos localmente em `localStorage`, e as fotos em `IndexedDB`. A exportação JSON inclui os textos e as imagens. **Sincronização privada entre dispositivos:** a configuração Web está habilitada; após login autorizado, o painel lê e grava em `dashboardUsers/{uid}` no Firestore nomeado. As alterações são sincronizadas entre aparelhos autenticados na mesma conta, sujeitas à disponibilidade de rede e quotas. Para instruções, leia [`CONFIGURAR-SYNC-FIREBASE.md`](CONFIGURAR-SYNC-FIREBASE.md).

## Emulador removido

A seção de emulador Android foi removida por solicitação do usuário. Também foram removidos do repositório os workflows `sync-appetize.yml` e `sync-spawndroid.yml`, além da configuração pública do build. Nenhum serviço externo de emulação é carregado pelo painel.

Segredos eventualmente cadastrados no GitHub permanecem armazenados até serem excluídos manualmente em **Settings → Secrets and variables → Actions**; após a remoção dos workflows, o site não utiliza `APPETIZE_API_TOKEN` nem `SPAWNDROID_API_KEY`.

## Arquivos

- `docs/index.html`: painel web.
- `docs/quadrinheiro-logo.svg`: logotipo original.
- `docs/PAINEL-WEB.md`: esta documentação.

## Identidade visual e acessibilidade

O painel adota a linguagem visual do aplicativo Android, referenciada em `app/src/main/java/com/example/ui/theme/Theme.kt` e `Type.kt`, e os princípios do arquivo `apple-design/SKILL.md` fornecido para a reformulação:

- Cores nativas: preto `#000000` e cinza-claro `#F2F2F7`, cartões neutros e azul do sistema `#0A84FF`.
- SF Pro Text/Display quando disponível no dispositivo, com fontes de sistema como alternativa; **nenhum arquivo de fonte da Apple é redistribuído**.
- Navegação flutuante no celular, superfície translúcida no cabeçalho, agrupamento por proximidade e hierarquia mais discreta.
- Resposta imediata à pressão do ponteiro, transições curtas e reversíveis; opção `prefers-reduced-motion` para eliminar deslocamentos.
- Alternativas de acessibilidade para menor transparência (`prefers-reduced-transparency`) e contraste reforçado (`prefers-contrast`).
- A visão geral mostra um anel de progresso **calculado dos critérios realmente marcados** nas tarefas; não representa evidência nem progresso declarado automaticamente.

**Arquivos:** `docs/index.html` contém os dados e a lógica da interface; `docs/visual.css` contém a camada visual revisada, carregada após os estilos antigos. Para atualizar o visual, altere `visual.css` e, se necessário, incremente o sufixo `?v=...` na referência de `index.html` para limpar caches.

## Notas e anexos de imagens

- Para criar uma nota, abra **Notas → Nova nota**, escreva um título e conteúdo, escolha **Ideia**, **Rascunho**, **Referência** ou **Pesquisa** e feche o editor: as alterações são salvas durante a digitação.
- Para anexar fotos, use a área **Adicionar fotos**, arraste imagens ou cole uma captura da área de transferência. São permitidas até 10 fotos por nota; arquivos individuais de até 15 MB são reduzidos quando necessário.
- As notas podem ser pesquisadas, filtradas por categoria, fixadas no topo, editadas e excluídas.
- As fotos são mantidas em armazenamento **local** do dispositivo/navegador, não em uma nuvem nem publicadas no repositório. Limpar os dados do site poderá apagá-las; faça backups periódicos com **Exportar** (inclui as fotos).
- Um backup pode ficar grande quando inclui muitas imagens. A importação aceita JSON até 120 MB, substituindo os registros anteriores após confirmação.
- O painel não sincroniza automaticamente notas entre celular e computador. Para sincronização em nuvem no futuro, será necessário configurar armazenamento privado com autenticação.

Arquivos relacionados: `docs/notes.js` (funcionalidade/IndexedDB) e `docs/notes.css` (interface da seção Notas).

## Visão geral — painel de atividade (revisão visual)

A página inicial agora adota uma organização compacta inspirada em painéis de atividade profissionais: fundo neutro, cartões de cantos suaves, ícones discretos, navegação lateral e tabelas de eventos legíveis.

- **Seis indicadores** calculados dos dados disponíveis: tarefas pendentes, em andamento, concluídas, com evidências, notas locais e última release do GitHub.
- **Backlog por área**: quantidade de tarefas e proporção de critérios de aceite marcados em até quatro áreas.
- **Evolução**: progresso dos checklists, tarefas P0 ainda abertas e total de medições cadastradas; não representa benchmark automático.
- **Atividade recente**: os últimos commits públicos retornados pela API do GitHub, com título, autor, hash, data e acesso à origem. A consulta ocorre ao abrir o site, ao usar **Sincronizar GitHub** e periodicamente enquanto a página está ativa. Falhas de rede exibem os últimos dados consultados ou um estado vazio, sem inventar registros.
- **Registros recentes**: notas, decisões e benchmarks criados no próprio navegador; nenhum deles é publicado pelo site no repositório.

A camada visual desta revisão fica em `docs/dashboard.css`, carregada depois de `visual.css` e `notes.css`. O HTML e as rotinas de exibição permanecem em `docs/index.html`. A versão móvel mantém as sete abas acessíveis pela barra inferior. Tema claro, movimento reduzido e contraste reforçado continuam disponíveis.

## Ajustes do painel (tema e cor como no app Android)

Acesse **Ajustes** pelo menu lateral/inferior ou pelo atalho no cabeçalho. As preferências são salvas no navegador em `quadrinheiro-ui-settings-v1` e acompanham a exportação/importação de JSON.

- **Modo de aparência**: Sistema (segue `prefers-color-scheme`), Claro ou Escuro. Uma preferência anterior do painel é preservada na primeira visita.
- **Cor de destaque**: Azul, Branco, Preto, Vermelho, Verde, Roxo e Laranja. Valores idênticos aos acentos efetivos de `AppTheme` e `getIOS27ThemeColors` em `app/src/main/java/com/example/ui/theme/Theme.kt` (variam entre claro e escuro). Branco e Preto usam os acentos especiais de contraste definidos pelo app, não apenas os valores das amostras da paleta.
- **Layout compacto**: reduz o espaçamento dos cartões, tabelas e conteúdos.
- **Reduzir animações**: desativa transições CSS sem remover controles ou funções.
- **Reduzir transparência**: troca materiais translúcidos por superfícies sólidas.
- **Sincronização automática**: quando ligada, consulta metadados GitHub ao abrir o painel e a cada hora enquanto a página estiver visível. Quando desligada, mantém o botão **Sincronizar GitHub** para atualização manual.
- **Dados e backups**: resumo dos registros locais e atalhos para exportar/importar um arquivo JSON que inclui as preferências e os anexos das notas.
- **Restaurar ajustes padrão**: redefine apenas preferências, sem apagar registros do projeto.

**Arquivos:** `docs/settings.js` implementa os controles, a persistência e os valores das cores; `docs/settings.css` contém os estilos. Nenhum token secreto de serviço é armazenado nesses controles. Esses ajustes pertencem ao painel web e não alteram as preferências do aplicativo Android.

## Navegação, ícones e organização do painel

A navegação do site foi simplificada para a finalidade deste painel: **Visão geral, Repositório, Backlog, Integrações, Performance, Decisões, Notas e Ajustes**. Todos são módulos do acompanhamento do desenvolvimento do Quadrinheiro; não representam opções do menu do app Android.

- Os ícones da navegação, Repositório, Integrações, resumos, medição e Ajustes agora compartilham o mesmo conjunto de SVGs de linha, com dimensões, espessura de traço e fundos padronizados. As integrações não usam mais letras como substituto dos ícones.
- O botão **Expandir/Recolher** no topo da barra lateral funciona em desktop e salva o modo em `localStorage`, chave `quadrinheiro-sidebar-v1`. Com a barra recolhida, os botões mantêm rótulos acessíveis e `title`; no celular, a navegação permanece como barra inferior.
- **Ajustes** fica apenas na navegação lateral ou inferior, sem um botão duplicado no cabeçalho. No desktop, aparece no final da lista.
- O estado expandido/recolhido acompanha o backup como `sidebarMode`.
- Foram mantidos os recursos pertinentes ao trabalho: consultas públicas ao GitHub, planejamento, evidências, métricas registradas, comparativo de referências técnicas, decisões e notas locais. A entrada `mobile-native`, voltada à interface web e não à performance do aplicativo Android, foi retirada da lista de skills de performance.
- Não foram adicionadas funcionalidades fictícias nem alterado o código do app. O painel continua sendo somente leitura em relação ao GitHub e os dados de progresso continuam locais.

Arquivos: `docs/navigation.css` e `docs/sidebar.js`. Os ícones SVG são renderizados pelo componente `ico()` no `docs/index.html`, reutilizado em `settings.js` e `notes.js`.

## Revisão de alinhamento e animação (09/11/2026)

- `docs/polish.css` padroniza os alinhamentos dos títulos, botões, cartões, grades e linhas de atividade na Visão geral. As colunas usam `minmax(0, 1fr)` para evitar conteúdo cortado; *container queries* reorganizam os painéis conforme a largura disponível, mesmo com a barra lateral recolhida.
- O botão de expandir/recolher da barra lateral utiliza o símbolo de **painel dividido** da referência: um retângulo de cantos arredondados com divisória vertical, sem setas que alteram a aparência a cada clique.
- A página ganhou transições curtas de entrada, feedback de toque e mudanças suaves de estado. A implementação segue os princípios de movimento funcional das [Human Interface Guidelines da Apple](https://developer.apple.com/design/human-interface-guidelines/motion): movimento breve, com significado e possibilidade de redução. Os controles de redução de movimento do painel e `prefers-reduced-motion` desativam esses efeitos.
- Notas agora têm **Baixar .md** no cartão e no editor. O arquivo usa UTF-8 e contém título, categoria, datas e texto escrito pelo usuário. Para evitar links quebrados, fotos locais não são incluídas nem representadas como URLs inventadas no Markdown; seus nomes são listados, e os binários continuam disponíveis por **Exportar backup JSON**.
- Foi corrigido também o estado vazio da aba Notas, que apontava para um identificador inválido do componente compartilhado de ícones.

A atualização foi projetada para manter todos os registros locais e as oito abas existentes.

## Logo temático e barra lateral

- O `Q` na barra lateral usa a mesma geometria do logotipo Android com preenchimento `var(--accent)`, sem quadrado de fundo. O favicon também se adapta à cor e ao tema.
- O logo **continua visível quando o menu é recolhido**. Na barra lateral, o eixo horizontal de todos os ícones é fixo; apenas os textos mudam de opacidade/recorte ao alternar o estado. O logo na versão recolhida funciona como botão de expansão ao receber clique/toque, mostrando o ícone de dividir painel quando o cursor passa por ele.
- O bloco inferior de texto extenso da barra lateral foi removido. O estado de dados e sincronização é apresentado em **Ajustes**.
- Implementação: `docs/smooth-sidebar.css`, `docs/sidebar.js` e `docs/settings.js`.

## Salvamento privado em qualquer dispositivo

- Arquivos: `docs/cloud.js`, `docs/cloud.css`, `docs/cloud-config.json` e `firestore.rules`.
- **Agora ativado:** o Firebase Web usa o banco nomeado e a coleção `dashboardUsers/{uid}`. Ao autenticar, carrega, salva e monitora registros privados via Firestore. Notas e fotos também podem ser transferidas entre dispositivos. A primeira conexão resolve diferenças entre versão local e remota antes de substituir registros.
- O `cloud-config.json` público não pode conter segredos administrativos. O site no GitHub Pages continua publicamente acessível, mas as coleções privadas só autorizam leitura/escrita do respectivo UID autenticado; as regras específicas do painel já foram publicadas no Firebase Console (09/11/2026). Commits GitHub futuros não publicam alterações adicionais automaticamente.
- Fotos são armazenadas em partes privadas no Firestore, evitando a exigência atual de plano Blaze para Cloud Storage. O acesso gratuito depende das quotas do Firestore e do banco escolhido. Backups JSON continuam recomendados.

## Integração final de login e marca colorida (09/11/2026)

- `docs/cloud-config.json`: aplicativo Web Firebase registrado, Google Auth habilitado, banco nomeado `ai-studio-android-quadrinh-d0141987-7d2c-491d-b885-fc910c102b71` e `allowedEmails` com a conta autorizada.
- `docs/cloud.js`: documentos em `dashboardUsers/{uid}`, fotos em `dashboardUsers/{uid}/photos/{id}/pieces/{n}`, sincronização em tempo real e reenvio das alterações locais pendentes ao recuperar a conexão.
- `firestore.rules`: regras do painel alinhadas à versão publicada manualmente no Firebase, com verificação Google, e-mail e UID. Regras do app Android preservadas.
- `docs/quadrinheiro-login-logo.svg`: SVG recortado da **logo colorida oficial**, com rosa, âmbar, azul e ciano. O destaque cromático da entrada é discreto; a logo Q da barra lateral continua monocromática e adaptável ao tema.
- **Limite de validação:** alterações no GitHub não constituem prova de login real ou gravação no Firestore. Teste em navegador com a conta Google autorizada é necessário para confirmação ponta a ponta.
