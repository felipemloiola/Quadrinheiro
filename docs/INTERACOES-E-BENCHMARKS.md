# Interações e validação de fluidez

Os controles compartilham `AppMotion`: molas sem rebote para toques comuns e
amortecimento de 0,8 para solturas com velocidade. Com **Reduzir movimento**, as
transições automáticas são estáticas, os indicadores continuam informando estado
e os arrastes continuam acompanhando o dedo. O sistema Android ainda controla a
escala global das animações.

Durante um arraste, o valor visual é atualizado diretamente. A mola é usada ao
soltar ou mudar o valor programaticamente. Cancelar restaura o estado anterior;
confirmar executa o callback uma única vez. Sliders e estrelas aguardam a intenção
horizontal antes de bloquear a rolagem vertical. Alvos interativos têm pelo menos
48 dp de altura; switches, sliders e avaliações expõem ações de acessibilidade.

## Testes de regressão

Com Gradle 9.3.1, Java 21 e SDK Android 36.1:

```sh
gradle :app:testDebugUnitTest --tests com.example.FluidMotionPolicyTest
gradle :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.FluidControlsTest
```

Os testes instrumentados verificam confirmação única via acessibilidade,
cancelamento do switch, restauração do slider, rolagem sobre estrelas e repetição
do stepper sem incremento adicional ao soltar. Os testes unitários verificam
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
