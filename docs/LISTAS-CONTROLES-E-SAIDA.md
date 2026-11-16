# Listas: controles e saída dos painéis

## Revisão: menus ancorados e navegação por nível

A revisão seguinte substitui também os `DropdownMenu` restantes do detalhe e dos itens por `RetainedGlassMenu`, o mesmo renderizador do seletor do perfil. Cada janela Compose hospeda seu próprio overlay e uma captura reutilizada da cena, evitando que o menu seja desenhado atrás do Dialog. Não há captura por botão nem animação contínua adicional.

A visibilidade usa `AppleMenuPicker`, com Privada/Pública e marcação da escolha atual. Referência estética: [Apple HIG — Pop-up buttons](https://developer.apple.com/design/human-interface-guidelines/pop-up-buttons), para opções mutuamente exclusivas. A publicação continua exigindo conta compatível, confirmação e salvamento explícito.

Detalhe → edição → escolha da capa → recorte preserva a página anterior. Subpáginas entram e saem lateralmente usando o progresso de `FluidSheetState` em `graphicsLayer`; a apresentação inicial e as confirmações compactas continuam como folhas. Fechar o recorte retorna à seleção, fechar a seleção retorna ao editor, e fechar o editor retorna ao detalhe. Navegar para o catálogo/HQ encerra a navegação interna. Movimento reduzido continua respeitado.

O ranking usa posições animadas de itens com chaves estáveis, abrindo espaço durante o arraste e animando também mudanças pelo comando Mover. A edição permanece local até salvar.

A busca solicita português e filtra o idioma antes de agrupar edições. Idioma explícito estrangeiro prevalece sobre editora, loja brasileira ou título traduzido. Resultados online sem idioma precisam de evidência em português na sinopse original; uma tradução em cache não é prova. Isso pode reduzir resultados de fontes que omitem o idioma. A busca de metadados complementares da HQ continua podendo consultar fontes estrangeiras; os registros da biblioteca não são reescritos.

Na revisão visual, o menu estreito revelou um erro do cálculo de largura compartilhado: faltava espaço para a seleção, ícone e seus intervalos. A largura considera esses elementos e o seletor de visibilidade tem largura mínima. Um teste confere a diagramação do controle: o rótulo Pública deve permanecer em uma linha entre os ícones.

Validação: 24 testes do serviço de catálogo, 40 unitários e 16 cenários de interface passaram. Após o ajuste de largura, os três testes de menu/movimento/navegação foram repetidos com sucesso, incluindo a verificação de uma linha. Build debug e APK de testes gerados; instalação preserva os dados. A revisão manual percorreu os menus e a sequência de voltar sem aplicar alterações. Não foram realizadas medições físicas comparativas de CPU/GPU, bateria ou p95/p99; não há afirmação de desempenho em toda a matriz Android.

## Histórico da revisão anterior

Revisão de 16/11/2026, substituindo o estúdio escuro isolado e os diálogos Material das listas.

| Solicitação | Implementação |
| --- | --- |
| Menu de visibilidade desconexo | Painel compacto no tema do app, opções com ícones, descrição e seleção semântica; sem AlertDialog roxo |
| Botões e slider do recorte diferentes | Mesma CurationSheet do editor, AppleSlider existente, centralização com botão do app e confirmação no cabeçalho |
| Escolher capa sem destaque | Ação primária com fundo contrastante e texto invertido, preservando o tema claro/escuro |
| Retirar listas públicas | Removido o atalho de exploração do cabeçalho; não foram apagadas listas nem alteradas regras de publicação |
| Saída estranha | Uma animação dirigida por FluidSheetState; janela e conteúdo permanecem até a conclusão; sem segunda animação ou escurecimento da janela Android |

O deslocamento de saída usa a altura do painel: folhas compactas e painéis completos não cortam a animação antes de sair da tela. O escurecimento acompanha o mesmo progresso. Transformações são aplicadas em `graphicsLayer`, respeitando movimento reduzido.

Salvar, adicionar HQs, confirmar recorte e navegar a partir do detalhe aguardam a saída antes de remover ou substituir a janela. O pedido de confirmação de alterações pendentes acontece antes de fechar o editor. O gesto preditivo permanece disponível no editor sem alterações pendentes.

Confirmações de publicação, conexão, descarte, exclusão, notas e posição também usam os painéis existentes. Texto longo e teclado podem rolar dentro das confirmações compactas. Os dados, contas, catálogo, regras do banco e registro de leitura permanecem sob as mesmas políticas.

Testes de interface adicionados: retenção do painel durante a animação, fechamento único e escolha de visibilidade após a saída. Os testes existentes de notas, ordem, salvamento, seleção, pinça e isolamento continuam sendo usados. A validação visual é feita no emulador, preservando a lista e os registros reais; não há afirmação de medição de desempenho em aparelho físico.

Resultado: 37 testes unitários aprovados e 15 cenários de interface validados. O teste de confirmação do recorte passou na reexecução após sua espera ser adaptada à saída assíncrona; o código do app não precisou mudar nessa reexecução. APK instalado preservando dados, atalho público ausente e emulador mantido aberto. Revisão visual confirmou o menu neutro, a ação de capa contrastante e o recorte no tema claro com AppleSlider.
