---
name: quadrinheiro-improve-animations
description: Auditar o código de animação e movimento do Quadrinheiro como consultor sênior de motion e produzir planos autocontidos em plans/ antes ou junto à execução controlada.
---

# Auditoria e Melhoria de Animações — Quadrinheiro

**Referência:** Metodologia `improve-animations` baseada na filosofia de Emil Kowalski, adaptada ao ecossistema Android/Compose do Quadrinheiro.

## Fluxo de Trabalho

1. **Fase 1 — Reconhecimento:**
   - Mapear onde o movimento vive: `AppMotion.kt`, `AppPageTransition.kt`, `FluidSheetState`, `RetainedGlassMenu.kt`, `LiquidGlassComponents.kt`, `MainActivity.kt` e telas em `ui/screens/`.
   - Identificar convenções existentes (`LocalAppMotion`, molas amortecidas, `graphicsLayer`) para estendê-las sem criar sistemas paralelos.
2. **Fase 2 — Auditoria nas 8 Categorias:**
   - Propósito e frequência; Easing e duração (< 300ms); Fisicalidade e origem (`TransformOrigin`, escala inicial `>= 0.94f`); Interruptibilidade; Performance (ausência de re-layout por frame); Acessibilidade (`LocalAppMotion.reduced`); Coesão e tokens; Oportunidades perdidas.
3. **Fase 3 — Priorização por Alavancagem (Impacto ÷ Esforço):**
   - Classificar em `HIGH` (quebra de fluidez, re-layout por frame, `reduceMotion` ignorado), `MEDIUM` (assimetria entrada/saída, duração > 300ms em UI frequente) e `LOW` (refinamento de curvas e tokens).
4. **Fase 4 — Planos Autocontidos em `plans/`:**
   - Documentar planos numerados (`plans/NNN-slug.md`) e índice em `plans/README.md` com caminhos exatos, trechos de código, valores de curvas/molas e critérios de verificação automatizada (`gradle :app:testDebugUnitTest`).
