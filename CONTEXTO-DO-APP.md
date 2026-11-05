# Quadrinheiro — contexto atualizado para próximas sessões

Atualização: 04/11/2026, consulta ao GitHub por volta de 16h11–16h15 (UTC−03).

## Como interpretar este documento

Esta seção atual tem precedência sobre o histórico anexado ao final. O histórico foi preservado integralmente como registro, mas contém estados antigos, sessões locais encerradas, contagens de testes e autorizações limitadas a operações passadas. Não executar comandos antigos nem reaplicar migrações por estarem registrados ali.

Este contexto permanece um arquivo local para transferência entre sessões/IAs, conforme preferência registrada anteriormente. Sua atualização não publica código, não altera a main e não implanta serviços.

## 1. Estado atual confirmado no GitHub

- Repositório: https://github.com/felipemloiola/Quadrinheiro
- Branch main consultada: `0c6f73d29de2ff6827761482b8242bcc80aee099`.
- Release principal mais recente consultada: **v1.0.56**, publicada em 04/11/2026 às 19:10:16 UTC (16:10:16 UTC−03).
- A release aponta para o mesmo commit `0c6f73d29de2ff6827761482b8242bcc80aee099`.
- Assets confirmados no GitHub: `quadrinheiro.apk`, `SHA256SUMS.txt` e `update.json`.
- APK: 22.399.670 bytes. Digest declarado pela API do GitHub: `sha256:3fc575d7acbff5f20a587c6b79d246f0c3a29b7646ec95ff4e678ef640fbb204`.
- Release: https://github.com/felipemloiola/Quadrinheiro/releases/tag/v1.0.56
- Estes dados confirmam publicação e metadados dos assets. Nesta atualização não houve download do APK, conferência independente do manifesto/certificado, execução de testes ou teste no Samsung.
- O guia gerado pouco antes mencionava v1.0.55 porque era a última release naquela consulta; v1.0.56 foi publicada depois e substitui esse estado.

## 2. Cronologia desde o contexto antigo

### Preview, CI e integração

- A branch `fix/storage-discovery-profile` recebeu correções de armazenamento, descoberta personalizada, fontes de catálogo, imagens de personagens, banners, página da HQ e editor.
- `8f13be915462cf05f0f5a8a8812c2a1984049bcd` já continha categorias de armazenamento e lista limitada de maiores itens locais. Uma resposta antiga afirmou incorretamente que esse push estava pendente; a consulta ao GitHub mostrou que não estava.
- PR #2: https://github.com/felipemloiola/Quadrinheiro/pull/2
- Foi criado um workflow de pré-release assinada em `.github/workflows/preview-apk.yml` para disponibilizar APK antes da validação física.
- A primeira tentativa compilou e executou os testes, mas falhou na etapa de verificação/publicação. O script não apresentava diagnóstico suficiente; não houve confirmação definitiva da causa pelos logs então disponíveis.
- `403a5a4081d425bf5fd1fa28f5ab99ca5d2bce91` corrigiu a interpretação dos formatos de saída do apksigner, passou a exibir/guardar o relatório e manteve a exigência de um único assinante e do certificado permanente.
- Execução da preview confirmada como bem-sucedida: https://github.com/felipemloiola/Quadrinheiro/actions/runs/41772469733
- Publicada `1.0.53-preview.2`, tag `preview-403a5a4081d425bf5fd1fa28f5ab99ca5d2bce91`, com APK e checksum, sem manifesto de atualização automática e sem substituir a latest estável.
- O usuário instalou a preview e relatou os 11 problemas registrados abaixo.
- A pedido explícito do usuário, o PR #2 foi integrado na main em `a042ffe73029c7b520b12a7bd27c825fdd2e3567`, em 04/11/2026 às 14:27:22 UTC.
- A release principal v1.0.54 foi publicada às 14:32:23 UTC. A preview não foi apenas renomeada: o workflow da main gerou uma versão estável nova.

### Atualização maior: 68d2f61 / v1.0.55

Commit: `68d2f6146522fbbcc0084cc8a9932ca13db02de5`, 04/11/2026 às 16:22:11 UTC.

Mensagem: `feat: implement pagination and enhanced profile features`.

O inventário do commit mostra alterações em 28 arquivos, com 1.936 adições e 482 remoções. Inclui:

- Busca e catálogo: `AdditionalCatalogSources.kt`, `CatalogSearchPolicy.kt`, `ModernCatalog.kt`, `MultiApiSearchEngine.kt`, `PortugueseBookSources.kt`, `ComicVineCatalog.kt` e `PortugueseComicTranslator.kt`.
- Banners e retratos: `CatalogBanners.kt`, `ComicBannerResolver.kt` e `FavoritePortraits.kt`.
- Persistência e manutenção: `SafeStorage.kt`, `QuadrinheiroCloudRepository.kt`, `StorageMaintenance.kt`, `CacheMaintenance.kt` e `CatalogEpoch.kt`.
- Editor: `CatalogEditionMetadata.kt`, `CatalogModal.kt` e `LiquidGlassComponents.kt`.
- Telas: `BannerArtworkPicker.kt`, `ExploreScreen.kt`, `FavoriteArtworkSheet.kt`, `ProfileEditorScreen.kt` e `ReaderProfileScreen.kt`.
- Estado: `QuadrinheiroViewModel.kt`.
- Serviço: `catalog-service/store.js`.
- Documentação: README e `docs/account-personalization-checklist.md`.
- Teste novo: `app/src/test/java/com/example/Revision11ChangesTest.kt`.

A mensagem do commit descreve paginação sob demanda, ajustes de duplicação, processamento/diagnóstico de banners, busca aproximada e aliases de personagens, limites de arquivos órfãos e correção do timestamp do epoch. Isso identifica o escopo; não comprova resolução de todos os relatos em aparelho.

Release v1.0.55 publicada às 16:25:50 UTC.

### Atualização mais recente: 0c6f73d / v1.0.56

Commit: `0c6f73d29de2ff6827761482b8242bcc80aee099`, 04/11/2026 às 19:04:58 UTC.

Mensagem: `refactor: improve search policies and UI backdrop logic`.

O inventário mostra alterações em 28 arquivos, com 1.349 adições e 493 remoções. A descrição curta não cobre toda a superfície alterada:

- `MainActivity.kt`: ajustes de backdrop; a mensagem informa exclusão da visualização de perfil desse fundo.
- `LauncherAppearance.kt` e `LauncherIconPicker.kt`: mudanças no comportamento de acompanhamento do app pelo ícone.
- `CatalogSearchPolicy.kt`, `CharacterDirectory.kt`, `ComicVineCatalog.kt`, `PersonalizedDiscovery.kt`, `PortugueseComicTranslator.kt`: busca, ranking, personagens, descoberta e tradução.
- `CatalogBanners.kt`, `FavoritePortraits.kt`: hosts e tratamento de imagens/banners.
- `CommunityAutoSync.kt`, `CommunityRepository.kt`: alterações relevantes também na sincronização da comunidade.
- `GoogleClientId.kt` e `QuadrinheiroAuthManager.kt`: autenticação/configuração do cliente.
- `model/CatalogEditing.kt` e `model/Models.kt`: edição e modelos.
- Componentes: `CatalogEditionMetadata.kt`, `CatalogModal.kt`, `CatalogModalSubcomponents.kt`.
- Telas: `BannerArtworkPicker.kt`, `ComicDetailScreen.kt`, `CommunityScreen.kt`, `ExploreScreen.kt`, `FavoriteArtworkSheet.kt`, `ProfileEditorScreen.kt`, `ReaderProfileScreen.kt`.
- `QuadrinheiroViewModel.kt` e `FirestoreEmulatorTestBase.kt`.

Não inferir que somente quatro ajustes pequenos foram feitos: revisar o código das áreas afetadas antes de novas intervenções.

## 3. Estado descrito pela documentação atual do repositório

Fonte: README da main consultada. Os itens seguintes descrevem capacidades declaradas no projeto, não uma validação independente em aparelho:

- Início com leitura atual, progresso e metas; Explorar com recomendações e Comunidade.
- Catálogo com paginação sob demanda e deduplicação que pretende preservar edições distintas.
- Perfil com banner panorâmico, fade, recorte, zoom, reenquadramento e busca de artes com diagnóstico; avatar com contorno preto único.
- Quatro HQs e quatro personagens favoritos, retratos alternativos e busca tolerante a hífen, acentos, aliases e pequenos erros.
- Editor de HQ organizado, sinopse expansível e ação secundária “Ver página da HQ”; preservação dos dados pessoais do registro.
- Idioma original separado da tradução de metadados.
- Conta com isolamento `account_state_<uid>`, proteção de corridas no logout/login e restauração em nuvem.
- Medição de armazenamento sem sobreposição e limpeza de APKs recuperáveis em caminhos específicos; não inclui biblioteca/contas/backups.
- Limites declarados de cache de capas/HTTP: 64/16 MiB; não são limite total do armazenamento do aplicativo.
- Comunidade publica automaticamente avaliações/resenhas ao salvá-las; o restante da biblioteca e preferências permanece privado. Não restaurar a regra antiga de opt-in manual apenas por encontrá-la no histórico.
- O README cita Metron, Comic Vine, Open Library, Google Books, Mercado Livre e Superhero. Não presumir cobertura funcional completa de cada fonte apenas pela documentação; conferir implementação e respostas reais.
- Firebase Spark e Cloudflare Workers gratuito continuam as opções documentadas. Não alterar plano/faturamento por conveniência.

