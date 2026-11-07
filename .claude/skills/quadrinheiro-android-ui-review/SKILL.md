---
name: quadrinheiro-android-ui-review
description: Auditar a experiência Android Compose do Quadrinheiro usando princípios de design Apple com adaptação às diretrizes Android.
---

# Revisão de interface Android inspirada na Apple

**Referência conceitual:** https://github.com/dickwu/apple-design-skill. Este arquivo não redistribui o conteúdo das referências de terceiros; consultar fontes originais durante revisões que exijam citações HIG.

Para cada tela:

- **Leitura e hierarquia:** teste contraste de texto no fundo real, legibilidade com escala de fonte do sistema, truncamentos e alvos de toque de aproximadamente 48 dp no Android.
- **Acessibilidade:** verifique TalkBack, `contentDescription`, `stateDescription`, labels de inputs, navegação por teclado e seleção sem depender só de cor.
- **Estrutura:** confira voltar, abas, folhas, busca, estados vazios, carregamento, erros e confirmações destrutivas.
- **Materiais:** aplique os estilos existentes de `LocalAppleMaterials`; sem camadas translúcidas desnecessárias ou texto sobre fundos ilegíveis. Honre redução de transparência.
- **Movimento:** use `LocalAppMotion` e evite movimento contínuo/decorativo com `reduced=true`.
- **Consistência:** compare modo claro/escuro, tablets, tamanho de fonte aumentado, Android 7.0 e APIs com shaders.

Formato de resultado: tela/arquivo, achado reproduzível, impacto, correção em Compose, evidência/teste. Separe **inspiração HIG** de requisito efetivo da plataforma Android.
