# Guia de Design Engineering, Motion & Liquid Glass (Android / Jetpack Compose)

Este documento adapta ao ecossistema **Android (Kotlin + Jetpack Compose + Backdrop GPU)** os princípios de engenharia de interface, física de molas, gestos 1:1, performance 120Hz e Apple HIG iOS 26 (derivados das skills *Animate / Recipes*, *Animation Performance*, *Mobile Native*, *Gestures*, *Design Engineering* e *Apple Design*).

---

## 1. Frequency Gate — Quando Animar (e Quando NÃO Animar)

Nem toda transição deve ser animada. A regra de ouro é avaliar a **frequência de uso**:

| Frequência | Exemplos no App | Regra |
|---|---|---|
| **Alta (dezenas/centenas de vezes por sessão)** | Troca direta de abas na barra inferior, digitação, seleção rápida em filtros | **Zero fade lento ou delay.** Troca imediata ou micro-feedback ≤ 120 ms (`AppPageTransition` usa troca direta quando `!nextSlide`). |
| **Média (abertura de contexto)** | Abrir detalhes de uma HQ, abrir sheets (`CurationSheet`, `CatalogSheetModal`), abrir menu flutuante (`RetainedGlassMenu`) | **Mola física iOS 26 (`AppMotion.ios26*Spring()`).** Desaceleração orgânica, interruptível e com resposta imediata. |
| **Baixa / Momento-chave** | Compartilhar conquista, salvar meta, editar banner | **Coreografia expressiva com Liquid Glass**, profundidade de fundo recuando e feedback háptico semântico. |

---

## 2. Física Global iOS 26 (`AppMotion`)

Todas as animações de entrada, saída e gestos devem passar por `LocalAppMotion.current` (`com.example.ui.theme.AppMotion`). **Nunca** use curvas `LinearEasing` ou `EaseIn` em elementos de interface — movimentos de UI devem começar rápidos (resposta instantânea) e desacelerar organicamente.

### Presets Oficiais de Molas (`AppMotion`)

| Função | Parâmetros (`dampingRatio`, `stiffness`) | Uso Recomendado |
|---|---|---|
| `motion.ios26ModalSpring()` | `0.86f`, `380f` | Entrada orgânica de modais, diálogos (`LiquidGlassDialog`), sheets (`CurationSheet`, `FluidSheetState`) e expansão de painéis. |
| `motion.ios26ModalExitSpring()` | `0.94f`, `560f` | Saída mais rápida e firme que a entrada — a saída nunca deve parecer "arrastada". |
| `motion.ios26DetailZoomSpring()` | `0.92f`, `380f` | Transição Liquid Glass da lista de HQs para `ComicDetailScreen` (zoom suave `0.92f → 1f` + fundo recuando `1.0f → 0.94f`). |
| `motion.ios26JellyPressSpring()` | `0.56f`, `390f` | Efeito elástico ("gelatina" equilibrada estilo iOS 26 / Tab Bar) exclusivo dos botões Liquid Glass (`LiquidGlassPillButton`, `LiquidGlassCircleButton`, `SettingsBackButton`). |
| `motion.ios26FloatingMenuSpring(axis)` | Eixo X: `0.56f / 430f`, Eixo Y: `0.60f / 390f` | Abertura assimétrica elástica dos mini menus flutuantes (`RetainedGlassMenu`) inspirada no `LiquidGlassKMP`. |
| `motion.ios26FloatingMenuCollapseSpring()` | `0.88f`, `720f` | Recolhimento rápido de menus flutuantes. |
| `motion.ios26DragReleaseSpring(carriedMomentum)` | Com inércia: `0.82f / 440f`, Sem inércia: `1.0f / 480f` | Retorno de sheets/elementos após soltar o dedo — só permite leve overshoot quando há velocidade real carregada pelo gesto. |
| `motion.pressFeedbackTween()` | `120 ms`, `EaseOutQuint` | Feedback tátil instantâneo em cards de lista, grade e dashboard (`Modifier.tactileCardPress`), sem bounce. |

### Curvas Customizadas (`CubicBezierEasing`)

- **`AppMotion.EaseOutStrong`** `CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)` — Entradas e transições curtas padrão (`UI_ENTER_MS = 220`, `UI_EXIT_MS = 150`).
- **`AppMotion.EaseOutQuint`** `CubicBezierEasing(0.23f, 1.0f, 0.32f, 1.0f)` — Feedback de toque imediato (`PRESS_FEEDBACK_MS = 120`).
- **`AppMotion.EaseInOutStrong`** `CubicBezierEasing(0.77f, 0.0f, 0.175f, 1.0f)` — Morfismos de elementos já presentes na tela.
- **`AppMotion.EaseSheet`** `CubicBezierEasing(0.32f, 0.72f, 0.0f, 1.0f)` — Curva clássica de sheets do iOS.