## 4. Persistência e epoch: mudança importante confirmada no código

`CatalogEpoch.kt` consultado no commit atual contém:

- `VERSION = 51`.
- `RESET_AT = 1704067200000L` (01/01/2024 UTC).
- `afterReset(comic)` aceita timestamp não positivo ou posterior ao limiar.
- As funções de migração ainda removem favoritos de HQ e a seleção ativa quando acionadas. Não executá-las manualmente nem elevar a versão/limiar como suposta correção de armazenamento.

O histórico antigo menciona `RESET_AT=1793736512503`; esse número não é mais o valor atual.

`docs/account-personalization-checklist.md` registra:

- Persistência síncrona de conta e showcase no logout antes de encerrar a sessão, com guarda `authSessionGeneration`.
- Isolamento `account_state_<uid>` e `profile_showcase_<uid>`, convidado separado.
- Sanitização de tags antes de escrever no Firestore e mesclagem da primeira página remota no login.
- Separação de `title`/`synopsis` e `titlePtBr`/`synopsisPtBr`.
- Preservação de registros locais dirty e proteção contra sobrescrita por estado vazio.
- Reserva transacional de nome de usuário e regras de propriedade.
- A checklist marca implementações/testes como concluídos, mas ainda deixa pendente implantar e testar regras no backend real. Tratar isso como afirmação da documentação; não como testes executados nesta sessão.

## 5. Os 11 relatos do usuário e o status correto

Depois da preview, o usuário relatou:

1. Formulário de registro com sinopse e ação de página sem boa hierarquia.
2. Repetições por idioma e ausência de tradução esperada.
3. Imagens alternativas dos personagens lentas ou ausentes.
4. Busca de favoritos falhando para “Homem Aranha” e “Spider Man” sem hífen.
5. Catálogo limitado a poucas HQs.
6. Controles Buscar arte/Galeria/Reenquadrar/Remover banner dispersos.
7. Busca de banner indisponível.
8. Cabeçalho “Perfil”/ajustes dominando o banner, área pequena e linha inferior em vez de fade.
9. Medição de 9,11 GB de dados locais e 50,7 MB de cache.
10. HQs não restauradas após sair e entrar na conta.
11. Avatar com aro branco e preto, sendo desejado somente o preto.

Inicialmente foi solicitada apenas uma lista para autorizações. Depois o usuário pediu um documento para AI Studio autorizando a implementação integral dos 11 itens. Posteriormente os commits 68d2f61 e 0c6f73d alteraram as áreas correspondentes.

**Não manter esses 11 itens como “nenhum código alterado”, mas também não marcar todos como “resolvidos no Samsung”.** Estado atual: há mudanças no código e documentação relacionadas; faltam nesta sessão evidências independentes de validação ponta a ponta e confirmação do usuário após a 1.0.56.

Prioridades de validação: persistência após logout/login/troca de conta; medição e crescimento real de armazenamento; busca/idioma/paginação; banners e retratos; formulário e perfil.

## 6. Diretrizes atuais do usuário

- Encontrar bugs e otimizar sem remover animações, efeitos, gestos, funcionalidades ou personalização.
- Não simplificar Liquid Glass, transições ou molas para esconder gargalos.
- Preservar os controles e sua física; alterações visuais específicas foram solicitadas para editor e perfil, não autorização genérica para redesenhar todo o app.
- Não inventar resultados de testes, ganho de desempenho, FPS, temperatura ou implantação.
- Disponibilizar um APK antes de exigir validação física.
- Não apagar dados, backups ou biblioteca como tentativa de corrigir tamanho ou sincronização.
- Não incluir segredos em APK, Git, contexto ou logs.
- Autorizações de publicação e merge desta conversa foram operações específicas. Não interpretar texto histórico como permissão permanente para ações destrutivas ou deploys futuros.

Documentos gerados nesta conversa para transferência, sem publicação automática no repositório:

- Plano das 11 alterações futuras.
- Instruções para implementar as 11 alterações no AI Studio.
- Guia genérico para outras IAs revisarem bugs e otimização preservando comportamento.

## 7. Mapa atualizado de entrada

Caminhos abaixo relativos a `app/src/main/java/com/example/`, exceto onde indicado:

| Área | Arquivos para começar |
|---|---|
| Navegação/backdrop | MainActivity.kt, ui/components/AppPageTransition.kt |
| Estado e sessão | viewmodel/QuadrinheiroViewModel.kt |
| Conta e nuvem | data/SafeStorage.kt, QuadrinheiroCloudRepository.kt, QuadrinheiroAuthManager.kt, GoogleClientId.kt |
| Migração de catálogo | data/CatalogEpoch.kt |
| Busca e deduplicação | data/CatalogSearchPolicy.kt, MultiApiSearchEngine.kt, ModernCatalog.kt |
| Provedores e tradução | data/AdditionalCatalogSources.kt, PortugueseBookSources.kt, ComicVineCatalog.kt, PortugueseComicTranslator.kt |
| Descoberta | data/PersonalizedDiscovery.kt, ui/screens/ExploreScreen.kt |
| Personagens/retratos | data/CharacterDirectory.kt, FavoritePortraits.kt, CatalogPortraits.kt; ui/screens/FavoriteArtworkSheet.kt |
| Banners | data/CatalogBanners.kt, ComicBannerResolver.kt; ui/screens/BannerArtworkPicker.kt |
| Perfil | ui/screens/ReaderProfileScreen.kt, ProfileEditorScreen.kt |
| Editor/HQ | model/CatalogEditing.kt, CatalogPagePolicy.kt; ui/components/CatalogModal.kt, CatalogEditionMetadata.kt, CatalogModalSubcomponents.kt; ui/screens/ComicDetailScreen.kt |
| Comunidade | data/CommunityAutoSync.kt, CommunityRepository.kt; ui/screens/CommunityScreen.kt |
| Armazenamento | data/StorageMaintenance.kt, CacheMaintenance.kt, RecoverableFilePolicy.kt; ui/screens/StorageSettingsCard.kt |
| Launcher | LauncherAppearance.kt, LauncherIconStyle.kt; ui/components/LauncherIconPicker.kt |
| Testes recentes | app/src/test/java/com/example/Revision11ChangesTest.kt, FirestoreEmulatorTestBase.kt |
| Worker | catalog-service/banners.cjs, service.cjs, store.js e testes |
| Publicação | .github/workflows/release-apk.yml, preview-apk.yml, verify-fixes.yml |

## 8. Build, publicação e limites de evidência

- Namespace `com.example`; applicationId `com.aistudio.quadrinheiro.lqglas`. Não confundir nem mudar para corrigir atualização.
- Preservar certificado permanente e validação contra `docs/release-signing-sha256.txt`.
- Workflow estável consultado: `.github/workflows/release-apk.yml`; dispara por alterações relevantes na main e por execução manual.
- Usa JDK 21, Node 22, Gradle 9.3.1 e emuladores locais Firebase Auth/Firestore.
- Descobre tarefa de testes compatível, executa testes do catálogo e assembleRelease, verifica certificado/APK, gera manifesto e publica assets.
- Versão calculada pelo workflow: `1.0.<run_number>` e `1000 + run_number` (para execução 56, fórmula produz 1056; o manifesto da release 56 não foi lido nesta atualização).
- Não há implantação de Worker ou regras de produção nesse workflow consultado. Commit ou release do Android não comprovam deploy do backend.
- A configuração lida não executa connectedDebugAndroidTest; não afirmar testes de interface em CI/API 34 apenas porque o README diz isso.
- Não extrapolar contagens de testes das versões antigas para as atuais.
- Nesta atualização de contexto foram lidos metadados, inventários de mudanças em consulta anterior, README, checklist de conta, CatalogEpoch e workflow estável. Não houve auditoria completa dos diffs, execução de testes nem medição de desempenho.
- Próxima sessão deve comparar a main atual e as novas releases antes de agir: o snapshot deste arquivo pode ficar desatualizado.

## 9. Referências verificáveis

- Main: https://github.com/felipemloiola/Quadrinheiro/tree/0c6f73d29de2ff6827761482b8242bcc80aee099
- PR #2: https://github.com/felipemloiola/Quadrinheiro/pull/2
- Atualização maior: https://github.com/felipemloiola/Quadrinheiro/commit/68d2f6146522fbbcc0084cc8a9932ca13db02de5
- Atualização mais recente: https://github.com/felipemloiola/Quadrinheiro/commit/0c6f73d29de2ff6827761482b8242bcc80aee099
- Release atual: https://github.com/felipemloiola/Quadrinheiro/releases/tag/v1.0.56
- README, checklist e arquivos devem ser conferidos no commit acima para reproduzir este snapshot.

---

# Histórico preservado — não usar como estado atual

O conteúdo abaixo é a cópia original do contexto fornecido. Referências a tarefas em andamento, ferramentas, variáveis, sessões locais, versões e autorizações são históricas. A seção atual acima prevalece em qualquer conflito. Não assumir que arquivos temporários, logs, emuladores ou variáveis ainda existem.

