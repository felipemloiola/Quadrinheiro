# Painel do Quadrinheiro

Site público: https://felipemloiola.github.io/Quadrinheiro/

Publicado automaticamente pelo GitHub Pages a partir da branch `main`, diretório `docs/`. Atualize `docs/index.html` no repositório para publicar mudanças sem redistribuir arquivos HTML.

## Emulador Android dentro do site — Appetize

A aba **Emulador** incorpora o player Android do [Appetize](https://appetize.io) por um iframe oficialmente suportado. O APK real é executado na nuvem: o site funciona em PCs, celulares e tablets, sem instalação de Android Studio.

### Ativação única

1. Crie uma conta em https://appetize.io e gere um token de API em Organization → API Token.
2. No GitHub: **Settings → Secrets and variables → Actions → New repository secret**. Nome exato: `APPETIZE_API_TOKEN`; valor: token secreto.
3. Execute **Actions → Atualizar APK no Appetize → Run workflow** na branch `main`.
4. O workflow baixa `quadrinheiro.apk` da release mais recente, envia ao Appetize e cria/atualiza `docs/emulator-build.json` com o **Build ID público** (nunca com o token).
5. Abra https://felipemloiola.github.io/Quadrinheiro/#emulator e clique em **Play** no player. Em novas releases, o workflow repete o upload e atualiza a referência automaticamente.

O player só abre uma sessão Android quando o usuário toca em Play. Feche a sessão ao terminar para não desperdiçar a franquia mensal. Consulte o plano gratuito no Appetize: minutos são limitados.

### Identificador manual

Se um APK já estiver publicado no Appetize, é possível inserir seu **Build ID público** na aba Emulador, sem GitHub Actions. O navegador salva essa preferência localmente, sem armazenar credenciais. Ela prevalece sobre o identificador automático até ser alterada; para acompanhar as novas releases automaticamente, não preencha o campo manual.

### Arquivos

- `docs/index.html`: painel, métricas, backlog e player Appetize.
- `docs/quadrinheiro-logo.svg`: logotipo oficial do projeto.
- `.github/workflows/sync-appetize.yml`: upload e atualização do build público.
- `docs/emulator-build.json`: criado automaticamente pelo workflow após seu primeiro sucesso.

### Dados locais

Backlog, métricas, decisões e checklist são salvos em localStorage no navegador. Para migrar de um `index.html` local, exporte JSON na instalação anterior e importe no endereço publicado.

### Observações

O envio do APK e o embed são suportados oficialmente, mas a primeira sessão real ainda depende da configuração do token e da franquia do Appetize. O painel não pode acessar a chave API e não altera o repositório diretamente.

Documentação: https://docs.appetize.io/platform/embedding-apps
