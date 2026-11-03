# Interações e validação de fluidez

Os seletores Estante/Diário, sliders e tab bar foram restaurados do commit
`32f1408f46d69d98026d516a1c575f2b11fd6ac5`, anterior à primeira revisão.
Isso inclui os wrappers AppleSlider/AppleSegmentedPicker e o contêiner original
da navegação. Esses controles usam cópias dos auxiliares originais no pacote
`catalog.legacy`; suas molas, geometria e gestos seguem a versão antiga.

Os demais controles conservam as correções posteriores, incluindo AppMotion,
movimento reduzido, repetição do stepper e acessibilidade do switch.
`RestoredControlsTest` verifica a seleção dos segmentos, o ajuste do slider
e a mudança de aba. A comparação com o commit original valida a restauração
dos componentes e funções solicitados.

A composição da Biblioteca mantém sua identidade ao alternar Estante/Diário,
preservando a mola do seletor e a memória de rolagem de cada seção. Os callbacks
dos seletores acompanham o estado mais recente. Os menus mantêm a última linha
válida durante um arraste para fora e não aplicam um véu branco à superfície
pressionada. `MenuTrackingTest` cobre a seleção ao soltar fora do menu.

## Referências e adaptações

A origem dos controles líquidos é o
[AndroidLiquidGlass/Backdrop do Kyant](https://github.com/Kyant0/AndroidLiquidGlass).
Consulte `THIRD_PARTY_NOTICES.md` para origem e licença.

Campos de uma linha têm altura mínima de 48 dp, sem somar padding vertical ao
botão de limpar. O stepper desenha uma superfície de 32 dp dentro de uma área
de toque de 48 dp, mantendo o pressionar e segurar.

Widgets leem exclusivamente a sessão ativa. O logout invalida o conteúdo
imediatamente, e a troca de conta invalida também a preferência de página.
Ações do widget carregam o proprietário que originou seu conteúdo. Atualizações
comuns são agrupadas, enquanto transições de sessão têm atualização imediata.
`WidgetSessionTest` cobre troca de proprietário, logout, ações antigas e revisões
consecutivas no mesmo milissegundo.

A leitura ativa do widget e da iluminação usa o mesmo resolvedor, incluindo IDs
externos e a remoção/conclusão da seleção anterior. Os snapshots e bitmaps dos
widgets são calculados em Dispatchers.IO; as quatro capas da estante carregam
em paralelo usando o ImageLoader/Coil compartilhado do aplicativo. Uma revisão
cancela trabalho anterior. Uma capa provisória não entra no cache de sucesso,
permitindo recuperá-la na próxima atualização com conexão. `WidgetCoverTest`
cobre falha, recuperação e reutilização da capa real. O layout recebe apenas
mais espaço interno, preservando suas cores, fontes e formatos.

O carregamento entre login e início reutiliza HomeReadingNowTab. Os mesmos
componentes medem cabeçalho, metas, leitura e capas; apenas o desenho de seu
conteúdo é substituído por placeholders. A barra inferior permanece no mesmo
lugar. A pulsação ocorre no desenho e respeita Reduzir movimento.
OpeningAndLoadingTest compara os limites do cabeçalho e das metas antes/depois
e registra screenshots para conferência visual. A coleta
dos fluxos da tela principal acompanha o lifecycle, evitando recomposições
durante o segundo plano. O ícone adaptativo usa a mesma logo com menos inset
e fundo claro/escuro via recursos do sistema; `LauncherThemeTest` verifica as
duas configurações. O launcher pode manter ícones em cache até atualizá-los.

Estas mudanças removem trabalho da interface e recriações desnecessárias; não
representam uma medição de FPS em aparelho físico. Use os benchmarks abaixo
para comparar hardware e coleções representativas.

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
cancelamento do switch, rolagem sobre estrelas e repetição
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

## Abertura de menus e edição

O editor usa LazyColumn com chaves estáveis e rascunho mantido acima dos itens.
As seções fora do viewport não são compostas/medidas ao abrir, e campos/notas
continuam no rascunho ao rolar. A localização duplicada durante composição foi
removida; a preparação existente no ViewModel continua preservada.

O pré-aquecimento mantém instâncias reais de RuntimeShader em filas por fonte,
incluindo o shader de refração do editor. Cada nó retira uma instância exclusiva,
com reposição em Dispatchers.Default; uniforms mutáveis não são compartilhados.
Isso retira a construção dos shaders preparados do primeiro clique. O custo
final de renderização da GPU depende do dispositivo e não é uma promessa de FPS.
Os menus não reiniciam suas molas ao atualizar o mesmo request/âncora.

O toggle reconhece intenção horizontal a partir de 2 dp, preserva a rolagem
vertical e mantém a superfície da lente visível durante a pressão.
OpeningAndLoadingTest verifica movimento visual antes do touch-up, confirmação
única, shaders independentes e salvamento após rolar o editor.
