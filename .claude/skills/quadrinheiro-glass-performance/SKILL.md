---
name: quadrinheiro-glass-performance
description: Ajustar e auditar efeitos Liquid Glass no motor Compose existente, usando Prismal como referência óptica sem depender do seu renderer View/OpenGL.
---

# Óptica e desempenho

**Referência:** https://github.com/styropyr0/Prismal (MIT). O Prismal original exige minSdk 25, captura a hierarquia e usa pipeline View/OpenGL. O Quadrinheiro exige minSdk 24 e já usa Compose/AGSL/Backdrop.

Regras de implementação:

1. Não adicionar `implementation("com.github.styropyr0:Prismal:...")` nem JitPack ao app de produção sem testes de compatibilidade e custo. Para Compose, avaliar também o PrismalAGSL recomendado pelo próprio autor.
2. Prefira alterar de forma pequena e documentada o shader SDF do Quadrinheiro; preservar o fallback de `RenderEffect` para APIs antigas.
3. Ao desativar aberração cromática, usar uma única amostra da textura em vez de separar canais RGB. Preservar caminho RGB quando ativo.
4. O brilho de borda inspirado em Fresnel não pode reduzir contraste, interferir na seleção por toque ou gerar uma nova captura de fundo por componente.
5. Medir a mesma tela com/sem efeito: frames p95/p99, alocações, GPU, bateria e jank. Validar aparelhos de API 24, 25, 31 e 33+.
6. Reverter o preset se houver regressão perceptível em legibilidade ou fluidez.
