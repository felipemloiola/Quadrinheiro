# Plano 001 — Tokens `EaseOutStrong`, Duração Sub-300ms e Conformidade `LocalAppMotion`

## Contexto e Causa
1. `app/src/main/java/com/example/ui/theme/AppMotion.kt` utilizava `FastOutSlowInEasing` (`CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)`), cuja fase inicial lenta atrasa a resposta visual percebida pelo usuário.
2. Em `MainActivity.kt`, `SettingsBackButton.kt` e `CatalogModalSubcomponents.kt`, animações de visibilidade, aba inferior e botão de busca invocavam `spring()` / `tween()` / `fadeIn()` diretamente, ignorando `LocalAppMotion.current.reduced`.
3. As transições de entrada de `WelcomeIntroScreen` e `AuthScreen` em `MainActivity.kt:502,534` duravam `450ms`, acima do limite de `300ms` para UI.

## Especificação Exata
- Em `AppMotion.kt`:
  - Definir `EaseOutStrong = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)` e `EaseOutQuint = CubicBezierEasing(0.23f, 1.0f, 0.32f, 1.0f)`.
  - Definir constantes `UI_ENTER_MS = 220` e `UI_EXIT_MS = 150`.
  - Usar `EaseOutStrong` como `easing` padrão em `AppMotion.tween`.
- Em `MainActivity.kt`:
  - Reduzir `motion.tween(450)` nas linhas 502 e 534 para `motion.tween(240)`.
  - Em `IOS27BottomTabViewContainer`, ler `val motion = com.example.ui.theme.LocalAppMotion.current` e substituir todas as chamadas diretas a `spring(...)` e `tween(...)` por `motion.spring(...)` e `motion.tween(...)`.
- Em `SettingsBackButton.kt` e `CatalogModalSubcomponents.kt`:
  - Passar `motion.tween(...)` assimétrico em `fadeIn`, `fadeOut`, `expandVertically` e `shrinkVertically`.
- Em `AuthSubcomponents.kt`:
  - Usar `AppMotion.EaseOutStrong` e `220ms` em `animateContentSize`.

## Verificação
- Executar `gradle :app:testDebugUnitTest --tests com.example.FluidMotionPolicyTest`.