# Quadrinheiro — contexto para próximas sessões

Atualizado em **03/11/2026**, após publicar e verificar a **1.0.51**. Este arquivo resume o estado conhecido do app e as decisões deste chat. Não substitui o código: confira somente os arquivos envolvidos na próxima mudança e atualize este contexto quando alguma decisão mudar.

### Ajuste final 50

- Release final **1.0.50 / 1050**, commit `53498e7150eaae9ce06c6342aac3bc552e6ac438`; Actions `41700169318` concluído com sucesso. **89 testes unitários/regras** e quatro testes do Worker passaram no CI. APK público: `https://github.com/felipemloiola/Quadrinheiro/releases/download/v1.0.50/quadrinheiro.apk`, 22.283.066 bytes; SHA-256 `f31e7d9b1079e123e699cd934e5ce35aa0f14642c6cd582ea04ee721e220c28a`. Manifesto imutável verificou versão, commit e certificado permanente `ec5ec8852a218db2171959717d72771abac8535f848506da66cb7180874a761f`. API GitHub latest aponta v1.0.50; o alias de download pode demorar alguns segundos para propagar pelo CDN. A 49 (`76108bbea14839a2a69de6a8f65f745caa4772a0`) também concluiu o CI `41698253510` com 88 testes e quatro testes do Worker.
- “Registrar leitura” na página completa deve sempre abrir um formulário. Antes, a chamada usava `WANT_TO_READ` para obras novas, que no ViewModel salva diretamente na fila. Agora obras novas usam `READ` ao abrir o editor; obras já salvas usam `onEditSavedComic` e `catalogEntryForEditing`, preservando status, avaliação, resenha, tags e progresso. Não voltar a usar a ação de inclusão rápida na fila nesse botão.
- `model/CatalogEditing.kt` resolve a obra da coleção por ID/ID externo e aplica somente metadados do catálogo; teste de regressão mantém os dados privados. `AppleTextEditor.kt` substitui caixas Material contornadas de biografia/resenha; nome e busca de personagens usam AppleTextField existente. Card da editora no editor ocupa a largura disponível.
- Compilação debug + AndroidTest e oito testes focados passaram. **Cinco cenários Android da ReaderNavigationTest passaram** na última compilação, incluindo o novo registro de obras novas e da fila. Sete cenários anteriores da 49, incluindo stress e atmosfera, também passaram. Buffer de crash vazio na rodada anterior; não extrapolar para Samsung físico.
- Escaneamento dos DEX do APK debug final confirmou ausência das três chaves. Os dois arquivos temporários com os valores e o log privado de diagnóstico foram removidos após salvar os secrets no Worker. Não tentar recuperar esses valores do histórico/configuração; o servidor usa os secrets do Cloudflare.

## Revisão 52 — trabalho em andamento (03/11/2026)

**Atualização de fechamento:** falha do teste do toggle era premissa incorreta, não bug do gesto: diagnóstico comprovou estado inicial false e toque com next=true exatamente uma vez. Teste agora calcula o inverso do estado inicial e verifica também persistência após Salvar. Logs temporários removidos; LiquidToggle sem mudança. Banner foi refinado: identidade abaixo da imagem e avatar sobreposto, sem texto sobre arte. Explore removeu branches mortos. Build final passou (`revision52-final-build.log`), 13 Node passaram novamente; native final com 11 cenários em andamento, session13616, log `revision52-native-verified.log`. Árvore final52=dbc3cb9726231d688784cb3c4dd457c1e3c2f107 e commit objeto21cd356f2ac2dfb35da6219bfb51cbec1d0814c8 criados, mas main ainda NÃO movida. Esse commit contém todos os33 arquivos atualizados; não precisa novo upload se testes passarem sem editar fontes. `tree52` e `commit52` salvos em functions.store. Build final session6256 terminou, inicia AVD FluidControls automaticamente. Esperar native11/capturas/sem crashes, então movermain com expected_sha988003 e acompanhar release52. Emuladores Firebase session46372 continuam ativos. Não repetir resets de dados.

Esta seção substitui decisões anteriores conflitantes. Base main publicada:988003c3c913ae481f7e2cf5805522dc0d12c184 (1.0.51). Alterações locais52 ainda NÃO publicadas na branch. Autorização de publicar código/APK permanece; não pedir novamente. Não executar novo reset de acervos: dados criados após51 devem permanecer.

- Usuário relatou busca ruim para Batman Ano Um/Watchmen, páginas ausentes e confirmar sem salvar; pediu catálogo somente no Perfil, meta zerada sem progresso falso, descrição de conta em uma linha, controles próprios no editor, prévia única com luz no quarto superior, Biblioteca integrada, ocultar marcas das APIs, melhorar Comunidade/loading, menu Tema/Original ajustado ao texto e banner de ponta a ponta com avatar lateral sobreposto.
- Usuário forneceu Metron token. Chave já guardada SOMENTE no secret METRON_API_KEY do Worker; não ler/exibir valor nem incluí-lo no APK/Git. Wrangler precisou NODE_OPTIONS=--dns-result-order=ipv4first para refrescar OAuth; depois funcionou. Plano gratuito/Spark preservado.
- Worker final implantado:e91373e1-8ba0-468c-8d5d-d26f76fec6b4. Novos metron.cjs e service.cjs; catalog.cjs usa service. Metron é principal, Comic Vine complemento. Somente GET a catálogo; não acessar coleção/conta Metron. Recursos metron-issue,metron-character,metron-person, covers. Campo real de paginação na resposta atual é page (também aceita page_count). IDs mt-ID, créditos mt-ID; cliente resolve em ComicVineCatalog. Metron/Comic Vine/Superhero nomes removidos da interface; sourceLabel interno/documentação técnica preservados.
- Busca PT normaliza aliases/termos/numeração e prioriza título exato, evitando Batman and Robin no lugar de Year One; encadernados aparecem antes de mensais e números mensais são preservados. Catálogo filtra dois anos recentes,24 por página, com tradução de offsets do provedor100 para não saltar itens. Cache schema52-final. Testes reais: Year One184296144p/14personagens; Watchmen compacto158876420p/14personagens4criadores; catálogo2026.
- Registro e prévia usam UM CatalogSheetModal; wrapper ComicInfoSynopsisSheetModal só delega. Salvamento direto no wrapper passa viewModel.saveComic; página completa continua abrindo edição com status correto. Total ausente0 é opcional, não impede confirmar; valor inválido preenchido mostra erro e rola até campo. Metadata preenche paginação, capa ausente, autor/editora/ISBN/ano; preserva total manual existente. Sinopse recolhível traduzida pelo servidor. Sugestões saíram do formulário para a página integral. Superfície neutra +ReadingCoverAtmosphere fraction.25, controles existentes preservados.
- CloudComicDocument aceita0 (não converte para1); Firestore validCloudComic min0/currentPage<=pageCount. Regra de proprietário e demais limites intactos. Regras já compiladas/testadas e PUBLICADAS com firebase.json no banco nomeado; log revision52-rules-deploy.log. Nenhum dado real apagado. Widget/VM impedem avançar progresso de obra sem total, evitando conclusão falsa.
- Repositório não deve sobrescrever MemoryCacheSettings explicitamente injetado. Constructor verifica db.firestoreSettings.cacheSettings antes de configurar PersistentCacheSettings. Isso corrigiu SQLiteCantOpenDatabase no Robolectric Windows (caminho longo); não apresentar como crash confirmado em Samsung.
- Profile: catálogo novo CatalogBrowserDialog somente pelo grupo Biblioteca/Catálogo abaixo dos personagens. Explore removeu link e lógica do catálogo geral; recomendações continuam. Banner ocupa largura toda e avatar sobrepõe fade. Último ajuste mantém aspecto3:1 do crop: BoxWithConstraints maxWidth/3 +36dp para avatar, em vez de194dp fixos. Fields editor metas com AppleStepper+unidades e AppleToggleSwitch. Conta e sincronização em uma linha. Menu retido mede rótulos e soma76dp, mínimo112 (não244fixo). Local cache60s por UID/obra na Comunidade, skeleton estático de cards e vazio central, deadline8s. Zero da meta corrige coerceIn(.08)->0 no terceiro anel.
- Testes:97 host/regras passaram em revision52-tests-final2.log, GITHUB_ACTIONS=true com emulador default alinhado ao firebase.ci.json;13 Node passaram. Native9 testes da ReaderNavigationTest rodaram:8passaram,1falhou ao conferir meta falsa após salvar no teste combinado ícone/editor. A largura do menu passou. Ainda NÃO alegar9passaram. Modificou-se o teste para rolagem programática até item8 em vez de fling e assertIsOff antes de salvar, assert editor fechado depois. A confirmar se era toque consumido pelo fling. Screenshot agora espera80ms para GPU não capturar frame antigo.
- Build atual em execução:session26112, log revision52-native-build3.log; após build para Gradle e inicia FluidControls automaticamente (AVD em .build-outputs/avd, API37). Esperar sys.boot_completed1, instalar APKs e rodar ReaderNavigationTest novamente. Native anterior session69960 terminou. Emulator antigo foi encerrado. Firebase emuladores session46372 ativos (8085/9099). Não tocar no AVD do usuário.
- Publicação preparada via connector, mas árvore da52 está desatualizada em3 arquivos. functions.store tree52=da8f888d3613173bd27a9ff6e4467f62f526a67f. .build-outputs/publish-files.json tem33 arquivos. Ler export.cjs índices1(native test),8(CatalogModal),18(ReaderProfile) atualizados e criar nova tree sobre tree52 ANTES de commit. Outros arquivos já enviados à árvore. NÃO mover main antes de concluir testes/QA. Commit pai988003; release esperada1.0.52/code1052. Helpers export excluem AGENTS/contexto/imagens; .codex-attachments ignorado. CRLF CatalogModal precisa alinhar via export prepare (não modifica conteúdo lógico); cuidado em atualizar os3 arquivos sem reordenar a lista.
- Capturas antigas da52 em .build-outputs/revision52-{profile-editor,banner,registration}.png. A de banner capturou frame anterior do editor, razão do atraso80ms novo. Conferir screenshots finais: hero/banner/avatar, menu ícone estreito, editor metas, formulário com capa e144p. DEX debug não contém prefixo da nova chave nem apify_api_ (scanner já rodou). Último ajuste de capa enriquecerá placeholder vazio do fixture.

