# Listas: controles e saída dos painéis

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