> **Acessibilidade (`reducedMotion`):** Quando o sistema ou o usuário ativa *Reduzir Movimento*, todos os métodos `spring()` colapsam automaticamente para `snap()` e `tween()` colapsa para duração `0 ms`, mantendo o estado final instantaneamente sem atraso.

---

## 3. Equilíbrio Tátil: Cards vs. Botões Liquid Glass

Seguindo o equilíbrio adotado no app:

1. **Cards de Lista, Grade e Painel Dashboard (`Modifier.tactileCardPress`)**:
   - Escala imediata para `0.97f` no *pointer-down* em `120 ms` (`EaseOutQuint`), aplicada **exclusivamente em `graphicsLayer`**.
   - Zero bounce lento e zero relayout, mantendo 120fps em `LazyColumn` / grades densas.
2. **Botões Liquid Glass (`LiquidGlassPillButton`, `LiquidGlassCircleButton`, `SettingsBackButton`)**:
   - Utilizam `Modifier.iosLiquidGlassInteractiveSurface` com `motion.ios26JellyPressSpring()` e alongamento direcional suave no `layerBlock` da lente GPU, replicando a física tátil da Tab Bar iOS 26.

---

## 4. Gestos 1:1, Projeção de Momento & Resistência Elástica

Gestos nativos devem responder **pixel por pixel (1:1)** durante o arraste, sem atrasos, e herdar a velocidade real do dedo ao soltar:

- **Projeção de Momento (`AppMotion.projectMomentum`)**:
  Baseada na fórmula WWDC da Apple (*Designing Fluid Interfaces*):
  $$\text{projected} = x + \frac{v / 1000 \cdot d}{1 - d}$$
  com `decelerationRate = 0.99f` (normal) ou `0.998f` (scroll longo). Decide o destino final (snap vs. dismiss) pela **trajetória projetada**, não apenas pela posição estática.
- **Resistência Rubberband (`AppMotion.rubberband`)**:
  $$\text{rubber} = \frac{c \cdot |x| \cdot D}{D + c \cdot |x|} \quad (c = 0.55)$$
  Evita "paredes duras" quando o usuário puxa um sheet além do topo (`CurationSheets`, `FluidSheetState`).
- **Interruptibilidade & Velocity Handoff**:
  Arrastar novamente durante uma animação de retorno cancela o `Animatable` em andamento e continua a partir do ponto atual; ao soltar, a velocidade do `VelocityTracker` é passada diretamente para `animateTo(..., initialVelocity = velocity)`.

---

## 5. Feedback Háptico Causal (`AppHapticsController`)

Haptics devem ser **causais** (ligados a eventos físicos claros), nunca decorativos ou repetitivos em scroll livre. Use `val haptics = rememberAppHaptics()`:

| Método | Constante Android | Quando Disparar |
|---|---|---|
| `haptics.selection()` | `CLOCK_TICK` / `SEGMENT_TICK` | Troca de item em foco no `RetainedGlassMenu`, troca de mês nos mini-anéis, incrementos de stepper (`+` / `-`). |
| `haptics.impactLight()` | `VIRTUAL_KEY` / `KEYBOARD_TAP` | Toque em botões Liquid Glass, seleção de item em menu, toque em cards de resumo, voltar. |
| `haptics.impactMedium()` | `CONTEXT_CLICK` | Ações destrutivas (`destructive = true`) ou encaixe em detent/snap de sheet. |
| `haptics.success()` | `CONFIRM` | Confirmação de salvamento / conclusão de tarefa importante. |
| `haptics.error()` | `REJECT` | Rejeição de validação ou erro de ação. |

---

## 6. Performance 120Hz (Orçamento de 8 ms por Frame)

Para manter quadros constantes em telas de 90Hz/120Hz:

1. **Anime apenas na fase de composição/desenho (`graphicsLayer`)**:
   Use `scaleX`, `scaleY`, `translationX`, `translationY`, `alpha`, `shape`/`clip` dentro de `Modifier.graphicsLayer { ... }`. **Nunca** anime `Modifier.padding`, `width`, `height` ou `offset(Dp)` em loops contínuos de gesto ou scroll.
