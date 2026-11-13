# Voltar preditivo e Live Updates

O Quadrinheiro usa as APIs do Android com a linguagem visual de vidro do app. As referências são as [Live Activities da Apple](https://developer.apple.com/design/human-interface-guidelines/live-activities), o [voltar preditivo em Compose](https://developer.android.com/develop/ui/compose/system/predictive-back-setup) e os [Live Updates do Android](https://developer.android.com/develop/ui/views/notifications/live-update).

## Voltar

- Páginas principais, ajustes, subcategorias e estatísticas mostram o destino durante o gesto. Coleções em Explorar têm seu próprio destino de retorno.
- Folhas que usam `FluidSheetState`, incluindo compartilhar e os modais do catálogo, acompanham o gesto e restauram a posição anterior ao cancelar, inclusive quando expandidas.
- Cancelar preserva a rota e o estado da página atual. Confirmar navega uma vez; botão Voltar e teclado continuam funcionando. Na raiz de Início, o sistema controla o retorno ao launcher.
- Transformações usam `graphicsLayer`, sem mudar dimensões por quadro, sem outra captura de fundo e com no máximo duas páginas compostas por transição. A página por trás não recebe toque nem semântica de acessibilidade durante a prévia.
- `LocalAppMotion.reduced` remove deslocamento e escala. O manifesto permite o callback nativo; Androids antigos mantêm o botão Voltar pelo AndroidX.

Menus, diálogos e editores com confirmação própria preservam a prioridade de fechamento. A mudança não converte toda navegação interna de cada diálogo em uma pilha nova.

## Live Updates de leitura

Ative **Ajustes → Biblioteca → Live Updates de leitura**. O controle usa a preferência existente de leitura/Now Bar. O painel apresenta capa, título e progresso reais; não inventa o total quando a edição não informa páginas. O material usa o fallback de transparência do app.

A notificação usa `ProgressStyle` no Android 16+ quando há total conhecido, solicita promoção e oferece ações para voltar/avançar uma página e encerrar. Sem total conhecido, usa progresso indeterminado. Em versões anteriores, usa a notificação padrão de progresso. Não há `RemoteViews`, superfície sobreposta à câmera, fonte Apple ou dependência iOS.

O [framework inicial do Android 16](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-release/core/java/android/app/Notification.java) exigia `colorized` para promover notificações. O formato atual exige o contrário. O builder tenta primeiro o formato atual; somente na API 36, quando o framework o rejeita e aceita a variante antiga, usa a configuração legada. Não há dependência de identificação de fabricante ou números de QPR.

O Android e o fabricante decidem a apresentação na tela bloqueada, chip de status e Now Bar. O app informa permissões bloqueadas e permite abrir os ajustes adequados, com retorno aos detalhes do aplicativo no Android 7.0. Ao voltar dos ajustes do sistema, reavalia as permissões e atualiza a sessão. Um canal bloqueado não é tratado como sessão ativa.

Isolamento por conta e validação das ações antigas permanecem ativos. Não foram adicionados polling, serviço em primeiro plano ou animações contínuas. A preferência continua opcional.

### Samsung Now Bar: API pública e validação física

A [Samsung confirma a integração da Now Bar com os Live Updates do Android 16](https://news.samsung.com/cz/one-ui-8-plynulejsi-galaxy-chytrejsi-ai-a-nove-vychytavky-ktere-si-zamilujes). Não foi localizado um SDK público separado da Samsung que permita ao app garantir sua inclusão na Now Bar. A [documentação de suporte](https://www.samsung.com/ie/support/mobile-devices/how-to-use-the-now-bar-on-the-lock-screen-of-your-samsung-galaxy-device/) exige apps suportados, notificações permitidas e conteúdo autorizado na tela bloqueada. A [documentação Android](https://developer.android.com/develop/ui/views/notifications/live-update) também permite critérios adicionais do fabricante.

O app usa somente a API pública de notificações: `ProgressStyle`, solicitação de promoção, título, estado contínuo e canal `LOW`. O builder não adiciona metadados privados Samsung nem usa reflexão em `semFlags`. Esses experimentos, pesquisados no projeto [MukulBolt9/xz](https://github.com/MukulBolt9/xz), foram retirados do APK principal e preservados exclusivamente no módulo opcional de diagnóstico. A identidade do pacote, o fallback Android 7.0 e a assinatura permanecem preservados.

A publicação inicial não espera pela capa. A capa chega em uma segunda atualização, com a mesma validação de conta e geração. Salvar o progresso e voltar ao app também reavaliam a sessão, sem depender do debounce dos widgets.

O módulo opcional `tools/nowbar-probe` compila o mesmo builder público do app e publica dados fictícios, sem substituir a instalação de produção. No S23 com Android 17/One UI 9, a apresentação padrão na Now Bar foi confirmada depois de ativar **Configurações → Opções do desenvolvedor → Mais configurações → Apps → Notif. ao vivo para todos apps**. A hierarquia do AOD mostrou o título e 20% dentro do contêiner da Now Bar, e o usuário confirmou o resultado. Antes disso, os campos privados Samsung e a permissão de promoção Android não bastaram para exibir o cartão.

Esse ajuste Samsung é global: afeta outros aplicativos compatíveis e não é um requisito de integração a ser imposto silenciosamente pelo app. O teste temporário foi restaurado; depois o usuário autorizou expressamente mantê-lo ativado. O Quadrinheiro não altera essa configuração. Para desativá-la, use o mesmo caminho. Não há evidência de que apps fora da lista de suporte sejam promovidos nesse firmware com o controle desligado.

O teste físico foi feito com outro pacote e dados fictícios: não equivale a validar uma atualização do Quadrinheiro instalado. O APK de produção usa outra chave de assinatura e não pode ser substituído pelo debug preservando a instalação. Diagnósticos e identificadores privados do aparelho não integram o repositório.

Validação final do caminho público: 12 testes unitários aprovados (`ReadingLiveActivityTest`), incluindo Samsung sem campos privados, APIs 24/34/36, isolamento por conta e encerramento da sessão. APK principal e módulo isolado compilados. No teste físico final, o mesmo builder do app produziu um cartão real na Now Bar; o registro do sistema confirmou `ProgressStyle`, `PROMOTED_ONGOING` e `semFlags=0`. Dois testes instrumentados (`LiveUpdatesPresentationTest`) também passaram na etapa anterior, somente no emulador API 37. A sessão fictícia foi encerrada ao finalizar, mantendo a opção global ativada conforme autorizado.

## Validação

Os testes de regressão cobrem cancelamento/confirmação do gesto, ambas as bordas, botão Voltar com movimento reduzido, restauração de folha expandida, notificação padrão/promovível, canal bloqueado, total desconhecido e compatibilidade do destino de permissões na API 24. Um teste de apresentação usa uma HQ fictícia, texto ampliado e transparência reduzida.

Validação local: APK debug compilado, relatório unitário com 287 testes (276 passaram, 11 de Firebase ignorados sem os emuladores do backend), 24 testes do catálogo aprovados e 13 testes instrumentados aprovados no emulador API 37. Os testes de notificações incluem Robolectric nas APIs 24, 34 e 36 e elegibilidade no framework real da API 37.

Os testes conectados preservam os APKs e capturas ao terminar (`android.injected.androidTest.leaveApksInstalledAfterRun=true`). Antes de executá-los sobre uma instalação existente, confira a assinatura: o APK de debug não substitui uma instalação assinada com outra chave. Prefira um emulador dedicado a testes.

Desempenho físico (CPU/GPU, p95/p99, memória e temperatura), TalkBack em aparelho e apresentação Samsung em outros firmwares ainda precisam de validação. O cartão confirmado no S23 dependeu do controle de teste descrito acima. Testes no emulador não demonstram equivalência visual ao iOS nem desempenho de 120 Hz em aparelho físico.