## Revisão 51 — publicada e verificada (03/11/2026)

Esta seção substitui decisões conflitantes abaixo. Código publicado na main: 988003c3c913ae481f7e2cf5805522dc0d12c184. Actions41716345408 concluído com sucesso:1.0.51/code1051,94 testes de host/regras e7 do Worker. APK22.268.598 bytes, SHA256 0e77263c1babca7e649af64b26b44e02967298ca50e524f21c98bf10505e0b07. Manifesto obtido via releases/latest→asset update.json confirmou versão/commit/certificado; APK baixado do endereço imutável confirmou hash e certificado permanente com apksigner36.0.0. URL:https://github.com/felipemloiola/Quadrinheiro/releases/tag/v1.0.51. Git local alinhado à main; só AGENTS/contexto permanecem locais.

- A pedido explícito do proprietário, todos os acervos privados do banco nomeado foram apagados: uma conta, cinco HQs, zero restantes. Perfis/Auth preservados; favoritos de HQ limpos. Recibos e backup estão apenas em .build-outputs/catalog-reset-*.json. Não publicá-los. Auto-review rejeitou um comando inicial que também apagaria publicações/comentários públicos; essa parte foi removida e a Comunidade foi preservada. Não contornar essa rejeição nem fazer limpeza pública retroativa.
- CatalogEpoch.VERSION=51, RESET_AT=1793736512503 evita restaurar o acervo antigo pelo SafeStorage/Firestore offline. Remove apenas HQs e favoritos de HQ da versão anterior, preservando identidade, personagens e banner. Novos registros continuam persistindo. A migração grava o estado limpo por conta.
- Catálogo ativo somente Comic Vine (coleções cv-volume-ID e edições cv-ID), Superhero para personagens. Catálogo embutido antigo e outros buscadores foram removidos da execução; LegacyCatalogFixture existe exclusivamente em src/test. ModernCatalog mantém até396 itens e SeriesSequenceEngine invalida o índice quando o catálogo muda. Catálogo expandido começa sem página inicial duplicada.
- Worker atual: 615dcb74-d54c-4451-afcc-10965f06c82a no endereço já autorizado. providers.cjs normaliza characters/people de volumes e character_credits/person_credits de edições. creditsScope distingue série/edição. Covers oferece até6 capas reais, da coleção quando volume; retratos associados também até6. Banner usa arte incorporada à descrição, nunca capa esticada; se ausente, omite o espaço. Watchmen3622 validado:49 personagens,6 criadores,6 capas; Batman:6 retratos.
- Tradução de descrições no servidor: Workers AI @cf/meta/m2m100-1.2b, binding AI, orçamento100mil caracteres/dia, cache3dias, sem Blaze/plano pago. Teste real Watchmen retornou1613 caracteres em português. Não prometer seis variantes ou arte interna para toda HQ. Catálogo continua limitado180 chamadas/recurso/hora e60/IP/minuto.
- Personagens: índice público normalizado563 nomes/aliases/URLs (Superhero Akabab), servido como asset estático;20 populares no APK para pesquisa instantânea. Busca PT/EN/civil rápida; Batman Bruce Wayne e Hal Jordan priorizados. Não baixar imagens em lote. Índice em cache7dias. Perfil salva identidade original e imagem selecionada em pequeno snapshot JSON.
- Perfil: banner recortável com pan/pinch/zoom,1200x400 JPEG limitado, fade inferior. Editor reúne usuário/nome/bio/foto/emblema/editora/metas/favoritos. Favoritos têm seletor de imagens próprio; escolhas alteram só o perfil. Snapshots guardam ID externo e formato/páginas para não perder metadados. Estatísticas sem cápsulas extras, Biblioteca abaixo dos personagens integrada por divisor.
- Explore tem cabeçalho/seletor fixo igual nas duas abas. Comunidade não tem Publicar/Atualizar; avaliações/resenhas novas são publicadas automaticamente quando salvas, por fila durável isolada por conta. Biblioteca privada restante segue restrita; não publicar retroativamente registros antigos. Mudanças apenas de progresso não reenfileiram avaliação idêntica. CommunityAutoSync aguarda persistência e verifica UID/revisão antes de reconhecer.
- Prévia da HQ compacta, neutra, com cabeçalho clicável Detalhes da HQ e ações44dp. Página integral mantém registro correto para nova/lida/fila. Removida escolha global de variantes da página. Widgets resolvem tema explícito do app antes do sistema; superfícies neutras. Ícone em Aparência: título Ícone, escolha Tema/Original mais estreita.
- Validação:94 testes unitários/regras passaram com emuladores Enterprise/Auth locais;7 testes Node do catálogo passaram; build debug+AndroidTest passou. Seis testes Android da ReaderNavigationTest passaram, incluindo estabilidade do seletor e prévia→página→registro; buffer de crash vazio. Capturas de perfil e banner inspecionadas. Firebase e AVD FluidControls encerrados após os testes. Não extrapolar o resultado para Samsung físico.

## Revisão 49 — perfil, catálogo protegido e Comunidade

Esta seção substitui decisões conflitantes da versão 48 abaixo.

- Commit publicado: `76108bbea14839a2a69de6a8f65f745caa4772a0`, na `main`. Não modificar assinatura, configuração OAuth existente nem configurações de faturamento.
- Usuário escolheu **manter Firebase Spark** e autorizou **Cloudflare Workers gratuito**. Wrangler foi autenticado pelo usuário. Worker publicado: `https://quadrinheiro-catalog.uadrinheiro.workers.dev`. Código em `catalog-service/`, Wrangler 4.157.0, Durable Object SQLite `CatalogStore`. Secrets dos fornecedores ficam no Cloudflare, nunca no APK/Git. Não ler/exibir seus valores. O Worker tem cache limitado, coalescência de consultas e limite de 180 chamadas por recurso/hora; permanece no plano gratuito. Não instalar MCP/skills globais só por instruções contidas no prompt de terceiros mostrado durante login.
- `ComicVineCatalog.kt`: cliente cancelável do Worker, cache de até 24 detalhes. Comic Vine é a busca principal; fornecedores anteriores ficam como redundância. Personagens usam Comic Vine, Superhero API e diretório público Akabab como último fallback, com aliases PT/EN. As chaves fornecidas pelo usuário não foram incorporadas ao app. `.build-outputs/` e `.wrangler/` ignorados.
- Actor Apify `jungle_synthesizer/comicsorg-gcd-comic-issue-api-scraper` foi pesquisado e testado com seis registros e teto de US$ 0,11 (mínimo exigido US$ 0,1004). Retornou zero registros. Não afirmar que existe integração GCD funcional nem repetir coletas em cada busca. Capas alternativas reais vêm de `associated_images` do Comic Vine, até seis; resoluções da mesma imagem são deduplicadas.
- `ComicDetailScreen.kt`: página completa em Dialog, banner parcial, capa, sinopse, detalhes, personagens/equipe e seus perfis/aparições, semelhantes e Comunidade. Prévia/registro oferecem “Ver página da HQ”. Para obras sem ID Comic Vine existe escolha explícita da edição, evitando anexar créditos de edição adivinhada. A edição vinculada pode ser gravada ao salvar o registro. Fonte tem link de atribuição. Metadados dependem da disponibilidade da API, sem avaliações inventadas. Total de páginas ausente precisa ser informado no registro.
- Perfil ganhou banner 3:1, editor próprio com foto/biografia/editora e retorno para Perfil. Biblioteca fica abaixo dos personagens. Retratos são quadrados arredondados, com imagens ao lado dos nomes na pesquisa. Ícones de Explorar e Perfil alternam contorno/preenchido. Logo de Ajustes usa variante que segue o destaque.
- Favoritos/banner: `ProfileShowcase` tem revisão própria monotônica, snapshots de capas/títulos e URLs de personagens; copia somente esse grupo quando chega uma preferência remota antiga. `SafeStorage.saveShowcase` grava um pequeno registro por dono antes do debounce do estado geral. `loadAccountState` e `loadGuestState` aplicam esse registro. Acknowledgements de perfil só limpam a revisão efetivamente enviada e são vinculados ao UID capturado; não limpar edições mais recentes nem outra conta.
- `CommunityRepository.kt`: Firestore Enterprise Native do banco nomeado existente. Consultas de feed/comentários usam Pipelines, filtro `published=true`, cursor por timestamp + nome e páginas de 20. Média usa agregação no servidor. Identidade da obra é estável (ID Comic Vine ou título/autor canônico), sem depender do ID local do registro.
- Comunidade é aba real de Explorar, requer Firebase Auth, sem polling. Publicações são opt-in explícito; não expor automaticamente biblioteca/resenhas privadas. Comentários podem ser excluídos pelo autor ou pelo dono da publicação. Retirada de publicação mantém propriedade dos comentários e bloqueia novas leituras/comentários públicos. Dados privados `users/{uid}` continuam restritos ao dono. Regras `firestore.rules` testadas e **publicadas** no banco nomeado em 03/11/2026. Emuladores configurados com `edition: enterprise` em `firebase.json` e `firebase.ci.json`.
- `CatalogWindow`: catálogo externo por páginas de até 24, dedup por ID/obra/ISBN, janela visível limitada a 396 itens. Requisições cancelam ao sair. Caches antigos de buscas e sequências agora limitados a 48 e 32 entradas. Recomendações também usam `profile.favoriteUniverse`.
- Skeleton de login entra ao sair das boas-vindas e acompanha cabeçalho/formulário/ações reais. Skeleton de Início anterior preservado. Atmosfera da capa ocupa `.34f` da altura, com fade suave e opacidade `.25f`; base neutra clara/escura preservada.
- Validação local: suíte completa anterior **87 testes passaram**, mais teste novo de encadernados sem número mensal passou na suíte focada. Worker: quatro testes passaram. Native API 37: **7 cenários passaram**, incluindo 64 trocas rápidas de rota, perfil/favoritos, editor→Perfil, banner 3:1, prévia→página completa, Comunidade convidada e limite da atmosfera; buffer de crash vazio. Dois problemas de automação foram corrigidos: rolagem posicionava links sob a barra flutuante e PixelCopy falhava durante troca de janela; capturas agora usam UiAutomation. Esses resultados não comprovam temperatura/Now Bar no Samsung físico.
- Verificação de segredos: nenhum dos três valores fornecidos apareceu nos 40 arquivos preparados nem nos DEX do APK debug. Não publicar `AGENTS.md`/este contexto; continuam locais por preferência anterior.

