# Revisão de interface e sessão de leitura

## Escopo

Revisão dos componentes compartilhados e dos fluxos relatados: menus, catálogo, registro/informações da HQ, atualização, carregamento inicial, ícone e acompanhamento da leitura. As orientações da Apple foram traduzidas para Kotlin/Compose e APIs Android. Isto não representa certificação nem cobertura de toda a documentação da Apple.

## Ajustes

- **Movimento e resposta:** o menu mantém a medida final dos itens durante a abertura. Molas interrompíveis animam a camada gráfica em vez de recalcular largura, altura e composição dos textos por quadro. O realce acompanha o dedo e preserva a última opção válida quando ele sai dos limites. O modo de movimento reduzido continua usando a política compartilhada. A geometria restaurada dos segmentos, sliders e tab bar foi preservada.
- **Legibilidade:** cores semânticas são corrigidas por luminância: texto principal 7:1, secundário 5,5:1 e terciário/acento 4,5:1 sobre a superfície de referência. A preferência de contraste aumentado eleva os textos para 7:1. Os materiais de conteúdo recebem mais opacidade e a iluminação da capa nas folhas é discreta. Fundos translúcidos ainda exigem inspeção visual; a garantia numérica refere-se à superfície sólida de referência.
- **Tipografia e ações:** os botões do catálogo quebram a linha sem cortar “Quero Ler”; títulos e metadados seguem uma hierarquia mais legível. São mantidos os símbolos existentes, com rótulos nas ações de leitura; SF Symbols não foi importado como dependência Android.
- **Folhas:** área do puxador de 24 dp, reduzindo o vazio superior. Ações de fechar/compartilhar nas informações da HQ têm alvos de pelo menos 44 dp.
- **Atualizações:** diálogo com marca, hierarquia de título/conteúdo, superfície do tema e ações de largura total com alvo mínimo de 48 dp. Download, verificação e instalador Android continuam internos ao fluxo do aplicativo.
- **Ícone:** marca ampliada e aliases de launcher com recursos claros/escuros distintos, evitando depender apenas de atualização de cores de um mesmo recurso em cache. Sincroniza na inicialização, retorno ao app e alterações de configuração; uma tarefa periódica faz uma nova tentativa em segundo plano. A execução periódica pode ser adiada pelo Android e temas/pacotes de ícones do launcher podem substituir a aparência.

## Now Bar / Live Update

A opção **Leitura na Now Bar** fica em **Ajustes → Biblioteca e Exibição**. Ela é salva com as preferências da conta e inicia automaticamente o acompanhamento da HQ selecionada, após permissão de notificações. A tela inicial não contém um botão de ativação. Desativar a opção remove a notificação. Encerrar ou dispensar a notificação suspende aquela HQ até escolher outra ou desligar/ligar a preferência.

A seleção acompanha `activeReadingComicId`, a mesma usada pela iluminação de fundo e pelos widgets. A notificação fornece capa, página, percentual e controles −1/+1/Encerrar. No Android 16 ou superior, usa `NotificationCompat.ProgressStyle`, com a capa como marcador de progresso. Em versões anteriores mantém o template padrão. O canal permite conteúdo na tela bloqueada; as escolhas de privacidade do usuário têm precedência. Troca de conta/logout invalida a sessão e as identidades dos PendingIntents incluem conta e HQ, impedindo que controles antigos sejam redirecionados para outra leitura. Carga de capa e publicação executam fora da thread de interface.

O manifesto solicita `POST_PROMOTED_NOTIFICATIONS`; a notificação usa `setRequestPromotedOngoing(true)`, é contínua e usa template padrão, sem `RemoteViews` customizado ou sessão de mídia artificial. Samsung/Android decidem a elegibilidade e a promoção. O estilo exato da Now Bar, tela bloqueada e AOD é controlado pela One UI: o app fornece os mesmos dados e a capa dos widgets, não consegue impor seu layout. A integração foi direcionada ao Android 17/One UI 9 informado pelo usuário; a promoção em um Samsung real precisa ser verificada nesse aparelho.

O exemplo do Spotify é um cartão de reprodução: usa MediaSession/MediaStyle e controles de áudio. Não foi encontrada nas fontes públicas consultadas da Samsung uma API que permita impor esse cartão a uma atividade de leitura. A implementação não simula áudio para obter uma aparência de player. Por isso, a reprodução exata do cartão do Spotify permanece fora do que pode ser garantido por esta integração.

No Samsung, verificar **Configurações → Tela de bloqueio e AOD → Now Bar → Notificações ao vivo**, além das notificações e do conteúdo na tela bloqueada para o Quadrinheiro. A preferência do app inclui um atalho para a autorização de notificações promovidas (`Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS`) quando necessário; após autorizada, abre as configurações de notificações do aplicativo. A promoção final depende da elegibilidade definida pela One UI.

O repositório NowBar de styropyr0 é uma recriação visual em Compose, não uma API de integração ao sistema; seu código não foi incorporado.

## Ajustes adicionais de apresentação

- Ações Lido/Lendo/Quero Ler: cápsula visual de cerca de 30 dp e área de toque independente de 48 dp, rótulos de 13 sp e preenchimento menor. A quebra de linha continua disponível para fontes ampliadas.
- Compartilhamento: os seletores usam uma camada gravada da própria superfície do painel, sem amostrar a tela anterior. O atalho grande de leitura concluída foi removido do editor; a ação no cabeçalho continua disponível.
- Boas-vindas: cada etapa mede apenas o conteúdo visível. Logo, título e botão participam do mesmo fluxo; o tamanho da etapa é animado, sem deslocamentos fixos que dependam da altura de conteúdo oculto.

