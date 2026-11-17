# Revisão de design: listas e ficha da HQ

Revisão de 16/11/2026: escolha/recorte da capa, detalhe/editor/adição de listas, menus, ranking, recomendações explicáveis e ficha/expansão da capa. Android Compose, minSdk 24. Aplicadas as skills locais de qualidade, revisão visual, vidro e desempenho/revisão de animações; este arquivo também registra o plano da revisão de movimento.

Referências estéticas: [Layout](https://developer.apple.com/design/human-interface-guidelines/layout), [Materials](https://developer.apple.com/design/human-interface-guidelines/materials), [Accessibility](https://developer.apple.com/design/human-interface-guidelines/accessibility) e [Pop-up buttons](https://developer.apple.com/design/human-interface-guidelines/pop-up-buttons). Controles e semântica seguem Android; não é certificação de conformidade com iOS.

**Estado:** correções implementadas, testes automatizados e fluxo visual de capas conferido com vidro ativo no emulador e, adicionalmente, nos componentes de listas no Galaxy S23. A sessão emulada apresentou ANRs no caminho gráfico antes da verificação final. Estabilidade prolongada e orçamento de 8ms/120Hz não estão certificados.

## Achados e correções

| Prioridade / arquivo | Antes | Depois | Por quê / evidência |
| --- | --- | --- | --- |
| HIGH — `ListPresentation.kt:162` | Título de uma linha reduzia a altura do cartão | Capas 2:3, larguras iguais, área de texto de duas linhas em todos | Dimensões uniformes sem altura fixa que corte fontes ampliadas. Testes com títulos curtos/longos em duas fileiras, claro/escuro e fonte 160%. |
| HIGH — `ListPresentation.kt:164` | Papel de rádio sem estado ou indicação da capa atual | Selectable, contorno, check sobre a miniatura e estado selecionado | Não depende só de cor; o marcador não toma largura do título/número da edição. Miniatura decorativa não repete o título na leitura assistida. |
| HIGH — `RetainedGlassMenu.kt:158` | Mover/Nota eram anunciados como rádios | Ações são botões; Privada/Pública continuam opções exclusivas | Modelo selectionMenu preservado ao atualizar âncora/rótulo; teste de papel, estado e execução. |
| MEDIUM — `RetainedGlassMenu.kt:81` | Linha de 46dp | Mínimo de 48dp, cresce com a fonte | Convenção de toque Android, preservando o renderizador do perfil. |
| MEDIUM — `EditorialComponents.kt:20` | Restavam ações OutlinedButton | CurationGlassAction compartilhado | Coerência entre listas, recomendações e rodapé da HQ; fallback opaco preservado. |
| HIGH — `CurationSheets.kt:153` | Rótulos longos limitados a uma linha | Até duas linhas centralizadas, reticências e alvo mínimo 48dp | Evita corte nas ações lado a lado sem fixar a altura. |
| MEDIUM — `CurationSheets.kt:98` | Captura para menu em todo painel | Captura/overlay apenas no detalhe/editor que têm menus | Seleção/recorte/adição/confirmações dispensam essa camada. Redução estrutural, sem afirmar ganho medido de GPU. |
| HIGH — `ListCoverCropDialog.kt:67` | Alternativa acessível só na vertical, direção anunciada invertida | Ações nas quatro direções com sentido da imagem, slider e Centralizar existentes | Enquadramento disponível sem arraste/pinça; teste executa subir/esquerda e confere o foco confirmado. |
| MEDIUM — `CurationSheets.kt:104` | Back adicional podia alcançar o pai durante a saída | Back consumido durante saída solicitada | Preserva navegação por nível e retenção do conteúdo. |
| HIGH — `CurationSheets.kt:93` | O novo owner do Dialog perdia a escala tipográfica recebida do app | LocalDensity do chamador preservada dentro do painel | O teste mede TextLayoutResult.layoutInput.density.fontScale; solicitar 160% fora do Dialog não bastava para provar a escala interna. |
| HIGH — `ComicArtwork.kt:121` | Ampliação disponível só por pinça | Ações acessíveis de ampliar/reduzir/redefinir e estado de ampliação | Mantém o gesto e a transição existente, sem criar mais controles visuais sobre a capa. |

## Revisão do movimento

- **Propósito e frequência:** apresentação/retorno identificam hierarquia; ranking abre espaço no arraste com chaves estáveis. Nenhuma animação decorativa contínua foi adicionada.
- **Layout versus GPU:** painéis, ranking, fade e parallax preservam transformações em camadas de renderização. Banner da HQ continua fora da LazyColumn, sem crossfade repetido; não há nova captura por miniatura.
- **Interruptibilidade, easing e assimetria:** mantidos AppMotion/FluidSheetState e molas existentes. A capa expandida entra em 240ms e sai em 150ms. O menu compartilhado mantém o morph a partir das dimensões do gatilho. Isso difere da escala inicial 0,94–0,97 para popovers da skill: não declarar essa regra validada. Mudar esse motor exige comparar com o seletor do perfil, referência aprovada pelo usuário.
- **Acessibilidade:** caminhos de movimento/transparência reduzidos preservados. Testes cobrem fonte ampliada no grid, papéis/estados, alternativa Mover e pinça. Não substituem sessão manual completa com TalkBack.
- **Materiais e coesão:** conteúdo usa material de conteúdo; vidro fica nos controles/navegação. Não alterar shader ou preset de refração sem benchmark.

## Verificação e limites

Executados 37 testes unitários (PersonalCurationTest, FluidMotionPolicyTest e LiquidGlassOpticsContractTest) e 24 testes do catálogo, todos aprovados. Build debug e APK de testes gerados.

Na rodada ampla de 20 cenários de interface, os 18 cenários de menus, navegação, seleção múltipla, ranking, notas, contas, registro e expansão passaram. Dois testes novos do grid falharam apenas na seleção porque seus exemplos não tinham imagem: coverItem ignora capa vazia. As verificações de igualdade das dimensões já passavam. Os exemplos foram corrigidos para usar um PNG local válido.

Rodada final: os dois testes do grid corrigidos, o novo teste de enquadramento sem gestos e os dois da ficha/zoom passaram (5/5). São 21 cenários distintos cobertos entre as rodadas, com testes aprovados após as correções descritas. A aprovação da escala efetiva de 160% ocorreu na rodada definitiva abaixo. Logs locais: design-review-build.log, design-review-catalog.log, design-review-native.log, design-review-verified-build.log e design-review-final-native.log. APK instalado preservando dados; revisão da lista real não aplica nem salva alterações.

A captura com capas reais revelou que o check ao lado do texto reduzia a largura útil do título. O marcador foi movido para o canto da miniatura. Os dois cenários do grid foram repetidos e aprovados, incluindo uma nova asserção de ausência de overflow no título/número da edição em fonte normal. Logs: design-review-card-title-build.log e design-review-card-title-native.log.

Uma asserção adicional de escala efetiva revelou que a configuração 160% do teste não chegava ao texto dentro do Dialog (era 100%). As verificações anteriores de geometria permanecem válidas em fonte normal, mas não comprovavam fonte ampliada. O painel passou a preservar a LocalDensity do chamador; a validação final deve incluir a asserção de escala interna antes de considerar fonte 160% aprovada.

Validação definitiva da escala: build design-review-scaled-panels-build.log aprovado; os 7 cenários de CurationDesignReviewTest e CurationMenuMotionTest passaram em design-review-scaled-panels-native.log. A asserção confirmou fontScale 1,6 no TextLayoutResult, cartões uniformes e ausência de overflow do título em fonte normal. A captura clara com fonte ampliada foi inspecionada; o conteúdo continua rolável. Não houve alteração nas preferências de fonte do sistema ou da conta: o teste injeta a densidade apenas na composição, e a correção respeita a densidade já fornecida pelo tema.

### Pendência gráfica encontrada no teste real

O fluxo real com transparência ativa apresentou ANR de foco/entrada. O relatório do próprio processo, obtido pela API pública ApplicationExitInfo, mostrou a thread principal aguardando DrawFrameTask e a RenderThread bloqueada em qemu_pipe_read → glGenTextures_enc → GrGLGpu::createTexture. Isso identifica o bloqueio no caminho gráfico emulado; a causa raiz ainda requer comparação em ambiente estável/hardware real. Os testes com redução de transparência não reproduziram esse cenário.

O daemon Gradle ocioso foi encerrado para liberar memória. Foi tentada uma inicialização com debug.hwui.renderer=skiavk, restaurando imediatamente skiagl; a mudança efetiva de pipeline não foi confirmada. O mesmo AVD foi reiniciado preservando seus dados, com backend software para comparação; esse modo também apresentou falhas de inicialização em outros apps do sistema. O modo gráfico automático foi restaurado. Nenhuma preferência gráfica foi adicionada ao produto, e o APK não força Vulkan nem desativa o vidro.

Após inicializar o AVD no modo automático e aguardar o boot, o fluxo real passou: biblioteca → detalhe da lista → editor → escolha de capa → retorno à biblioteca, sem salvar/aplicar alterações. Scripts design-review-restored-open.log e design-review-restored-visual.log terminaram sem erro; a captura final foi conferida e o processo permaneceu ativo. Os dados e o emulador foram preservados. Isso não prova correção definitiva do ANR observado sob a sessão anterior.

Foi oferecida e realizada a validação adicional no S23 descrita abaixo. A medição prolongada e a comparação de desempenho com/sem efeitos continuam pendentes.

Sem certificação física de CPU/GPU, p95/p99, temperatura e bateria. A matriz visual de tablets e APIs 24/25/31/33+ não está integralmente executada; contratos unitários não substituem esses ensaios. Não publicar código ou APK nesta entrega.

### Verificação adicional no Galaxy S23 (Android 17 / API 37)

A instalação de distribuição existente foi preservada: a chave de assinatura correspondente não está disponível neste ambiente. Foi criada uma instalação de revisão separada, com nome distinto e dados sintéticos, usando os mesmos componentes de listas e o motor de vidro do repositório. Nenhuma conta, biblioteca ou preferência do aplicativo original foi copiada ou alterada. O dispositivo permaneceu com suas configurações de animação; manter a tela ligada foi restrito à Activity de revisão.

No hardware, foram percorridos detalhe → menu de opções → editor → visibilidade → escolha → enquadramento nos temas claro e escuro, com transparência e movimento ativos. O slider alterou o zoom, o arraste moveu a arte e o gesto lateral de voltar retornou do recorte somente à escolha de capa. O retorno pelos demais níveis foi conferido sem aplicar/salvar alterações.

Essa comparação revelou duas correções adicionais no código compartilhado:

- O menu claro sobre arte escura perdia contraste. A superfície agora usa material regular com piso de opacidade para texto legível nos dois temas, respeitando o fallback opaco. O blur máximo fica em 20dp, sem nova captura nem animação contínua. Testes de pixels verificam contraste mínimo de 4,5:1 sobre fundos preto/branco na preferência de opacidade baixa.
- A largura lógica de 360dp do S23 fazia o título de referência ocupar três linhas. A tipografia do seletor passou a 14sp/20sp, preservando a área uniforme de duas linhas e a escala de fonte configurada. Títulos arbitrariamente longos continuam com reticências e nome completo na semântica.

As capturas, o harness e os APKs de revisão ficam somente em `.build-outputs/`, ignorados pelo Git. Esta validação cobre os componentes de listas isolados; não substitui o teste integral da distribuição, da ficha da HQ ou de sincronização com uma conta real. A coleta inicial de frames foi de um fluxo frio de debug sem baseline equivalente, portanto não fundamenta aprovação de fluidez/energia nem diagnóstico definitivo do emulador.

Build final aprovado em `s23-review-card-width-build.log`. No S23, os nove cenários de `CurationGlassContrastTest`, `CurationDesignReviewTest` e `CurationMenuMotionTest` passaram (`s23-review/final-native.log`), incluindo os extremos de contraste e fonte efetiva de 160%. A primeira rodada havia passado oito de nove e revelou o corte do título na largura de 360dp; a segunda confirmou a correção, sem remover ou enfraquecer a asserção de overflow.

A repetição visual final terminou sem erro nos scripts `s23-review/final-dark.log` e `s23-review/final-light.log`. Foram inspecionadas as capturas dos dois temas, cartões, visibilidade e botão Salvar em repouso. O processo de revisão permaneceu ativo, sem ANR observado nesse percurso. A instalação original manteve a versão e a data de atualização anteriores ao ensaio; o emulador continuou aberto.
