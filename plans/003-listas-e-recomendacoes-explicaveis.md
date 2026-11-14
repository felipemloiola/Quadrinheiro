# Listas pessoais e recomendações explicáveis

Status: implementado e validado localmente. Sem publicação ou deploy de regras.
Data: 13/11/2026.

## Base consultada

- `CONTEXTO-DO-APP.md`, `README.md`, `AGENTS.md` e integração visual Android/Apple.
- Código atual: `PersonalizedDiscovery` usa fingerprint v14, motivos curtos e snapshots por conta. `ExploreScreen` já apresenta `reasonLabel`. Ampliar esse caminho, sem outro recomendador.
- Biblioteca em `MainTabsScreens.kt`, estado no `QuadrinheiroViewModel`, persistência em `SafeStorage` e `QuadrinheiroCloudRepository`. Já há backup JSON e isolamento por UID.
- Referência de produto: [listas Letterboxd](https://letterboxd.zendesk.com/hc/en-us/articles/15179317247247-What-are-lists-for) e [ordenação e notas](https://support.letterboxd.com/hc/en-us/articles/15179277798543-Is-there-an-easy-way-to-move-items-in-a-long-list-aside-from-dragging-them). Usar o conceito, sem copiar identidade visual.

## 1. Escopo inicial: listas no estilo Letterboxd

- Nova seção **Listas** na Biblioteca, acessível pelo Perfil; preservar Estante e Diário.
- Criar/editar nome e descrição; capa automática com mosaico das primeiras HQs, usando as capas já disponíveis.
- Listas comuns ou numeradas como ranking, com ordem manual persistida.
- Adicionar HQs da biblioteca ou da busca existente, sem exigir registro como Lido/Lendo/Quero Ler. A mesma HQ pode participar de várias listas, uma vez por lista.
- Detalhe com título, descrição, quantidade, capas e notas opcionais por item. Abrir a HQ reutiliza o fluxo existente.
- Reordenar no editor por arrastar; oferecer também **Mover para posição** para acessibilidade e listas longas. Alterações do editor só são enviadas ao tocar em Salvar.
- Ação **Adicionar a uma lista** nos detalhes da HQ; selecionar lista existente ou criar uma.
- Remover um item ou excluir uma lista nunca apaga a HQ do acervo, sua resenha ou seu progresso. Excluir lista exige confirmação.
- Listas privadas nesta primeira entrega. Publicação, links públicos, curtidas, comentários, colaboração e listas automáticas ficam para uma fase própria. Não adicionar campos de posse/empréstimo agora.

### Dados e custo

- Modelo dedicado `ComicList` + `ComicListItem`: UUID da lista, nome, descrição, ranking, itens ordenados, notas, revisão e timestamps. Não inserir listas dentro de cada `ComicEntry`.
- Cada item usa identidade estável da edição/provedor e metadados mínimos de apresentação. Resolver contra a biblioteca quando disponível; conservar referência mínima se a HQ sair do acervo. Edições diferentes continuam distintas.
- Cloud: `users/{uid}/lists/{listId}`. Um documento limitado por lista, com até 200 itens e orçamento de 256 KiB; validar quantidade/tamanho antes de salvar, com mensagem clara ao exceder. Sem sinopses ou imagens binárias nesse documento. A consulta retorna até 100 listas ativas; tombstones são mantidos no servidor para impedir ressurreição, sem consumir a cota ativa.
- Persistência local separada e limitada por UID, sem mudar o modo somente online. Convidado permanece local e isolado. Incluir listas no backup JSON; backups antigos continuam válidos.
- Carregar listas ao abrir a seção, reutilizar estado em memória e não consultar catálogo por cartão. Uma gravação lógica por Salvar; nenhuma gravação por quadro de arraste.
- Integrar o gate de escrita existente, preservando sua espera pela Task real. Comparar revisão antes de sobrescrever: conflito em outro aparelho mantém o rascunho e pede recarregamento/revisão, sem perda silenciosa. Exclusão precisa de tombstone/controle de revisão para não ressuscitar por snapshot antigo.
- Regras Firestore específicas: somente dono, schema/tamanhos limitados, revisões válidas e testes de acesso entre contas. Não ampliar regras recursivas existentes.

## 2. Escopo inicial: recomendações explicáveis

- Manter a frase curta já mostrada em **Para você** e acrescentar **Por que esta HQ?**, abrindo uma folha com o motivo e sua referência real.
- Exemplos: autor em comum com uma HQ bem avaliada; personagem favoritado; próxima edição comprovada de uma série. Sem evidência suficiente, não inventar explicação nem tratar curadoria como personalização.
- Persistir motivo estruturado junto da recomendação: tipo de sinal, referência de origem e dados mínimos de exibição. O texto detalhado deve vir desse sinal; não gerar justificativa depois, independente do ranqueamento.
- Ações: **Não tenho interesse**, **Mais como esta** e os fluxos existentes de **Já li / Quero ler**. Registrar leitura exige a confirmação normal do app; não marcar automaticamente ao tocar em feedback.
- Não tenho interesse oculta aquela obra e suas variantes reconhecidas; não bloqueia todo autor/personagem. Permitir desfazer e gerenciar sugestões ocultas.
- Mais como esta registra preferência explícita e limitada, sem alterar favoritos ou avaliações. Aproveitar os sinais e o orçamento do retriever atual, sem multiplicar buscas.
- Feedback em registro privado limitado por conta: `users/{uid}/recommendationPreferences/state`, com versões e limites validados; espelho local e backup. Não transmitir histórico pessoal a provedores externos.
- Acrescentar feedback normalizado ao fingerprint canônico em uma única migração de versão. Consultar o motivo, navegar, ler páginas e abrir listas não regeneram sugestões. Alterações relevantes e feedback explícito podem atualizar a seleção, com coalescência e guardas de UID/geração existentes.
- Preservar filtro de português, identidade de obras/séries, diversidade e validação de capas. As listas não viram sinal de gosto automaticamente: uma lista chamada “Não gostei” não deve gerar afinidade positiva.
- Snapshots antigos sem motivo estruturado continuam legíveis: manter o rótulo existente, sem fabricar uma referência. Completar o detalhe na próxima geração válida, sem nova busca por abrir a folha.

## 3. Sequência de execução após revisão

1. Criar branch `codex/listas-recomendacoes-explicaveis` a partir do estado de trabalho adequado, preservando os commits anteriores. Fechar contratos de identidade, limites, serialização e revisão.
2. Implementar dados de listas, backup, regras e testes focados; depois telas, editor e ação de adicionar. Validar esse fluxo antes de avançar.
3. Ampliar o motivo estruturado do recomendador, armazenamento do feedback e fingerprint; depois folha explicativa e ações. Reutilizar UI, catálogo, capas e ranking existentes.
4. Rodar regressões afetadas, compilar uma versão consolidada e validar interface no emulador. Manter o emulador aberto. Publicação/atualização assinada são uma etapa posterior, sem releases intermediárias.

## 4. Critérios de aceite e verificação

- Criar, editar, reordenar, reiniciar e recuperar listas; adicionar item só do catálogo; deduplicar a mesma edição; preservar outra edição e os dados da biblioteca ao excluir listas.
- Testar troca de conta, convidado, backup antigo/novo, documentos grandes, falha de gravação e conflito entre aparelhos. Dados privados não aparecem na comunidade.
- Explicação corresponde ao sinal que realmente contribuiu; ausência de metadata não inventa autor, série ou continuação.
- Feedback persiste, pode ser revertido e não reaparece por snapshot antigo; não mistura contas nem refaz requisições ao navegar. Regressões existentes de idioma e estabilidade continuam passando.
- Sem dependência nova para IA, listas ou arraste se os recursos existentes bastarem; Android mínimo 24, componentes de vidro existentes, Predictive Back, alvos de 48 dp, TalkBack e redução de movimento/transparência preservados.
- Primeiro testes unitários afetados e regras em Firebase emulado; após consolidação, `:app:testDebugUnitTest`, `:app:assembleDebug` e instrumentação focada no emulador explicitamente selecionado. Testes do catálogo somente se o serviço for alterado. Não usar acervo real como fixture nem instalar debug sobre app de produção.

## Economia deliberada

Sem painel de sincronização, novo motor de recomendações, chamadas de IA, redesign geral, novo backend, polling ou publicação social nesta entrega. Implementar em duas etapas funcionais, consolidar validação/builds e registrar decisões neste plano para evitar releitura global e retrabalho. Nenhum dado privado do contexto deve integrar commits ou PRs.

## Implementação e operação

- Modelos/codec e snapshots em `data/PersonalCuration.kt`; controlador em `CurationController.kt`; transações com revisão e gate existente em `CurationCloudRepository.kt`.
- O envelope Firestore valida dono, revisão, tombstone, timestamp e tamanho. O payload é JSON privado, validado pelo cliente; não concede permissões nem é interpretado por serviços públicos. Esta escolha limita a uma gravação lógica por Salvar e evita duplicar listas no documento do perfil.
- Componentes em `ui/components/CurationSheets.kt`; Biblioteca com Estante/Diário/Listas; ações nos detalhes e no modal de registro. Uma HQ encontrada na busca também pode entrar em um rascunho ainda não salvo.
- Recomendações ampliam `RecommendedComic` e o ranqueador existente com evidência estruturada e feedback. Fingerprint v15; preferências explícitas não alteram favoritos/avaliações. Seleção vazia já calculada não reinicia consultas apenas por navegar. Guardas de conta e geração permanecem antes da publicação.
- Rascunhos são preservados ao fechar e antes de tentar gravar. Falha em preservá-los mantém o editor aberto. Durante a gravação, edição e fechamento ficam bloqueados para não perder alterações posteriores ao payload enviado. Conflitos permitem recarregar ou salvar o rascunho como uma nova lista; tombstones não são reativados.
- Backups antigos permanecem aceitos. A restauração das novas seções usa as revisões do destino e verifica a conta durante todo o processo; não copia o UID do arquivo importado.
- Regras novas são protótipos validados em Firebase emulado. Devem ser revisadas e implantadas no banco correto antes de distribuir um APK de produção com acesso cloud a estas funcionalidades. Nenhuma regra foi implantada no projeto real nesta tarefa.
- Publicação da branch/PR e geração de APK com assinatura permanente permanecem etapas posteriores. Não instalar debug sobre a instalação pessoal.

## Resultado da validação

- Rodada completa: 313 testes unitários/backend aprovados, sem falhas ou ignorados, com Auth/Firestore exclusivamente emulados no projeto demo. Inclui acesso por outra conta, consultas não autenticadas, schema inválido, payload excessivo, replay de revisão, conflitos e tombstones.
- Checagens finais dos módulos afetados: 46 testes aprovados. Incluem 18 casos de listas, snapshots, limites, rascunhos independentes, feedback e cache nas APIs 24/34 via Robolectric; mais recomendações, estabilidade, concorrência e isolamento existentes.
- Emulador API 37: seis cenários novos de interface e quatro regressões de voltar preditivo aprovados. Incluem catálogo para rascunho sem registro de leitura, ranking/notas/ordem, arraste, troca de conta, cota de listas ativas e conclusão de gravação após cancelamento da tela. A explicação foi testada com texto ampliado e transparência/movimento reduzidos.
- APK debug e APK de instrumentação compilados. Captura composta pelo Android inspecionada com dados fictícios; a captura de raiz do Compose retornava a janela de fundo e foi substituída pelo screenshot do sistema somente no teste.
- App principal abriu normalmente após os testes. Emulador deixado aberto em Início; dados existentes preservados. Não houve instalação no celular nem medição física de desempenho/temperatura.
- Dados remotos exigem implantação das regras novas no banco de produção e validação com o APK assinado. Essa etapa não foi executada.

Detalhes de preservação: rascunhos são separados por UID e UUID da lista (até 100, cada um limitado a 256 KiB), e salvar uma lista só remove seu próprio rascunho. A cópia de snapshot por conta continua limitada a 4 MiB; uma falha de cache após confirmação do servidor é apresentada como falha de cópia local, sem ocultar a nova revisão ou bloquear acesso aos dados carregados da conta. Preferências locais são hidratadas antes de publicar recomendações em cache.
