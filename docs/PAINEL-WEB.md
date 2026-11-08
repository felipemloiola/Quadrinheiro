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
