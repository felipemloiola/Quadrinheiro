# Planos de Melhoria de Animações — Quadrinheiro

Auditoria conduzida segundo as habilidades `.claude/skills/quadrinheiro-animation-performance`, `.claude/skills/quadrinheiro-review-animations` e `.claude/skills/quadrinheiro-improve-animations` (filosofia de Emil Kowalski adaptada ao Jetpack Compose).

## Tabela de Achados Priorizados (Impacto ÷ Esforço)

| # | Severidade | Categoria | Localização | Achado | Resumo da Correção |
| --- | --- | --- | --- | --- | --- |
| 1 | **HIGH** | Acessibilidade & Interruptibilidade | `MainActivity.kt:918,1215,1261,1267,1587-1735`, `CatalogModalSubcomponents.kt:887`, `SettingsBackButton.kt:31` | 20+ chamadas diretas a `spring()`, `tween()`, `fadeIn()` e `expandVertically()` ignoram `LocalAppMotion.current`, mantendo animações ativas mesmo com `reduceMotion = true`. | Encaminhar todas as especificações por `motion.spring(...)` e `motion.tween(...)` com tempos assimétricos de entrada/saída. |
| 2 | **HIGH** | Performance (Layout vs GPU) | `CatalogModal.kt:311-335` | `val targetMaxHeight by animateDpAsState(if (isSheetExpanded) 860.dp else 650.dp)` força re-medição e re-layout da árvore inteira do `CatalogSheetModal` a cada frame ao recolher, embora `FluidSheetState` já anime a folha na GPU via `graphicsLayer { translationY = sheet.offset }`. | Remover `animateDpAsState` de altura máxima em `CatalogModal.kt` e usar o limite estático `650.dp` no modo compacto, deixando o movimento 100% em `graphicsLayer`. |
| 3 | **MEDIUM** | Easing, Duração (< 300ms) & Tokens | `AppMotion.kt:18-34`, `MainActivity.kt:502,534`, `AuthSubcomponents.kt:137` | `AppMotion.tween` usa `FastOutSlowInEasing` (que possui aceleração inicial lenta `0.4f`) e entradas em `MainActivity.kt:502,534` usam `450ms` (> 300ms). | Adicionar curvas `EaseOutStrong = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)` e `EaseOutQuint = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)` ao `AppMotion`, padronizar `EaseOutStrong` como curva padrão e reduzir entradas de `450ms` para `240ms`. |
| 4 | **MEDIUM** | Fisicalidade & Assimetria | `MainActivity.kt:918-921` | O Toast flutuante usa `fadeIn() + slideInVertically` simétrico sem escala inicial (`0.96f`) e sem curva `motion`. | Aplicar entrada `fadeIn(motion.tween(200)) + slideInVertically(motion.tween(220)) { it / 2 } + scaleIn(initialScale = 0.96f, animationSpec = motion.spring(0.85f, 420f))` e saída rápida `140ms–160ms`. |

## Ordem de Execução e Status

| Plano | Título | Dependências | Status |
| --- | --- | --- | --- |
| [001-app-motion-tokens-and-reduced-motion.md](001-app-motion-tokens-and-reduced-motion.md) | Tokens `EaseOutStrong`, duração sub-300ms e conformidade `LocalAppMotion` | Nenhuma | DONE |
| [002-gpu-composite-sheet-and-toast-transitions.md](002-gpu-composite-sheet-and-toast-transitions.md) | Eliminação de re-layout por frame no `CatalogSheetModal` e fisicalidade do Toast | 001 | DONE |