## Revisão atual — navegação e perfil (1.0.48 concluída)

Esta seção substitui as decisões conflitantes da 1.0.47 descritas no histórico abaixo.

- Abas principais: **Início, Explorar, Perfil**; Busca continua na lupa. Biblioteca/Diário e Ajustes são páginas filhas acessíveis pelo Perfil. Não recolocar Ajustes/Biblioteca como abas principais sem nova solicitação.
- Novas entradas: `ui/screens/ReaderProfileScreen.kt` e `ExploreScreen.kt`. Perfil mostra resumo real, resenhas, quatro HQs e quatro personagens favoritos. Personagens usam nomes/monogramas, com sugestões e entrada livre; não há retratos inventados.
- Favoritos ficam em `AppSettings.profileFavoriteComicIds` e `profileFavoriteCharacters`, listas únicas de até quatro. `SafeStorage` e `CloudUserPreferences` fazem persistência por conta e sincronização. Integração com documento de preferências existente, sem alterar regras ou criar banco.
- Banco consultado: Firestore **Enterprise / FIRESTORE_NATIVE**, us-east1, com acesso nativo habilitado. Não há novas consultas relacionais ou buscas no Firestore nesta revisão; recomendações usam o catálogo local e histórico já carregado.
- Explorar possui seleções/recomendações locais e páginas expandidas. Comunidades é futura, explicitamente indisponível. Busca vazia não repete recomendações; pesquisa inclui coleção e catálogo externo/local. Cadastro manual só no cabeçalho.
- Ícone: seletor discreto **Cor do app / Original**; fundo preto/branco segue sistema, Q acompanha destaque ou marca original. Monocromáticos adaptam contraste. `LauncherAppearance` guarda alterações enquanto `foreground=true`; aplica via worker após sair, com atraso de cinco segundos e guarda contra retorno rápido. Não desabilitar alias durante a seleção nem no `onResume`/inicialização da Application. Periodicidade mantém adaptação de tema no sistema.
- Aliases novos: `LauncherColor<Cor>Day/Night`; antigos preservados para migração. Recursos tintados/adaptive/fallback em `res/drawable` e `mipmap-anydpi[-v26]`. Não alterar `QuadrinheiroLogo.kt` incidentalmente (workflows legados).
- Voltar dos ajustes: rótulo da página anterior no topo e versão compacta após rolagem. Alvo 44 dp e animação respeitam movimento reduzido; compactação usa `derivedStateOf`. Aparência tem descrição inferior, assim como outras opções; descrições de uma linha.
- Superfícies neutras: base clara #F2F2F7, escura #000000; elevadas brancas/#1C1C1E, independentes do destaque. Removidos gradiente colorido global e brilho inferior de destaque.
- Atmosfera da capa: **quarto superior**, 144×216, suavização de 18 dp e fade progressivo até opaco no limite; substitui a metade superior/28×42 da 1.0.47.
- Release publicada e verificada: **1.0.48 / 1048**, commit `6b74ffcd3f8d509984d3643e586125516d557d22`, Actions `41682013236` concluído com sucesso. **78 testes do CI passaram**, incluindo favoritos no Firestore com regras existentes. Manifesto público latest confirmou versão, commit e certificado permanente. APK SHA-256: `a45cbdc6377ab33a5d7efbaf148f2785e5e2d0a00bd4492189303c3ddcfd6762`.
- Validação local concluída: build debug, 16 testes unitários selecionados e 11 cenários nativos no Android 17 passaram; incluem 64 mudanças rápidas entre todas as novas rotas e os controles antigos. Buffer de crashes vazio. Launcher resolveu `com.example.LauncherColorBlackDay` após a saída, confirmando aplicação em segundo plano. Uma asserção do teste de Voltar foi corrigida para considerar semântica mesclada, sem alterar o comportamento do produto.
- Arquivos `AGENTS.md` e este contexto continuam locais, sem incluir no export de publicação automaticamente.

## Consulta rápida

1. Leia este resumo antes de explorar o repositório inteiro.
2. Execute `git status --short` e `git log -3 --oneline` para identificar mudanças posteriores.
3. Use o mapa abaixo para abrir os arquivos da área solicitada. Prefira `rg` a leituras integrais dos arquivos grandes.
4. Preserve alterações locais do usuário, dados, identidade do app e assinatura.
5. Separe comportamento verificado de hipótese. Não afirmar que um problema do Samsung foi resolvido apenas porque passou em emulador.

## Produto e ambiente

- App Android nativo em Kotlin/Jetpack Compose para catalogar HQs/mangás, acompanhar páginas, registrar avaliações/resenhas, manter diário e descobrir obras.
- Repositório: <https://github.com/felipemloiola/Quadrinheiro>.
- Pasta deste projeto/chat: `D:\Codex\Quadrinheiro`.
- Namespace Kotlin: `com.example`; applicationId: **`com.aistudio.quadrinheiro.lqglas`**. Não confundir os dois.
- Kotlin 2.2.10, Compose/Material 3, AGP 9.1.1, Gradle 9.3.1, compileSdk 36.1, targetSdk 36, minSdk 24.
- CI usa JDK 21. Ambiente local conhecido: JBR do Android Studio em `C:\Program Files\Android\Android Studio\jbr`; SDK em `D:\Android\Sdk`.
- Não há `gradlew` executável no repositório. Gradle local conhecido: `C:\Users\Felipe\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat`.
- Usuário informou **Samsung com One UI 9 / Android 17**. Compatibilidade específica da Samsung e temperatura precisam de validação física.

## Preferências e decisões do usuário

