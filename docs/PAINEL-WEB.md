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

## Dados e sincronização

O botão **Sincronizar GitHub** consulta informações públicas do repositório em modo somente leitura.

As tarefas, decisões, métricas e anotações são salvas localmente no navegador usando `localStorage`. A exportação e importação de JSON permite transferir ou fazer backup do progresso. Os registros não são sincronizados automaticamente entre dispositivos.

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
