# Plano 002 — Eliminação de Re-layout por Frame no `CatalogSheetModal` e Fisicalidade do Toast

## Contexto e Causa
1. Em `app/src/main/java/com/example/ui/components/CatalogModal.kt:311-335`, `targetMaxHeight` usava `animateDpAsState(if (isSheetExpanded) 860.dp else 650.dp)`. Como o modificador alterna imediatamente para `fillMaxHeight(0.96f)` quando expandido, o `animateDpAsState` só atuava ao recolher, forçando re-medição e re-layout de toda a árvore do modal a cada frame, competindo com `sheet.offset` no `graphicsLayer`.
2. Em `app/src/main/java/com/example/MainActivity.kt:918-921`, o Toast flutuante usava `fadeIn() + slideInVertically { it / 2 }` sem `scaleIn(initialScale = 0.96f)` e sem `motion.tween` / `motion.spring` assimétrico.

## Especificação Exata
- Em `CatalogModal.kt`:
  - Remover `val targetMaxHeight by animateDpAsState(...)` e aplicar `.heightIn(max = 650.dp)` diretamente no estado não expandido, mantendo toda a animação espacial na GPU via `graphicsLayer { translationY = sheet.offset }`.
- Em `MainActivity.kt`:
  - Configurar o `AnimatedVisibility` do Toast com:
    - `enter = fadeIn(motion.tween(200)) + slideInVertically(motion.tween(220)) { it / 2 } + scaleIn(initialScale = 0.96f, animationSpec = motion.spring(0.85f, 420f))`
    - `exit = fadeOut(motion.tween(140)) + slideOutVertically(motion.tween(160)) { it / 2 } + scaleOut(targetScale = 0.96f, animationSpec = motion.tween(140))`

## Verificação
- Executar `gradle :app:testDebugUnitTest --tests com.example.FluidMotionPolicyTest`.
