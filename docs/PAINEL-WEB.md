# Painel web do Quadrinheiro

**Site publicado:** https://felipemloiola.github.io/Quadrinheiro/

O painel é publicado automaticamente pelo GitHub Pages, a partir da branch `main` e da pasta `docs/`. Para atualizar o site, basta modificar `docs/index.html` no GitHub. Não é necessário baixar ou instalar novos arquivos.

## Seções disponíveis

- **Visão geral:** indicadores de tarefas, progresso e últimos marcos.
- **Repositório:** estrutura e referências ao projeto Android.
- **Backlog:** melhorias, prioridades, responsáveis, status e evidências.
- **Integrações:** acompanhamento das integrações de design e ferramentas planejadas.
- **Performance:** registro manual de medições e avaliação de desempenho.
- **Decisões:** histórico de decisões e próximos passos.
- **Notas:** ideias, rascunhos, pesquisas e referências com fotos, busca, fixação e salvamento automático.

## Dados e sincronização

O botão **Sincronizar GitHub** consulta informações públicas do repositório em modo somente leitura.

As tarefas, decisões, métricas e textos das notas são salvos localmente no navegador usando `localStorage`. As fotos anexadas às notas ficam no `IndexedDB`, para não sobrecarregar o `localStorage`. A exportação JSON inclui os textos e as imagens referenciadas pelas notas; a importação restaura ambos. Os registros não são sincronizados automaticamente entre dispositivos. Use backups para migrar de aparelho.

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
