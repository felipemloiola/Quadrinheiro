# Revisão de design: listas e ficha da HQ

Revisão de 16/11/2026: escolha/recorte da capa, detalhe/editor/adição de listas, menus, ranking, recomendações explicáveis e ficha/expansão da capa. Android Compose, minSdk 24. Aplicadas as skills locais de qualidade, revisão visual, vidro e desempenho/revisão de animações; este arquivo também registra o plano da revisão de movimento.

Referências estéticas: [Layout](https://developer.apple.com/design/human-interface-guidelines/layout), [Materials](https://developer.apple.com/design/human-interface-guidelines/materials), [Accessibility](https://developer.apple.com/design/human-interface-guidelines/accessibility) e [Pop-up buttons](https://developer.apple.com/design/human-interface-guidelines/pop-up-buttons). Controles e semântica seguem Android; não é certificação de conformidade com iOS.

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

Rodada final: os dois testes do grid corrigidos, o novo teste de enquadramento sem gestos e os dois da ficha/zoom passaram (5/5). São 21 cenários distintos cobertos entre as rodadas, sem falhas pendentes. Capturas do tema claro com fonte 160% e do tema escuro foram geradas. Logs locais: design-review-build.log, design-review-catalog.log, design-review-native.log, design-review-verified-build.log e design-review-final-native.log. APK instalado preservando dados; revisão da lista real não aplica nem salva alterações.

A captura com capas reais revelou que o check ao lado do texto reduzia a largura útil do título. O marcador foi movido para o canto da miniatura. Os dois cenários do grid foram repetidos e aprovados, incluindo uma nova asserção de ausência de overflow no título/número da edição em fonte normal. Logs: design-review-card-title-build.log e design-review-card-title-native.log.

Uma asserção adicional de escala efetiva revelou que a configuração 160% do teste não chegava ao texto dentro do Dialog (era 100%). As verificações anteriores de geometria permanecem válidas em fonte normal, mas não comprovavam fonte ampliada. O painel passou a preservar a LocalDensity do chamador; a validação final deve incluir a asserção de escala interna antes de considerar fonte 160% aprovada.

Validação definitiva da escala: build design-review-scaled-panels-build.log aprovado; os 7 cenários de CurationDesignReviewTest e CurationMenuMotionTest passaram em design-review-scaled-panels-native.log. A asserção confirmou fontScale 1,6 no TextLayoutResult, cartões uniformes e ausência de overflow do título em fonte normal. A captura clara com fonte ampliada foi inspecionada; o conteúdo continua rolável. Não houve alteração nas preferências de fonte do sistema ou da conta: o teste injeta a densidade apenas na composição, e a correção respeita a densidade já fornecida pelo tema.

Sem certificação física de CPU/GPU, p95/p99, temperatura e bateria. A matriz visual de tablets e APIs 24/25/31/33+ não está integralmente executada; contratos unitários não substituem esses ensaios. Não publicar código ou APK nesta entrega.