## Validação

Testes cobrem contraste dos temas claros/escuros e preferência de contraste, mudança de alias, sessão explícita/encerramento/seleção/troca de conta, ações do catálogo em largura de 300 dp e fonte ampliada, diálogo de atualização e abertura/seleção/reabertura do picker. A suíte anterior cobre arrasto para fora do menu, controles restaurados, toggle durante o gesto, skeleton baseado no layout real e rolagem/salvamento do editor. Não há medição de desempenho no Samsung do usuário; o emulador valida funcionamento, não garante uma taxa de quadros no dispositivo.

## Referências

- Skill local de design instalada pelo usuário em `D:\Codex\skills\apple-design\SKILL.md` (não incluída neste repositório).
- [Apple HIG: Getting started](https://developer.apple.com/design/human-interface-guidelines/getting-started)
- [Typography](https://developer.apple.com/design/human-interface-guidelines/typography), [Color](https://developer.apple.com/design/human-interface-guidelines/color), [Materials](https://developer.apple.com/design/human-interface-guidelines/materials), [Accessibility](https://developer.apple.com/design/human-interface-guidelines/accessibility)
- [Motion](https://developer.apple.com/design/human-interface-guidelines/motion), [Buttons](https://developer.apple.com/design/human-interface-guidelines/buttons), [Menus](https://developer.apple.com/design/human-interface-guidelines/menus), [Sheets](https://developer.apple.com/design/human-interface-guidelines/sheets), [SF Symbols](https://developer.apple.com/design/human-interface-guidelines/sf-symbols)
- [Adopting Liquid Glass](https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass)
- [Android Live Updates](https://developer.android.com/develop/ui/views/notifications/live-update)
- [Samsung Now Bar](https://www.samsung.com/africa_pt/support/mobile-devices/how-to-use-the-now-bar-on-the-lock-screen-of-your-samsung-galaxy-device/)
- [NowBar: recriação visual](https://github.com/styropyr0/NowBar)
- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) e [LiquidGlassKMP](https://github.com/philipplackner/LiquidGlassKMP)

- [Samsung: configuração e permissões da Now Bar](https://www.samsung.com/my/support/mobile-devices/how-to-use-the-now-bar-on-the-lock-screen-of-your-samsung-galaxy-device/)
- [Android: controles de mídia](https://developer.android.com/media/implement/surfaces/mobile)
- [Android: ProgressStyle](https://developer.android.com/reference/androidx/core/app/NotificationCompat.ProgressStyle)
- [Android: tela de autorização de promoção](https://developer.android.com/reference/android/provider/Settings#ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS)

## Revisão de continuidade, diário e armazenamento

- **Conteúdo e materiais:** cartões de conteúdo usam o material compartilhado com menos brilho e bordas discretas; os efeitos ópticos mais fortes ficam nos controles. Ajustes têm títulos de 16 sp e descrições de 13 sp com até duas linhas. Cores continuam semânticas e acompanham contraste/transparência.
- **Boas-vindas:** resumo compacto em uma página, com apresentação adaptada à altura e ao tamanho da fonte. O botão não depende de rolagem.
- **Diário:** resumo de leituras/avaliações e registros cronológicos; metas ficam na tela inicial. Removidos os blocos grandes de compartilhar resenha e meta. Registros sem data não recebem uma data inventada.
- **Folhas e compartilhamento:** alvos de 44 dp alinhados, símbolo de compartilhar sem cápsula redundante, salvar com símbolo de confirmação e exclusão no conteúdo. A lista é recortada abaixo do cabeçalho. Compartilhamento expõe somente a composição vertical, sem formato nem dimensões no cabeçalho.
- **Continuidade:** navegação hierárquica entra e sai por caminhos simétricos; troca de abas usa dissolução breve. Movimento reduzido elimina deslocamentos. A geometria dos segmentos e da tab bar permanece preservada. Menus usam refração e molas na camada gráfica, sem redimensionar os textos a cada quadro.
- **Widgets:** cores de recursos com variantes claras/escuras acompanham o sistema; atualização assíncrona retém o último conteúdo da mesma conta e impede exibir o de outra conta. Primeiro carregamento tem estado explícito, e falhas temporárias de capa permitem nova tentativa.
- **Armazenamento:** limites de 96 MiB para capas e 32 MiB para HTTP; traduções limitadas a 1.024 entradas/4 MiB. Limpeza restrita a APKs instalados/temporários vencidos e cartões exportados antigos (até oito/32 MiB/três dias). Biblioteca, perfis, registros e backups não são removidos. Não foi medido o armazenamento no aparelho do usuário; cache não é todo o dado ocupado pelo aplicativo.
- **Catálogo:** encadernados de obras independentes conhecidas, como Batman: Ano Um, não herdam uma edição mensal fictícia; fascículos e volumes numerados continuam preservados.

Referências desta revisão: [materiais](https://developer.apple.com/design/human-interface-guidelines/materials), [movimento](https://developer.apple.com/design/human-interface-guidelines/motion), [folhas](https://developer.apple.com/design/human-interface-guidelines/sheets), [barras de ferramentas](https://developer.apple.com/design/human-interface-guidelines/toolbars), [compartilhamento](https://developer.apple.com/design/human-interface-guidelines/collaboration-and-sharing), [Glance e recursos de aparência](https://developer.android.com/develop/ui/compose/glance/build-ui).
