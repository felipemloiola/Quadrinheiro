---
name: quadrinheiro-review-animations
description: Revisar código de animação e movimento em Jetpack Compose contra um alto padrão de qualidade derivado da filosofia de Emil Kowalski (review-animations).
---

# Revisão de Animações — Quadrinheiro

**Referência:** Filosofia de animação de Emil Kowalski (`review-animations`), adaptada para Android Jetpack Compose e `LocalAppMotion`.

## Os Dez Padrões Inegociáveis

1. **Movimento justificado:** Toda animação deve responder "por que isso anima?" — consistência espacial, indicação de estado, feedback tátil ou prevenção de corte brusco.
2. **Adequado à frequência:** Ações repetidas dezenas de vezes por dia usam movimento mínimo ou instantâneo; transições ocasionais usam curvas padrão sub-300ms.
3. **Easing responsivo:** Elementos que entram ou saem usam `ease-out` forte (`AppMotion.EaseOutStrong = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)` ou `AppMotion.EaseOutQuint`). Nunca usar `EaseIn` em respostas de interface.
4. **UI abaixo de 300ms:** Transições de interface devem ficar abaixo de 300ms (tipicamente `180ms–260ms` na entrada e `120ms–180ms` na saída).
5. **Origem física correta (`TransformOrigin`):** Menus flutuantes e popovers (`RetainedGlassMenu`) escalam a partir do gatilho (`TransformOrigin` ancorado), nunca do centro ou de `scale(0f)` (iniciar de `0.94f–0.97f` + `alpha`).
6. **Interruptibilidade:** Movimentos acionados por gestos ou cliques rápidos (abas, menus, toasts, sheets) devem usar molas (`motion.spring`) ou `Animatable` que redirecionam a partir do estado atual sem reiniciar do zero.
7. **Propriedades restritas à GPU:** Animar `translationX`, `translationY`, `scaleX`, `scaleY` e `alpha` via `Modifier.graphicsLayer`. Evitar animar `Dp` de tamanho/margem em contêineres complexos.
8. **Acessibilidade (`LocalAppMotion.reduced`):** Todo `spring`, `tween`, `fadeIn`, `fadeOut`, `expandVertically` e `shrinkVertically` deve passar por `LocalAppMotion.current` para respeitar `reduceMotion`.
9. **Assimetria entrada/saída:** A saída/fechamento deve ser mais rápida e direta que a entrada (ex.: entrada `220ms`, saída `150ms`).
10. **Coesão com o Liquid Glass:** Preservar `AppMotion`, `FluidSheetState`, `RetainedGlassMenu` e a física elástica existente dos componentes de vidro.

## Formato de Saída na Revisão

1. **Tabela de achados (`Antes` | `Depois` | `Por quê`)** com referência exata a `arquivo:linha`.
2. **Veredito por faixa de impacto:** Regressões de sensação → Performance (Layout vs GPU) → Interruptibilidade e Assimétrica → Acessibilidade (`reduceMotion`).
