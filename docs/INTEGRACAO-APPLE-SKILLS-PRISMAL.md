# Auditoria e integração seletiva — novembro de 2026

## Escopo

Projeto: **Quadrinheiro**, Android/Kotlin/Jetpack Compose, `minSdk 24`. Referências solicitadas:

- [claude-code-apple-skills](https://github.com/rshankras/claude-code-apple-skills) — catálogo de habilidades para agentes Claude/Apple (MIT).
- [apple-design-skill](https://github.com/dickwu/apple-design-skill) — orientações de revisão HIG para agentes; não há arquivo LICENSE na raiz consultada. Não copiar seu acervo sem verificar direitos.
- [Prismal](https://github.com/styropyr0/Prismal) — biblioteca Android View/OpenGL ES para Liquid Glass (MIT), **minSdk 25**. O README recomenda o [PrismalAGSL](https://github.com/styropyr0/PrismalAGSL) para aplicativos Compose.

Esses repositórios **não são três bibliotecas intercambiáveis**: os dois primeiros são conhecimento/skills de desenvolvimento e o terceiro é código gráfico Android.

## O que foi integrado nesta proposta

1. **Orientação de trabalho e revisão por agentes:** `AGENTS.md` e três habilidades específicas em `.claude/skills/`, cada qual com referência à fonte e critérios Android. Não vendorizamos bibliotecas de código Swift nem centenas de arquivos não utilizados.
2. **Contribuição óptica inspirada no Prismal:** no shader SDF já existente, uma contribuição Fresnel de borda discreta, sem outra captura de fundo e sem nova dependência. A variante com aberração cromática desativada passou a usar **1 leitura** de textura em vez de **3**, preservando caminho RGB completo quando ativo.
3. **Teste de contrato do shader:** `LiquidGlassOpticsContractTest` protege a escolha dos dois caminhos. É teste de estrutura do shader; **não substitui** teste visual ou compilação AGSL em dispositivo.

Não incluímos a dependência Prismal no build: ela exige API 25 e adicionaria um pipeline de composição View/OpenGL paralelo ao motor Compose/Backdrop já presente. A recomendação upstream para Compose é PrismalAGSL, que também precisa de avaliação de compatibilidade, licenças, benchmarks e API mínima antes de adoção.

## Achados técnicos e prioridades

| Prioridade | Achado | Ação recomendada |
|---|---|---|
| Alta | `release.isMinifyEnabled = false`, apesar de existirem regras ProGuard | Medir release minificado em branch própria com R8, Firebase, reflection e teste de atualização/assinatura antes de habilitar. |
| Alta | Muitos workflows pontuais que aplicam scripts de mudanças ao código | Consolidar em CI de PR para evitar deriva entre scripts e execução sobre branches de entrega; proteger `main`. |
| Alta | Pipeline gráfico rico com shaders, blur, animações e captura de backdrop | Comparar baselines de frame time/memória/temperatura e ativar degradação progressiva em dispositivos lentos. |
| Média | Compatibilidade Android 7.0 com dependências de efeito em API superiores | Manter fallback funcional em API 24/25/31/33+ com testes instrumentados e sem impor `minSdk 25`. |
| Média | Guidelines HIG descrevem plataformas Apple, mas o app é Android | Aplicar somente princípios transferíveis e testar semântica TalkBack, 48 dp, contraste, escala e movimento reduzido. |
| Média | A documentação contempla testes Android, porém o workflow de verificação está preso a branch de correção nomeada | Criar CI regular para PRs em etapa posterior, com Firebase emulador e debug key temporária. |

Esses itens são observações da configuração consultada, **não** evidências de vulnerabilidades exploráveis ou números de performance medidos.

## Plano de verificação e rollout

1. **Estático/contrato:** executar `gradle :app:testDebugUnitTest`; confirmar `LiquidGlassOpticsContractTest` e suites existentes.
2. **Build:** executar `gradle :app:assembleDebug` com SDK 36.1/JDK 21; executar `node --test catalog-service/test/*.test.cjs`.
3. **Dispositivos:** abrir biblioteca, detalhes e menus de vidro em API 24, 25, 31 e 33+, verificando que refração/brilho são desativados ou degradados sem erro em versões antigas.
4. **Acessibilidade:** aumentar fontes, ativar redução de movimento/transparência, validar TalkBack, testar alto contraste e fundos com capas claras/escuras.
5. **Performance:** comparar mesmos fluxos antes/depois com Macrobenchmark/Perfetto (p95/p99, jank, GPU e alocações); se piorar, reverter a mudança óptica sem remover skills.
6. **Publicação:** revisar visualmente e fazer merge somente após CI e testes em aparelho; sem publicar APK automaticamente.

## Licenças e atribuição

O projeto já documenta código adaptado do AndroidLiquidGlass/Backdrop sob Apache-2.0 em `THIRD_PARTY_NOTICES.md`. Esta alteração usa apenas **ideias técnicas** e habilidades originais específicas do Quadrinheiro, sem copiar código ou textos de Prismal/Apple Design. O efeito Fresnel é uma fórmula matemática comum; nenhuma fonte terceira nova é incorporada. Os repositórios permanecem identificados e linkados para rastreabilidade.
