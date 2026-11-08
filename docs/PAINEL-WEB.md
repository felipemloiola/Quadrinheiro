# Painel web do Quadrinheiro

Site: [Quadrinheiro · Projeto](https://felipemloiola.github.io/Quadrinheiro/) (após ativar o GitHub Pages).

## Publicação automática sem baixar `index.html`

O site está em `docs/index.html`, acompanhado de `docs/quadrinheiro-logo.svg`. O GitHub Pages publica o diretório `docs/` da branch `main`. Ative uma única vez em:

**Settings → Pages → Build and deployment → Source: Deploy from a branch → Branch: main → Folder: /docs → Save.**

Endereço esperado: https://felipemloiola.github.io/Quadrinheiro/

Atualize o arquivo `docs/index.html` no repositório para publicar futuras versões automaticamente; não é preciso baixar nem instalar o HTML. Pode haver atraso de publicação/cache e basta atualizar a página.

Se usava o site como arquivo local, os dados de backlog, métricas e decisões ficam em `localStorage` do endereço antigo. Use **Exportar** antes e **Importar** no site publicado, uma vez, para migrar os registros.

## SpawnDroid

A aba Emulador apresenta o acesso direto ao app já enviado na sua conta:
https://app.spawndroid.io/dashboard/apps/quadrinheiro

O console funciona em uma aba autenticada. A tentativa de incorporar o console via `iframe` é **experimental**: não há garantia documentada de que o provedor permita incorporação ou cookies de sessão por outro site. O link direto é o caminho recomendado. O painel não simula uma sessão nem conhece seu status remoto.

### Atualização automática do APK

Workflow: `.github/workflows/sync-spawndroid.yml`.

1. SpawnDroid → **API & CI/CD → API Keys** → crie chave com scope `upload`.
2. GitHub → Settings → Secrets and variables → Actions → New repository secret. Nome exato: `SPAWNDROID_API_KEY`.
3. GitHub Actions → **Atualizar APK no SpawnDroid** → **Run workflow**.

Depois disso, toda nova release com `quadrinheiro.apk` será enviada e o workflow também fará reenvio periódico para mitigar o prazo de retenção de 7 dias do plano Hobby. Upload **não inicia** uma sessão e **não consome** device-minutes. Abra o console manualmente para iniciar o teste.

O plano Hobby exibido na captura do usuário permite **200 device-minutes por mês**, com uma sessão paralela; o consumo ocorre enquanto o dispositivo está ativo. Pare a sessão no SpawnDroid ao terminar.

Nunca coloque chave API no HTML ou no repositório. Somente secrets do GitHub Actions.

## Observações

O site é estático e não grava no GitHub: tarefas, decisões e notas são salvas no navegador local. O sync GitHub lê apenas informações públicas.
