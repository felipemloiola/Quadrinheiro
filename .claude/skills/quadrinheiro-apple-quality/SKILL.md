---
name: quadrinheiro-apple-quality
description: Planejar, revisar e testar alterações de qualidade no Quadrinheiro Android com critérios inspirados no claude-code-apple-skills.
---

# Qualidade no Quadrinheiro

**Referência:** https://github.com/rshankras/claude-code-apple-skills (MIT). Esta habilidade é original e adaptada à stack Android; não copie modelos Swift/UIKit literalmente.

Ao receber uma tarefa:

1. Localize o fluxo funcional em `app/src/main/java/com/example/`, seus testes em `app/src/test/` e eventuais regras do Firestore.
2. Determine invariantes: `minSdk=24`, isolamento por conta, sincronização sem perda de dados, controles acessíveis, movimento reduzido e trabalho gráfico limitado.
3. Faça a menor mudança possível com teste de regressão. Use Compose state/lifecycle corretamente, `remember` para objetos caros, e dispatchers apropriados para I/O.
4. Performance: avalie frames lentos, custo de `RenderEffect`/AGSL, caches, navegação, carregamento incremental e trabalho fora da thread principal. Não use apenas microbenchmarks.
5. Segurança: confira auth, fronteiras de confiança do catálogo, limites de requisição, credenciais e mensagens de erro sem tokens.
6. Execute gradle/testes quando o ambiente permitir; se não permitir, declare exatamente o que não foi executado.
7. Entregue um PR descrevendo impacto, evidências, dispositivos/API levels testados, regressões e rollback.

Ordem de prioridade: falha de segurança/perda de dados > crash/ANR > acessibilidade > tempo de resposta/energia > consistência visual.
