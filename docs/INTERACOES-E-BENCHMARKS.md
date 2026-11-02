# Interações e validação de fluidez

Os controles compartilham `AppMotion`, que preserva o amortecimento solicitado
por cada interação, inclusive o rebote. Com **Reduzir movimento**, as
transições automáticas são estáticas, os indicadores continuam informando estado
e os arrastes continuam acompanhando o dedo. O sistema Android ainda controla a
escala global das animações.

Nos controles líquidos, a posição visual acompanha o alvo com a mola do Kyant
(amortecimento 1, rigidez 1000). Os deltas do dedo acumulam no alvo lógico para
evitar perda de deslocamento por causa do atraso visual da mola. X e Y têm molas
independentes (0,6/250 e 0,7/250); o realce e a deformação por velocidade usam
0,5/300. Toggle e slider expandem até 1,5× durante a pressão. A retração espera a
posição chegar perto do alvo e pode ser interrompida por outro toque.
Cancelar restaura o estado anterior;
confirmar executa o callback uma única vez. Sliders e estrelas aguardam a intenção
horizontal antes de bloquear a rolagem vertical. Alvos interativos têm pelo menos
48 dp de altura; switches, sliders e avaliações expõem ações de acessibilidade.

## Referências e adaptações

Parâmetros e efeitos seguem o [AndroidLiquidGlass/Backdrop do Kyant](https://github.com/Kyant0/AndroidLiquidGlass/tree/65ab177e90e5c1d8c62e70cf7755841982da65f6),
especialmente `DampedDragAnimation`, `InteractiveHighlight`, `LiquidButton`,
`LiquidToggle`, `LiquidSlider` e `LiquidBottomTabs`. A integração mantém as
preferências do Quadrinheiro, cancelamento sem confirmação, semântica de
acessibilidade e áreas de toque Android. O botão cápsula reutiliza `LiquidButton`.
As transições específicas de menus e busca recuperam suas molas expressivas.

A tab bar mantém a geometria 64/56 dp e as lentes 24/24 e 10/14 do Kyant,
inclusive durante rolagem. A superfície de gesto cobre a barra inteira: a lente
antecipa a opção no toque inicial, acompanha o arraste e só confirma ao soltar.
Cancelar restaura a opção anterior. O seletor Estante/Diário usa a mesma regra.
`NavigationTouchTest` verifica a posição da lente antes do touch-up e impede
recomposição do conteúdo das abas a cada frame da mola.

Campos de uma linha têm altura mínima de 48 dp, sem somar padding vertical ao
botão de limpar. O stepper desenha uma superfície de 32 dp dentro de uma área
de toque de 48 dp, mantendo o pressionar e segurar.

Widgets leem exclusivamente a sessão ativa. O logout invalida o conteúdo
imediatamente, e a troca de conta invalida também a preferência de página.
Ações do widget carregam o proprietário que originou seu conteúdo. Atualizações
comuns são agrupadas, enquanto transições de sessão têm atualização imediata.
`WidgetSessionTest` cobre troca de proprietário, logout, ações antigas e revisões
consecutivas no mesmo milissegundo.

O [LiquidGlassKMP do Philipp](https://github.com/philipplackner/LiquidGlassKMP/tree/eb40bdd6a49bcc3a6d79a44b672bddedecbedb7b)
usa SwiftUI nativo no iOS (`NavigationStack`, `Menu`, `TabView`) e Material 3 no
Android. Ele serve de referência para organização e navegação, não contém uma
implementação Android dos controles líquidos nativos do iOS. Não há promessa de
equivalência exata ao renderizador ou à física privados da Apple.

Consulte `THIRD_PARTY_NOTICES.md` para origem e licença do código adaptado.

## Testes de regressão

Com Gradle 9.3.1, Java 21 e SDK Android 36.1:

```sh
gradle :app:testDebugUnitTest --tests com.example.FluidMotionPolicyTest
gradle :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.FluidControlsTest
```

Os testes instrumentados verificam confirmação única via acessibilidade,
cancelamento do switch, restauração do slider, rolagem sobre estrelas e repetição
do stepper sem incremento adicional ao soltar. `LiquidSpringTest` verifica
expansão independente em X/Y, acúmulo dos alvos durante arraste, interrupção da
retração e ausência de deformação residual por velocidade. `GoogleConfigurationTest`
confirma o projeto e o cliente Web nos recursos gerados pelo Google Services.
Os testes unitários verificam
projeção de velocidade, resistência nos limites, o centroide da pinça e movimento
reduzido. Os testes de persistência que usam Firebase precisam dos emuladores
Auth/Firestore, conforme o workflow de release existente.

## Benchmarks e geração de perfil

O módulo `:macrobenchmark` usa uma variante `benchmark` não depurável do app,
assinada com a chave de debug. Ele não usa nem altera a assinatura de release.
Use um dispositivo de testes: a variante tem o mesmo applicationId e pode
conflitar com uma instalação assinada para produção.

```sh
gradle :macrobenchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.macrobenchmark.FluidityBenchmark
gradle :macrobenchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.macrobenchmark.QuadrinheiroBaselineProfileGenerator
```

O primeiro comando mede inicialização fria e tempo de frames ao percorrer
Biblioteca e Ajustes. O segundo usa `BaselineProfileRule` para gerar um perfil ART
a partir das mesmas jornadas. Os arquivos de resultado e traces ficam em
`macrobenchmark/build/outputs/`; revise o perfil gerado e incorpore suas regras ao
`app/src/main/baseline-prof.txt` antes de uma release.

Para medir uma biblioteca grande, importe uma coleção de teste representativa
no modo convidado antes da execução. Uma coleção vazia mede navegação e superfícies,
mas não o custo de rolar muitas capas. Use dispositivo físico para comparar
desempenho; números de emulador não comprovam fluidez em hardware real. Compare
os mesmos dados, dispositivo, taxa de atualização e estado térmico antes/depois.

## Revisão manual

- Soltar um flick curto para baixo deve fechar o painel; um impulso para cima
  deve expandi-lo. Retomar um painel em movimento deve preservar sua posição.
- Arrastar verticalmente a partir de um slider ou das estrelas deve permitir a
  rolagem; arrastar horizontalmente deve ajustar o controle.
- Cancelar um gesto não deve confirmar alterações. Pressionar o stepper por um
  segundo deve repetir e parar ao soltar, inclusive ao alcançar os limites.
- Testar TalkBack, teclado, fonte ampliada, tema claro/escuro e Reduzir movimento.
- Durante a pinça, o ponto da foto entre os dedos deve permanecer sob o centroide.

O workflow de release mantém os testes de Firebase e a verificação do certificado
permanente antes de publicar o APK e o manifesto de atualização.