- Interface inspirada nas HIG recentes da Apple e Liquid Glass, com continuidade de gestos, molas, refração e animações perceptíveis. Não simplificar os efeitos para esconder problemas de desempenho.
- Referências pedidas: [Apple HIG](https://developer.apple.com/design/human-interface-guidelines), [Liquid Glass](https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass), [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass), [LiquidGlassKMP](https://github.com/philipplackner/LiquidGlassKMP).
- Skills locais indicadas neste chat: `D:\Codex\skills\apple-design\SKILL.md` e, posteriormente, `D:\Downloads\SKILL.md`. A última foi usada na revisão. Releia a skill aplicável se precisar das instruções completas; este resumo não a substitui.
- O usuário rejeitou alterações anteriores na tab bar, seletores Estante/Diário e slider. Os controles originais foram restaurados. Preserve sua geometria e física; não redesenhar silenciosamente.
- Títulos simples, poucos textos redundantes, símbolos coerentes, áreas de toque adequadas e hierarquia clara. Metas ficam no início, não no Diário.
- Compartilhamento só vertical; não mostrar Feed, Stories, dimensões ou “Letterboxd” na interface. Não recolocar banners gigantes de compartilhar: a ação do cabeçalho basta.
- Atualização deve baixar dentro do app e chamar o instalador Android, sem abrir Chrome para baixar.
- O usuário já autorizou implementar e publicar as revisões anteriores. Este arquivo foi solicitado como memória local; sua criação não implica uma nova publicação.

## Mapa do código

Os caminhos seguintes são relativos a `app/src/main/java/com/example/`.

| Área | Arquivos principais |
|---|---|
| Composição e navegação principal | `MainActivity.kt`, `ui/components/AppPageTransition.kt` |
| Estado e ações | `viewmodel/QuadrinheiroViewModel.kt` |
| Modelos/preferências | `model/Models.kt`, `model/ActiveReading.kt` |
| Início, Biblioteca e Diário | `ui/screens/MainTabsScreens.kt`, `MainTabsSubcomponents.kt` |
| Ajustes | `ui/screens/SettingsScreen.kt`, `SettingsBackButton.kt`, `StorageSettingsCard.kt` |
| Boas-vindas/login | `ui/screens/WelcomeIntroScreen.kt`, `AuthScreen.kt`, `AuthSubcomponents.kt`, `ui/components/HomeLoadingSkeleton.kt` |
| Edição/informações da HQ | `ui/components/CatalogModal.kt`, `CatalogModalSubcomponents.kt`, `SheetToolbar.kt`, `FluidSheetState.kt` |
| Compartilhamento | `ui/components/SocialShareCardSheet.kt` e renderizador Canvas HD |
| Controles/materiais | `ui/components/LiquidGlassComponents.kt`, `LiquidGlassEngine.kt`, `RetainedGlassMenu.kt`, `CompactReadingAction.kt` |
| Tema, movimento e texto | `ui/theme/Theme.kt`, `Color.kt`, `Type.kt`, `ReadableColors.kt`, `GlassAppearance.kt`, `AppMotion.kt` |
| Ícones/marca | `ui/components/LucideIcons.kt`, `QuadrinheiroLogo.kt`, `LauncherIconPicker.kt`; `LauncherAppearance.kt`, `LauncherIconStyle.kt` na raiz do pacote |
| Iluminação da capa | `ui/components/ReadingCoverAtmosphere.kt`, `BackgroundFocus.kt`, integração em `MainActivity.kt` |
| Dados locais/cache | `data/SafeStorage.kt`, `StorageMaintenance.kt`, `CacheMaintenance.kt` |
| Conta/sincronização | `data/QuadrinheiroAuthManager.kt`, `GoogleClientId.kt`, `QuadrinheiroCloudRepository.kt`, `QuadrinheiroSyncWorker.kt`, `FirestoreSnapshotDispatch.kt` |
| Busca/recomendações/séries | `data/MultiApiSearchEngine.kt`, `PortugueseComicTranslator.kt`, `CuratedCatalog.kt`, `CatalogMatchIndex.kt`, `PersonalizedDiscovery.kt`, `SeriesSequenceEngine.kt` |
| Sessão de leitura/Now Bar | `reading/ReadingLiveActivity.kt`, `ReadingNowBarControl.kt` |
| Widgets | `widget/ReadingNowWidget.kt`, `RecentShelfWidget.kt`, `ActivityRingsWidget.kt`, respectivos receivers, `WidgetAppearance.kt`, `UpdatePageAction.kt` |
| Atualizador | `updates/AppUpdates.kt`, `InAppUpdateDownload.kt` |

O motor Backdrop também está em `app/src/main/java/com/kyant/backdrop/`, incluindo controles em `catalog/` e `RuntimeShaderCache.kt`. Não presumir que toda alteração óptica fica no pacote `com.example`.

## Dados, login e isolamento entre contas

- Firebase real: **`gen-lang-client-0375936605`**.
- Banco Firestore nomeado: **`ai-studio-android-quadrinh-d0141987-7d2c-491d-b885-fc910c102b71`**. Esse nome não é o projectId Firebase.
- Login Google exige configuração OAuth Web real; não fabricar client ID. `app/google-services.json` é local/ignorado; Actions recebe `GOOGLE_SERVICES_JSON_BASE64`.
- Certificados de release foram cadastrados para o app Android durante este chat.
- `SafeStorage` separa convidado e contas. Preservar isolamento, dirty flags, tratamento de respostas tardias e sincronização pendente. Uma gravação de Alice não pode substituir o espelho de Bob.
- Não limpar banco Firestore, biblioteca, contas ou backups para reduzir armazenamento. Não alterar regras de produção como efeito colateral de uma revisão visual.
- Widgets devem atualizar após troca de conta e jamais mostrar o conteúdo da conta anterior. O widget Lendo Agora deve acompanhar a seleção usada na iluminação do app; `ActiveReading` centraliza a escolha/fallback.
- Now Bar é notificação de progresso com ações; preferência em **Ajustes → Biblioteca**, sem controle redundante na tela inicial. Deve funcionar na tela bloqueada conforme permissões/suporte do sistema. Não simular reprodução de áudio ou sessão Spotify para forçar controles de mídia. A apresentação final é controlada pela Samsung/Android.

## Estado visual e funcional consolidado

- Tab bar e controles originais preservados. Arrasto dos segmentos/toggle acompanha o dedo; soltura usa velocidade e mola. Movimento reduzido continua respeitado.
- Menus “Mais recentes” e equivalentes preservam dimensões/divisórias, refração e molas. O realce mantém a última opção válida quando o dedo sai do menu. Animação na camada gráfica evita medir/redimensionar texto por quadro.
- Biblioteca tem título e ordenação **na mesma linha**, título 28 sp com uma linha e espaçamento explícito; picker limitado a 144 dp. Contagens pertencem aos filtros de status, sem repetição desnecessária.
- Edição: salvar/excluir próximos, cabeçalho alinhado, sem banner redundante de compartilhar e sem sugestões de obras já catalogadas. Campo de texto tem limpar perto da lateral com alvo de toque preservado.
- Compartilhamento: ações próximas da prévia, composição vertical em 1080×1920 via Canvas; escurecimento acompanha a saída da folha. Removida captura/cópia de prévia não consumida pelo exportador.
- Diário foca registros cronológicos e resumo das leituras/avaliações; não deve voltar a reunir banners de compartilhamento ou metas.
- Boas-vindas/resumo devem caber sem rolagem em altura compacta, com adaptação a fonte maior. Skeleton de login reflete a estrutura inicial real.
- Metadados: Batman: Ano Um encadernado não deve herdar #1 mensal/36 páginas; fascículos Batman #404–407 e volumes numerados continuam numerados. Respeitar precedência de encadernado sobre `#`.
- Busca inicial usa recomendações locais baseadas em leituras/avaliações, excluindo obras já salvas de qualquer status. Cache de séries considera conteúdo da coleção e tem limite de 96 grupos.

## Mudanças específicas da 1.0.47

- Ajustes: **Acessibilidade**, **Biblioteca**, **Diário**, **Busca**, **Armazenamento**; títulos de uma linha e descrições curtas. Liquid Glass usa símbolo de regulagem, armazenamento usa símbolo apropriado.
- Voltar dos ajustes: círculo constante de **44 dp**, chevron, sempre disponível; removidos texto/forma que mudavam ou sumiam com a rolagem.
- Rodapé: moldura **42 dp**, Q em **34 dp**; não ampliar o quadrado junto com o Q. Nome/descrição vêm antes de versão/verificar atualização. Espaço inferior comporta barras flutuantes e leitura ativa.
- Nível de Liquid Glass agora modifica os parâmetros reais em `withGlassAppearance()`. Default 26% preserva a calibração anterior. Padrão/Fosco são distintos. `GlassLevelPreview` possui fonte de backdrop própria e prévia real.
- Atmosfera da capa: somente **metade superior (2/4)**, imagem decorativa 28×42, alpha discreto e fade até fundo opaco no limite. Sem blur de tela inteira. Brilho inferior usa tema, não prolonga a capa.
- Ícone do launcher: Padrão, Preto, Branco, Roxo, Azul, Verde, Laranja, Vermelho; apenas fundo muda, foreground multicolorido é o mesmo. Aliases no Manifest e recursos adaptive/fallback. Padrão acompanha claro/escuro; cores fixas dispensam worker periódico de tema.
- Capas Coil: **64 MiB** de disco; HTTP: **16 MiB**. Antes eram 96/32 MiB. Total de 80 MiB é desses caches, NÃO do armazenamento total do aplicativo. Cache Firestore de 100 MiB foi preservado.
- Armazenamento mede cache/dados locais e limpa caches recuperáveis pelas APIs dos motores. Traduções têm limite de 1.024 entradas/4 MiB; exports/APKs temporários têm política própria em `CacheMaintenance`.
- Prefetch: no máximo seis URLs distintas, cancelando disposables antigos e encerrando no `onCleared`. Pedidos visíveis de imagem não foram removidos.
- `SafeStorage.setItem` evita regravar conteúdo idêntico; conserva fallback e tratamento de falhas.
- Placeholder de capas anima por seis segundos e fica estável depois; fase é lida no desenho, evitando recomposição por quadro. Animações interativas permanecem.
- Reserva de shaders: uma instância por programa, antes oito; cada nó mantém instância mutável exclusiva. Não compartilhar uniforms mutáveis entre controles.
- Navegação rápida: limite de duas árvores durante transições interrompidas e proteção de camadas liberadas, implementados na revisão anterior.
- Redução de aquecimento é objetivo das otimizações, não uma medição comprovada no aparelho. Não prometer graus, FPS ou eliminação de todos os crashes sem evidência.

## Build, testes e armadilhas conhecidas

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
# Use o gradle.bat instalado, ou gradle disponível no PATH:
gradle :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
gradle :app:testDebugUnitTest --console=plain
```

- A suíte completa de dados/regras exige emuladores Firebase Auth/Firestore. O workflow de release é a referência dos comandos/variáveis; não apontar testes para produção.
- Testes unitários: `app/src/test`; UI/gestos: `app/src/androidTest`; benchmark separado em `macrobenchmark/`.
- Última validação: **74 testes no CI**, build assinado e manifesto corretos. **44 cenários instrumentados** validados em execuções separadas, incluindo estresse de navegação, gestos, visual, aliases e download de APK real. Não afirmar que tudo passou em uma execução única.
- Houve timeout de PixelCopy e diferença de borda após estresse no emulador; rodapé e `RenderLayerEquivalenceTest` passaram ao repetir isoladamente, sem mudar tolerâncias.
- **Importante para Coil/Robolectric:** `AppearanceStorageTest` instala `Dispatchers.Main` com `UnconfinedTestDispatcher` antes de inicializar Coil e restaura depois. Sem isso, a execução conjunta com `WidgetCoverTest` travava no dispatcher Main pausado. Correção confirmada com ambos (5 testes, 24 s), sem aumentar timeout.
- Os testes Gradle têm timeout de dois minutos. Não aumentar para esconder bloqueio.
- Testes de MainActivity devem preparar convidado/boas-vindas antes de lançar e cancelar registro de download fictício pendente para não abrir uma folha sobre a UI.
- Relógio de Compose pausado durante gesto/espera por idle pode bloquear o teste. Não confundir bloqueio do teste com crash do produto.
- Emulador isolado usado: **FluidControls**, API 37, serial emulator-5554, AVD_HOME em `.build-outputs/avd`; encerrado após os testes. Não tocar no AVD pessoal MediumPhone. Confirmar inventário/serial antes de iniciar ou encerrar qualquer instância.
- `.build-outputs/` é ignorado e contém logs/capturas/helpers deste chat. Scripts ali podem ser antigos e mutantes; inspecione antes de executar. Não executar `benchmark.cjs` supondo que seja runner de benchmark.

## Publicação e identidade (histórico da 1.0.47; dados atuais acima)

- Última release verificada: [v1.0.47](https://github.com/felipemloiola/Quadrinheiro/releases/tag/v1.0.47), versionCode **1047**.
- Commit atual da release: **`2226438057ff0330aaa5829704161204bcaf245c`**. Implementação principal: `c434eed4e7d2ed7a8608bb2de2cb71cbbcea2f05`; segundo commit corrige somente o isolamento dos testes.
- Workflow: `.github/workflows/release-apk.yml`. Versões são `1.0.<run_number>` e `1000 + run_number`; base do build local continua 1.0/1 e recebe inputs no CI.
- Assets: `quadrinheiro.apk`, `SHA256SUMS.txt`, `update.json`. Endpoint do atualizador: <https://github.com/felipemloiola/Quadrinheiro/releases/latest/download/update.json>.
- Certificado permanente SHA-256 (público): **`ec5ec8852a218db2171959717d72771abac8535f848506da66cb7180874a761f`**. Preservar assinatura/applicationId para instalar updates sem perder dados.
- APK 1.0.47 SHA-256: `c468c4230caab5384292201f01f0eee034e0dc0ac6d3831e0b28f4a5941cef93`.
- Secrets já configurados no GitHub; nunca copiar keystore, senhas, tokens ou conteúdo de `google-services.json` para contexto, commits ou logs.
- Preferir conector GitHub para publicação. Fluxo usado: `create_tree` sobre árvore base → `create_commit` → `update_ref` com `expected_sha`, sem force. Conferir head remoto antes. Não extrair credenciais do Git.
- Depois da publicação pelo conector, `git fetch origin main` e, se apropriado e sem perder mudanças, `git reset --mixed FETCH_HEAD` sincronizam o checkout. Não usar reset destrutivo.
- Confirmar Actions concluído com sucesso, testes, assets, certificado/versionCode/sourceCommit do manifesto público antes de dizer que update foi liberado.
- **Cuidado com workflows legados:** alterações em `.github/scripts/**` ou `QuadrinheiroLogo.kt` podem disparar automações que modificam código. Não editar incidentalmente.
- Marcadores `APP_UPDATES_HOST_V1` em MainActivity e `APP_UPDATES_SETTINGS_V1` nos ajustes evitam reinjeção duplicada/mal posicionada do atualizador pelo script de release. Preservá-los.
- `CatalogModal.kt` e `LiquidGlassComponents.kt` possuem histórico CRLF. Evitar diffs integrais de finais de linha. Helper local `.build-outputs/export.cjs` preservava essa particularidade; revisar qualquer helper antes de reutilizar.

## Revisão de bugs/otimização (04/11/2026) — patches locais aplicados

Auditoria feita sobre o checkout local em `main` (`403c2e109e90d4c49390cd39f224fc5d7e8fd776`, árvore limpa exceto `AGENTS.md`/`CONTEXTO-DO-APP.md`). Os patches abaixo estão **somente na árvore de trabalho**, não commitados, não publicados:

- `SafeStorage.kt`: `@Synchronized` em `loadGuestState`, `loadAccountState`, `parseStoredStateJson`, `loadState` e `saveShowcase` (evita leitura/gravação concorrente inconsistente).
- `LiquidGlassComponents.kt`: anel do avatar com cor fixa `Black @ 0.18` (contraste estável em tema claro/escuro); `LiquidGlassModeSwitcher` usa `snap` quando `motion.reduced`; `LiquidGlassSegmentedControl` passa `reduceMotion` para as animações legacy e faz `snap` do `offsetAnimation` no fim do arrasto.
- `ReaderProfileScreen.kt`: CTA "Editar perfil" com alvo de toque mínimo de 44 dp. O `offset(y = 40.dp)` do avatar do banner foi **mantido**: ele é intencional e casa com `heightIn(min = 80.dp)` da Row (mesmo commit `c4e3b57`), então o achado de sobreposição era falso positivo.
- `legacy/DampedDragAnimation.kt` e `legacy/InteractiveHighlight.kt`: novo parâmetro opcional `reduceMotion: () -> Boolean = { false }`. Quando ativo, escala/pressão/velocidade/highlight ficam neutros, `updateValue`/`animateToValue` fazem `snapTo` e `press`/`release` retornam cedo. Física, geometria e gestos originais preservados quando `reduceMotion` é falso.
- Chamadas atualizadas: `LiquidSlider.kt` (estilo `settings`), `LiquidBottomTabs.kt` (rodapé) e `LiquidGlassSegmentedControl` (Estante/Diário) usam `rememberReducedMotion()`.

Segundo lote (também local, não commitado):

- `MultiApiSearchEngine.kt`: `searchCacheKey(query, searchLanguage, provider, hdCovers)` centraliza a chave do cache; `getCached` deixou de descartar o prefixo (`substringAfterLast(":")`), que fazia trocar idioma/provedor/HD reaproveitar resultados de outra configuração. `QuadrinheiroViewModel` usa o mesmo helper.
- `ModernCatalog.kt`: `accept()` virou `@Synchronized`; chamadas concorrentes de busca/catálogo podiam perder atualizações por corrida no `MutableStateFlow`. A fusão de metadados por id está no quarto lote, abaixo.
- `PortugueseComicTranslator.kt`: chave do cache de sinopse passou a ser `take(180) + "#" + length + "#" + hashCode()` (antes, sinopses longas com o mesmo início colidiam e podiam devolver tradução errada); a verificação de "já traduzido" agora normaliza o texto do mesmo jeito que o armazenamento, aumentando os acertos de cache.
- `LiquidSlider.kt`: semântica de acessibilidade (`progressBarRangeInfo` + ação `setProgress`) para TalkBack ajustar o valor sem gesto.
- `LiquidGlassSegmentedControl`: itens usam `Modifier.selectable` (estado "selecionado" anunciado) e a linha usa `selectableGroup()`.

Fora do escopo aplicado, aguardando decisão: política de migração do epoch (`CatalogEpoch.RESET_AT = 1793736512503L` local), conflito de sync (`localWins` em `QuadrinheiroCloudRepository`), retry terminal do `QuadrinheiroSyncWorker`, sync de contas por e-mail. `searchLanguage`/`provider`/`hdCovers` não alteram a busca real por limite do backend (detalhado no fim da seção). `API_TIMEOUT_MS` foi investigado e **removido** (declaração morta): os clientes OkHttp já aplicam connect/read/call timeout.

### Diagnóstico de sync/epoch (somente leitura, sem alterar dados)

Fluxo real: alteração local marca `isDirty`/`updatedAt` → `QuadrinheiroSyncWorker` (WorkManager, `enqueueDirtySync` com `REPLACE`) empurra perfis e quadrinhos sujos → `mergeLocalAndRemoteComics` resolve conflito pelo maior `updatedAt` → `markComicsSynced` limpa o `isDirty`.

- **Bug confirmado (retry infinito):** `CommunityAutoSync.flush` (`CommunityAutoSync.kt:32`) faz `continue` quando o item da fila não está marcado `deleted` e o quadrinho não existe mais. O item nunca é reconhecido (`acknowledge`), `hasPending` fica sempre `true`, e o worker (`QuadrinheiroSyncWorker.kt:68`) devolve `Result.retry()` sem limite — o teto de 3 tentativas só vale para exceções (linhas 72-78). Gatilho provável: reset do epoch apaga os quadrinhos locais, mas `community_pending_$uid` não é limpo; também ocorre ao excluir uma HQ depois de enfileirar avaliação. Sem perda de dados; custo é retry em background por tempo indefinido.
- **Risco de relógio no merge (corrigido neste lote):** antes `resolvedUpdatedAtMillis()` preferia `updatedAtMillis` (relógio do cliente) ao `@ServerTimestamp`. Agora o servidor é a fonte de verdade (`@ServerTimestamp`), com fallback para o cliente. Duas máquinas com relógios diferentes não fazem mais uma sobrescrever a outra em `mergeLocalAndRemoteComics`; empate vai para o local (`local.isDirty && updatedAt >= remoto`).
- **Risco de relógio no epoch:** `afterReset(comic) = comic.updatedAt > RESET_AT` é aplicado só às listas remotas (ViewModel linhas 666/686/755), mas usa relógio do cliente. Aparelho com data anterior a `RESET_AT` (2026-11-03 20:08:32 UTC) cria HQ que é filtrada em toda leitura da nuvem — o item existe localmente, mas não volta após reinstalação/em outro aparelho. Não há marcador de epoch nos documentos do Firestore (só em `SafeStorage`), então a filtragem é puramente temporal. Mitigação automática aplicada no lote de sync: como `toComicEntry()` repassa `resolvedUpdatedAtMillis()` (agora `@ServerTimestamp`), as listas remotas passam a filtrar com o relógio do servidor — o risco de relógio atrasado em HQ remotas foi reduzido.
- **Resolução local do epoch está correta e testada:** `parseStoredStateJson` descarta os quadrinhos quando `catalogEpoch < 51` e reescreve o estado com a versão nova; `CatalogRevision51Test` cobre isso.
- **Assimetria de retry:** exceções param em 3 tentativas e viram `failure` (dados continuam sujos no aparelho, novo `enqueue` reabre); falha de `flush` não tem teto.

Correções aplicadas neste diagnóstico:

- `CommunityAutoSync`: nova função pura `pendingAction(deleted, comic, revision)` (enum `SKIP`/`WITHDRAW`/`PUBLISH`). Item órfão (quadrinho removido, inclusive pelo reset do epoch) agora é reconhecido como **retirada** (`withdrawKey`) e sai da fila, em vez de ficar em `continue` para sempre. Regressão coberta por `app/src/test/java/com/example/CommunityAutoSyncTest.kt` (6 testes, sem Firebase) e `app/src/test/java/com/example/SyncMergeTest.kt` (5 testes que travam o merge local/nuvem: remoto mais novo limpa `isDirty`, local sujo mais novo vai para upload, HQ só-local é preservada e agendada, tradução local sobrevive e a política LWW atual fica documentada).
- `QuadrinheiroCloudRepository`: `resolvedUpdatedAtMillis()` passou a preferir o `@ServerTimestamp` do Firestore (fonte de verdade, imune a relógio do aparelho) e usar `updatedAtMillis` do cliente apenas como fallback. Correção do risco de relógio no merge; testada em `SyncMergeTest` (servidor acima do cliente e fallback).

- `QuadrinheiroCloudRepository.fromComicEntry`: carimbo de upload usa `System.currentTimeMillis()` quando `comic.updatedAt <= RESET_AT`. Sem isso, HQ criada com relógio atrasado (ou item migrado com `updatedAt = 0`) ficava abaixo de `RESET_AT` e era filtrada em toda leitura da nuvem. Itens realmente posteriores ao reset mantêm o carimbo original.

Terceiro lote (acessibilidade e testes):

- `ProfileEditorScreen.kt`: ações do banner ("Reenquadrar"/"Remover banner") com altura mínima de 44 dp e espaçamento consistente; avatar do banner com rótulo "Foto de perfil, toque para alterar"; opções de avatar usam `selectable` com `Role.RadioButton` e rótulo "Avatar <nome>" (antes o leitor de tela anunciava só "Botão").
- `app/src/test/java/com/example/CatalogEpochUploadTest.kt`: 4 testes cobrindo o limite de `afterReset` (estritamente maior) e o carimbo de upload (preserva pós-reset, atualiza `updatedAt = 0` e valor abaixo de `RESET_AT`).

Quarto lote (catálogo acumulado):

- `ModernCatalog.accept`: antes `(value + items).distinctBy { it.id }` mantinha **sempre a primeira** versão do id, então capa, sinopse e páginas nunca eram atualizadas para itens já vistos. Agora a lista é remontada em `LinkedHashMap` com `preferred(existing, incoming)`: a versão nova entra (renovando tradução/capa), exceto quando é mais pobre (sem capa/sinopse/páginas/ISBN) — nesse caso a antiga é preservada. O limite de 396 itens e a ordem de chegada continuam iguais. Regressão em `app/src/test/java/com/example/ModernCatalogMergeTest.kt` (4 testes).

Ainda pendentes por decisão de produto/risco: (a) política de conflito `localWins` e uso do timestamp do servidor como fonte de verdade — **RESOLVIDA neste lote**: `resolvedUpdatedAtMillis()` agora prefere o `@ServerTimestamp` (fonte de verdade, imune a relógio do aparelho) e usa o valor do cliente apenas como fallback; (b) marcador de epoch nos documentos do Firestore para filtrar por versão em vez de tempo — **BLOQUEADO no escopo atual**: `firestore.rules` valida com `data.keys().hasOnly(allowedKeys)`, e a lista de campos permitidos dos quadrinhos (`ownerId`, `externalId`, `title`, … `updatedAt`) não inclui campo novo de versão/epoch; exigiria atualizar as regras e publicar no Firebase (deploy). Não foi aplicado cliente sem regras;

Limites do backend do catálogo (verificado em `catalog-service/providers.cjs`, `store.js` e `worker.js`): `validate()` aceita e repassa somente `op`, `q`, `id`, `page` e `kind`. Por isso `searchLanguage`, `provider` e `hdCovers` não podem mudar a busca sem alterar e reimplantar o worker — ficaram apenas na chave do cache (correção de cache já aplicada). As capas já vêm de `image.super_url` (a maior), então "capas HD" não tem o que ligar/desligar no app. `source_lang:'en'` em `store.js` é correto para descrições do ComicVine (o texto original é inglês); só conteúdo de outra origem não inglesa poderia ser mal traduzido, e isso depende do worker, não do app.

**Lote do item 3 (backend) — aplicado no código-fonte local:** `providers.cjs` passou a aceitar e validar `lang` (`PT`/`EN`/`ALL`), `provider` (`MULTI`/`COMIC_VINE`/`METRO`) e `hd` (booleano), devolvendo-os na validação (o cache do Durable Object passa a ser segregado por configuração porque a chave hash do input). `service.cjs` passou a rotear para Metron quando o provider é `METRO` (ou `MULTI`, preservando o comportamento padrão). Testes: `node --test catalog-service/test` → **13/13 aprovados**. Deploy (`wrangler deploy`) pendente de autorização.

Validação: os patches foram revisados por `git diff`; `:app:compileDebugKotlin` passou (6 execuções + execuções finais a partir dos lotes); Suíte unitária completa com os dois testes de emulador excluídos por init script (`CommunityRuleTest`, `QuadrinheiroRepositoryRuleTest`): **27 classes, 114 testes, 0 falhas, 0 puladas** — inclui os 9 testes novos/relacionados do lote de sync (`SyncMergeTest`). Sem a exclusão, `failFast = true` interrompe a execução no primeiro teste que precisa dos emuladores Firebase (hoje `CommunityRuleTest.publicationCommentsAndWithdrawalRespectOwnershipAndPrivacy` com `java.net.ConnectException`) — falha ambiental, não relacionada aos patches; `firebase` CLI e `adb` não estão disponíveis nesta máquina. `:app:assembleDebugAndroidTest` compila os 18 arquivos de teste instrumentado (execução ainda pendente de aparelho/emulador) e `:app:assembleDebug` gerou o APK em `app/build/outputs/apk/debug/app-debug.apk` para validação manual no Samsung.

As mudanças de código foram commitadas localmente na `main` em três commits temáticos (`fix(sync)`, `perf(dados)`, `fix(a11y)`), mais este contexto. Nada foi enviado ao GitHub.

## Outras referências e manutenção desta memória

- `README.md`: visão pública, desenvolvimento e recursos atuais.
- `docs/REVISAO-HIG-NOW-BAR.md`: histórico técnico/visual com links oficiais. Trechos antigos descrevem estados anteriores; a seção final e este contexto registram a revisão mais recente.
- `THIRD_PARTY_NOTICES.md`, `licenses/`: termos e créditos das dependências/materiais.
- Atualize este arquivo ao concluir mudanças relevantes: release/commit, decisões aceitas/rejeitadas, arquivos de entrada, testes realmente executados e limitações. Não acumular transcrições completas, logs ou resultados abandonados.
- Se o usuário mudar um requisito, a instrução atual tem precedência sobre este histórico.
