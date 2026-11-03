# Quadrinheiro

<p align="center"><img src="branding/quadrinheiro-logo.svg" alt="Quadrinheiro" width="120" /></p>

Aplicativo Android para organizar quadrinhos, acompanhar leituras e registrar a história da sua coleção. Desenvolvido em Kotlin e Jetpack Compose, com materiais translúcidos, iluminação inspirada nas capas e controles com resposta por molas.

[Baixar a versão mais recente](https://github.com/felipemloiola/Quadrinheiro/releases/latest) · [Reportar um problema](https://github.com/felipemloiola/Quadrinheiro/issues)

## Recursos

- **Início:** leitura atual, progresso e metas, com acesso rápido às suas histórias.
- **Explorar:** recomendações baseadas nas leituras, avaliações e editora favorita; catálogo com carregamento por páginas e remoção de duplicatas. A aba Comunidade reúne publicações, avaliações, resenhas e comentários dos leitores.
- **Perfil:** banner panorâmico com recorte, zoom e fade, foto, biografia, resumo das leituras, quatro HQs e quatro personagens favoritos com retratos pesquisáveis em português e inglês. Biblioteca e catálogo ficam abaixo dos personagens; edição retorna ao Perfil. Favoritos e banner são preservados por conta e sincronizados.
- **Biblioteca:** estante em grade ou lista, filtros de status, diário, tags, notas, resenhas e cadastro manual.
- **Busca e catálogo:** Metron para edições, paginação, créditos e capas, com Comic Vine como complemento e Superhero API para personagens. Cada HQ tem um formulário único de prévia e registro e uma página completa com sinopse, dados editoriais, equipe, personagens, edições relacionadas e Comunidade. Os favoritos do perfil podem usar até seis imagens reais da fonte, incluindo capas da coleção e retratos alternativos; a disponibilidade depende da obra e do personagem.
- **Leitura:** progresso por página, metas na tela inicial e diário cronológico com registros e avaliações.
- **Conta:** acesso com Google, sincronização pelo Firebase e modo convidado com armazenamento separado.
- **Widgets:** leitura atual, títulos recentes e metas, com aparência clara ou escura conforme o tema escolhido no app. A leitura atual acompanha a HQ selecionada no aplicativo.
- **Acompanhamento:** notificação com progresso e ações de leitura; integração com Live Updates/Now Bar em aparelhos compatíveis. A preferência fica em **Ajustes → Biblioteca**.
- **Compartilhamento:** cartões verticais de leitura concluída, momento e metas, com prévia, salvamento e compartilhamento pelo Android.
- **Personalização e acessibilidade:** cores de destaque e do Q do ícone, fundo neutro claro/escuro, tamanho do texto, contraste, transparência e movimento reduzido. O ícone é aplicado após sair do app, preservando a tela em uso.
- **Armazenamento:** medição dos dados locais e limpeza de cache em Ajustes, preservando biblioteca, contas e backups. Capas e respostas HTTP têm limites separados de 64 e 16 MiB.
- **Atualizações:** download dentro do aplicativo, verificação do pacote e abertura do instalador do Android.

## Instalação e compatibilidade

Requer **Android 7.0 ou superior**. Baixe o APK na página de releases. O Android pode pedir autorização para instalar aplicativos desta origem. Nas próximas versões, o próprio Quadrinheiro conduz o download e solicita a confirmação de instalação do sistema.

A Comunidade exige uma conta conectada. Ao salvar uma avaliação ou resenha, ela é publicada automaticamente na Comunidade. O restante da biblioteca e as preferências continuam privados. Informações editoriais e imagens são atribuídas ao Comic Vine. Quando uma fonte não informa páginas ou equipe, o app não inventa esses dados; o total de páginas pode ser informado no registro.

A apresentação de notificações na tela bloqueada e na Now Bar depende da versão do Android, da One UI, das permissões e das preferências de privacidade do aparelho. A leitura utiliza uma notificação de progresso; não simula reprodução de áudio nem uma sessão do Spotify.

Os efeitos gráficos usam caminhos compatíveis com a versão do sistema. A interface é inspirada nas diretrizes de design da Apple, implementada com APIs Android; não é uma implementação oficial da Apple.

## Desenvolvimento

| Área | Tecnologia |
|---|---|
| Interface | Kotlin 2.2.10, Jetpack Compose e Material 3 |
| Materiais e movimento | Backdrop/Liquid Glass, shaders e molas do Compose |
| Imagens | Coil, caches de memória e disco limitados |
| Dados | SafeStorage por conta, Firebase Auth e Cloud Firestore |
| Widgets | Jetpack Glance |
| Build | AGP 9.1.1, Gradle 9.3.1 e JDK 21 no CI |
| Verificação | JUnit, Robolectric, testes instrumentados e emuladores Firebase |

### Preparar o ambiente

1. Instale Android Studio, JDK 21, Android SDK **36.1** e Gradle **9.3.1**. O repositório atualmente utiliza a instalação do Gradle; não inclui o executável `gradlew`.
2. Clone o repositório e abra a pasta no Android Studio.
3. Configure seu projeto Firebase e coloque o arquivo Android em `app/google-services.json`, ignorado pelo Git. Para login Google, habilite o provedor, configure o cliente OAuth Web e cadastre os certificados de assinatura do build.
4. Execute na raiz:

```sh
gradle :app:assembleDebug
```

O APK fica em `app/build/outputs/apk/debug/`. A configuração Firebase e as chaves de assinatura da publicação são fornecidas por secrets no GitHub Actions.

### Verificar alterações

```sh
gradle :app:testDebugUnitTest
gradle :app:connectedDebugAndroidTest
```

Os testes que exercitam autenticação e regras exigem os emuladores Firebase. Consulte o [workflow de publicação](.github/workflows/release-apk.yml) para comandos, portas e variáveis usados no CI. Os testes de interface exigem um dispositivo ou emulador Android; o CI utiliza API 34. Testes em emulador não substituem a validação de funções específicas da Samsung em aparelho real.

### Serviço do catálogo

O diretório [`catalog-service`](catalog-service) contém o Worker Cloudflare. As chaves dos fornecedores ficam em secrets do servidor, nunca em `BuildConfig`, recursos Android ou no APK. O aplicativo usa somente o endereço público do serviço. O Firebase permanece no plano Spark; o catálogo usa Workers Free com Durable Object SQLite, cache compartilhado e limites globais por recurso.

```sh
cd catalog-service
npm ci
npm test
npx wrangler login
npx wrangler secret put COMIC_VINE_API_KEY
npx wrangler secret put SUPERHERO_API_KEY
npx wrangler secret put METRON_API_KEY
npx wrangler secret put APIFY_API_TOKEN
npm run deploy
```

O serviço aceita somente operações de catálogo definidas no código. Respostas duplicadas compartilham a mesma consulta, o cache possui tamanho limitado e falhas não expõem URLs com credenciais. O Comic Vine limita consultas por recurso; os resultados ficam em cache por até três dias. O Actor GCD indicado foi avaliado, mas o teste inicial não retornou registros; não é executado a cada pesquisa ou seleção de capa.

### Organização

```text
app/src/main/java/com/example/
├── data/          # Persistência, catálogos, tradução, sincronização e caches
├── model/         # Modelos e preferências
├── reading/       # Sessão de leitura e notificações
├── ui/components/ # Controles, materiais, folhas e compartilhamento
├── ui/screens/    # Telas e fluxos
├── ui/theme/      # Cores, tipografia e política de movimento
├── updates/       # Download e verificação de atualizações
├── viewmodel/     # Estado da aplicação
└── widget/        # Widgets e ações
```

Os caches de capas e HTTP têm limites separados. Arquivos temporários de atualização e cartões antigos são limpos automaticamente; coleção, registros e backups do usuário não fazem parte dessa limpeza.

## Documentação e referências

- [Revisão de interface e acompanhamento da leitura](docs/REVISAO-HIG-NOW-BAR.md)
- [Apple Human Interface Guidelines](https://developer.apple.com/design/human-interface-guidelines)
- [AndroidLiquidGlass / Backdrop](https://github.com/Kyant0/AndroidLiquidGlass)
- [LiquidGlassKMP](https://github.com/philipplackner/LiquidGlassKMP)
- [Avisos de terceiros](licenses/)

## Autoria e uso

Criado por [Felipe Loiola](https://github.com/felipemloiola). Todos os direitos reservados, conforme os termos anteriormente publicados neste repositório. A disponibilidade pública do código não concede licença de redistribuição. Dependências e materiais de terceiros mantêm seus próprios termos.
