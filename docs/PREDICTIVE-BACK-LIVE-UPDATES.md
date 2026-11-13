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

### Compatibilidade Samsung em avaliação

Em aparelhos Samsung com API 35+, a notificação contínua recebe campos opcionais de atividade contínua e, quando acessível, o bit legado `semFlags=0x8000`. Essa extensão não documentada foi investigada no projeto [MukulBolt9/xz](https://github.com/MukulBolt9/xz); não garante aceitação pela Now Bar. A identidade do pacote permanece a do Quadrinheiro. O app não se identifica como Kakao Taxi, não altera listas de aplicativos permitidos e mantém a notificação Android quando a extensão é ignorada.

A publicação inicial não espera pela capa. A capa chega em uma segunda atualização, com a mesma validação de conta e geração. Salvar o progresso e voltar ao app também reavaliam a sessão, sem depender do debounce dos widgets.

O módulo opcional `tools/nowbar-probe` permite comparar a promoção Android e a extensão Samsung com dados fictícios, sem substituir a instalação de produção. No S23 com Android 17/One UI 9, o sistema aceitou a promoção padrão e a flag Samsung; em ambos os testes o usuário observou apenas uma notificação comum. Liberar o conteúdo bloqueado somente para o teste não resolveu. Na variante Samsung simples, sem solicitação de promoção e sem `ProgressStyle`, o usuário não viu nenhuma notificação. Portanto, a apresentação na Now Bar **não está validada nesse aparelho**. A configuração de conteúdo na tela bloqueada foi conferida conforme a [orientação da Samsung](https://www.samsung.com/ie/support/mobile-devices/how-to-use-the-now-bar-on-the-lock-screen-of-your-samsung-galaxy-device/); a política interna do fabricante pode impor outros critérios.

O teste físico foi feito com outro pacote e dados fictícios: não equivale a validar uma atualização do Quadrinheiro instalado. O APK de produção usa outra chave de assinatura e não pode ser substituído pelo debug preservando a instalação. Diagnósticos e identificadores privados do aparelho não integram o repositório.

Validação adicional dessa adaptação: 15 testes unitários aprovados (`ReadingLiveActivityTest` e `SamsungNowBarCompatibilityTest`), dois testes instrumentados aprovados (`LiveUpdatesPresentationTest`) somente no emulador API 37, APK principal e módulo isolado compilados. A sessão fictícia no S23 foi encerrada ao finalizar.

## Validação

Os testes de regressão cobrem cancelamento/confirmação do gesto, ambas as bordas, botão Voltar com movimento reduzido, restauração de folha expandida, notificação padrão/promovível, canal bloqueado, total desconhecido e compatibilidade do destino de permissões na API 24. Um teste de apresentação usa uma HQ fictícia, texto ampliado e transparência reduzida.

Validação local: APK debug compilado, relatório unitário com 287 testes (276 passaram, 11 de Firebase ignorados sem os emuladores do backend), 24 testes do catálogo aprovados e 13 testes instrumentados aprovados no emulador API 37. Os testes de notificações incluem Robolectric nas APIs 24, 34 e 36 e elegibilidade no framework real da API 37.

Os testes conectados preservam os APKs e capturas ao terminar (`android.injected.androidTest.leaveApksInstalledAfterRun=true`). Antes de executá-los sobre uma instalação existente, confira a assinatura: o APK de debug não substitui uma instalação assinada com outra chave. Prefira um emulador dedicado a testes.

Desempenho físico (CPU/GPU, p95/p99, memória e temperatura), TalkBack em aparelho e apresentação específica da Samsung precisam de validação em hardware. Testes no emulador não demonstram equivalência visual ao iOS nem desempenho de 120 Hz em aparelho físico.
