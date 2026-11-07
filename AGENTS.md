# Orientações para agentes — Quadrinheiro

Este repositório é um aplicativo **Android nativo** em Kotlin, Jetpack Compose e Material 3, não um projeto SwiftUI/iOS. Consulte o `README.md`, `CONTEXTO-DO-APP.md` e `docs/INTEGRACAO-APPLE-SKILLS-PRISMAL.md` antes de alterar a arquitetura visual.

## Habilidades disponíveis neste projeto

- `.claude/skills/quadrinheiro-apple-quality/SKILL.md`: práticas de qualidade e entregas, adaptadas de **claude-code-apple-skills**.
- `.claude/skills/quadrinheiro-android-ui-review/SKILL.md`: revisão de design e acessibilidade inspirada em **apple-design-skill**.
- `.claude/skills/quadrinheiro-glass-performance/SKILL.md`: avaliação dos materiais e efeitos ópticos em Compose, com referências ao **Prismal**.

## Regras técnicas

1. Preservar `minSdk = 24`: nunca importar diretamente bibliotecas com `minSdk` superior sem plano de compatibilidade comprovado e testes.
2. Respeitar o motor de vidro existente (`com.kyant.backdrop`, `SdfShader` e `LocalAppMotion`). Não duplicar captura do fundo nem criar animação contínua por componente sem medição.
3. Tratar *HIG da Apple* como referência estética; preferir APIs e convenções de acessibilidade do Android/Compose. Não afirmar conformidade oficial com iOS.
4. Todo controle interativo precisa de rótulo/estado semântico, área de toque adequada, feedback e caminhos sem animação e sem transparência quando configurados.
5. Isolamento por conta, autenticação, Firestore e chaves de catálogo são invariantes de segurança. Não incluir chaves no APK, logs, commits ou testes.
6. Antes de propor regressões visuais, comparar uso de CPU/GPU, jank, memória, aquecimento, acessibilidade e Android 7.0 / 12 / 13+.
7. Não alterar diretamente `main`. Criar branch e PR; manter notas de licença/atribuição de terceiros.

## Verificações

- `node --test catalog-service/test/*.test.cjs`
- `gradle :app:testDebugUnitTest`
- `gradle :app:assembleDebug`
- Testes de interface: `gradle :app:connectedDebugAndroidTest` (emulador/dispositivo e Firebase emulado quando necessário).
