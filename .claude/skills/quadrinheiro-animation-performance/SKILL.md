---
name: quadrinheiro-animation-performance
description: Manter animações Compose dentro do orçamento de quadro (8ms em 120Hz / 16.7ms em 60Hz) — animar propriedades exclusivamente na fase de composição/desenho (graphicsLayer), evitar recomposição e re-layout por frame, limitar raio de blur animado e diagnosticar quedas de frame. Adaptado da filosofia de engenharia de design de Emil Kowalski (animation-performance) para o Quadrinheiro Android.
---

# Desempenho de Animações em Compose — Quadrinheiro

**Referência:** Filosofia de engenharia de design de Emil Kowalski (`animation-performance`), adaptada para a pipeline do Android Jetpack Compose (`Composition` → `Layout` → `Drawing` / `RenderNode`).

## Regras Inegociáveis

1. **`Modifier.graphicsLayer { ... }` por padrão (`translationX`, `translationY`, `scaleX`, `scaleY`, `alpha`).** Qualquer animação fora do `graphicsLayer` exige justificativa técnica. Evite animar `width`, `height`, `padding`, `offset(Dp)` ou `animateDpAsState` quando uma transformação em `graphicsLayer` ou recorte produz o mesmo resultado visual sem disparar a fase de `Layout`.
2. **Nomeie a causa antes de alterar o código.** Toda correção deve identificar em qual fase do Compose o custo ocorre (`Composition`, `Layout` ou `Drawing`/AGSL).
3. **Orçamento de 8ms (120Hz).** Projete para telas de 120Hz em aparelhos intermediários, onde um passe de `Layout` em árvore profunda estoura vários quadros consecutivos.
4. **Leitura diferida de estado (Deferred State Read).** Nunca leia estados que mudam a cada frame diretamente no corpo de um `@Composable` se o valor puder ser lido dentro de lambdas de fase tardia (`Modifier.graphicsLayer { ... }`, `Modifier.offset { IntOffset(...) }`, `Modifier.drawBehind { ... }`).
5. **Limite de Blur Animado.** Mantenha raios de desfoque (`blur()`) animados abaixo de `20.dp` e desative efeitos caros de lente/aberração cromática durante rolagem ativa (`isScrolling`) ou quando `reduceTransparency` estiver ativo.

## Tabela de Sintomas e Soluções (Android Compose)

| Sintoma | Causa na Pipeline Compose | Solução |
| --- | --- | --- |
| Animação engasga em listas ou modais | `animateDpAsState` alterando `height`/`padding` a cada frame | Substituir por `Modifier.graphicsLayer { translationY = ...; scaleX = ...; scaleY = ... }` |
| Recomposição por frame durante gesto/arrasto | Leitura de `animatable.value` no escopo `@Composable` | Ler `animatable.value` apenas dentro de `graphicsLayer { ... }` ou `drawBehind { ... }` |
| Jank durante navegação entre abas/telas | Duas ou mais árvores pesadas medindo layout simultaneamente | Usar `AppPageTransition` com no máximo 2 páginas vivas e transformação via `graphicsLayer` |
| Queda de FPS ao abrir vidro com blur | Raio de `blur` excessivo ou múltiplas capturas de `Backdrop` | Manter `blur <= 20.dp` em transições dinâmicas e reutilizar uma única captura (`reuseRecordedContent = true`) |
| Movimento ignora acessibilidade | Uso direto de `spring()` / `tween()` do Compose | Usar sempre `LocalAppMotion.current.spring(...)` e `LocalAppMotion.current.tween(...)` |