2. **Leitura Diferida de Estado (`RootDepthHost`)**:
   O recuo de profundidade do fundo raiz (`MainActivity`) lê `rootDepthHost.depth` **exclusivamente dentro do bloco `graphicsLayer`**. Assim, abrir `ComicDetailDialog` ou arrastar um `CatalogSheetModal` anima o recuo da tela principal (`scale 1.0 → 0.94`, `ContinuousRoundedRectangle(28.dp)`) com **zero recomposições** da árvore de conteúdo.
3. **Congelamento Inteligente de Backdrop e Fling Extremo**:
   Durante `isExtremeFling`, `liquidGlassPanelSurface` e `iosLiquidGlassInteractiveSurface` desativam a refração de lente pesada / caem para superfície translúcida leve para evitar *jank* em rolagens rápidas, restaurando o vidro óptico completo assim que a lista desacelera.

---

## 7. Hierarquia de Materiais Liquid Glass (Android GPU vs. CSS)

Equivalente nativo no Android ao `backdrop-filter: blur(24px) saturate(150%)` com física óptica real via `drawBackdrop`:

| Camada | Modifier / Componente | Pipeline GPU | Opacidade da Base |
|---|---|---|---|
| **Botões & Topbar / Tab Bar** | `iosLiquidGlassInteractiveSurface` (`LiquidGlassPillButton`, `LiquidGlassCircleButton`, `SettingsBackButton`) | `vibrancy()` + `blur(8..20dp)` + `lens(depthEffect = true)` + `Highlight` + `InnerShadow` + `drawOpticalGlassMeniscus` | `0.26f – 0.30f` (claro/escuro) |
| **Painéis & Cards de Dashboard** | `liquidGlassPanelSurface` (`Home`, `ReadingStatisticsScreen`, `HomeReadingNowComicCard`) | `vibrancy()` + `blur(16..24dp)` + `lens(sutil)` + `adaptiveGlassHighlight` + menisco suave | `0.36f – 0.42f` |
| **Modais, Sheets & Menus Flutuantes** | `LiquidGlassDialog`, `CurationSheet`, `RetainedGlassMenu` (via `LiquidGlassOverlayPortal`) | `vibrancy()` + `blur(18..26dp)` + `lens` sobre `liveBackdrop` real da janela principal | `0.44f – 0.56f` |

> **Regra de Ouro da Refração:** Todo elemento Liquid Glass precisa estar ancorado a um `Backdrop` vivo (`mainContentBackdrop`, `backgroundBackdrop` ou `statsBackdrop` local) e usar uma base de cor com opacidade reduzida — um fundo opaco anula o desfoque dinâmico. Quando `AppSettings.reduceTransparency` estiver ativo, todas as camadas fazem fallback automático para superfícies sólidas de alto contraste.

---

## 8. Transição Lista de HQs ↔ Detalhes (`ComicDetailDialog` + `RootDepthHost`)

- **Abertura:** `ComicDetailDialog` materializa em zoom Liquid Glass (`scale 0.92f → 1.0f`, `ContinuousRoundedRectangle(32.dp → 0.dp)` com borda luminosa efêmera durante o voo) usando `motion.ios26DetailZoomSpring()`.
- **Profundidade do Fundo:** Simultaneamente, `LocalRootDepthHost` recebe `presentationProgress * (1f - predictiveExitProgress)`, recuando a tela principal para `0.94f` com cantos contínuos de `28.dp` sobre fundo preto.
- **Predictive Back 1:1:** O gesto de voltar do Android controla `predictiveExitProgress` em tempo real, restaurando o fundo principal à escala `1.0f` proporcionalmente ao arraste do dedo.

---

## 9. Checklist de Revisão (Antes de Publicar Novas Telas)

- [ ] **Alvos de toque ≥ 48.dp** em todos os controles interativos primários, com `contentDescription` e `Modifier.testTag(...)` em `snake_case`.
- [ ] **Contraste legível ≥ 4.5:1** (`readableForeground`) sobre qualquer superfície translúcida em modo claro e escuro.
- [ ] **Zero curvas lineares ou `ease-in`** em entradas/saídas; todas as molas vêm de `LocalAppMotion.current`.
- [ ] **Animações em `graphicsLayer`** — nenhum `animateDpAsState` dirigindo gestos 1:1.
- [ ] **Fallbacks respeitados**: `reduceMotion` desliga deslocamento/molas; `reduceTransparency` troca blur/lens por superfícies sólidas limpas.
- [ ] **Tipografia tabular (`SpaceGroteskTabularFamily`)** em contadores, porcentagens e métricas numéricas para evitar trepidação horizontal durante atualizações.
