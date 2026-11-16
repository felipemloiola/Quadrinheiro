# Listas e página da HQ — implementação dos rascunhos aprovados

Status: primeira etapa implementada e validada localmente; acabamentos pendentes para retomada. Branch `codex/listas-recomendacoes-explicaveis`.

## Decisões aceitas

- Listas: um destaque na Biblioteca, demais linhas compactas; banner no detalhe de todas; capa escolhida entre os itens com enquadramento ajustável. Editor e seletores com capas, busca, seleção e hierarquia consistente.
- Página da HQ: conceito C, ficha compacta, abas existentes, resumo de leitura e ações no rodapé, listas privadas associadas.
- Preservar integralmente os formulários/painéis existentes de registro e progresso. Botões da página abrem o fluxo atual, sem salvar status automaticamente.
- Recomendações: folha compacta com evidência real e ações existentes, sem alterar ranqueamento.
- Não publicar código/artefatos nem alterar backend sem autorização. Rascunhos e contexto privados não entram no Git.

## Etapas e retomada

1. [x] Modelo retrocompatível de capa/foco/destaque; testes de codec/isolamento.
2. [ ] Biblioteca e detalhe, capa/foco, menu de itens e seleção em lote implementados; falta consolidar catálogo dentro do seletor, ações diretas do menu e refinamento do editor.
3. [x] Página HQ C mantendo registro existente e navegação de entidades/comunidade; revisão visual em andamento.
4. [ ] Explicações receberam capa e botões lado a lado; falta reduzir a altura da folha preservando expansão e fontes grandes.
5. [x] Testes afetados, build e revisão visual inicial em emulador mantido aberto (matriz ampliada fica para retomada).

## Limite solicitado

Usuário autorizou fazer o possível com saldo semanal informado de 9% e parar com retomada documentada se necessário. Não há leitura direta desse saldo disponível nesta sessão. Usar passos verificáveis, evitar reauditorias amplas/geração de imagens e registrar exatamente o que passou e o que falta antes de encerrar.

## Invariantes

minSdk24, materiais/movimento existentes, conta/UID e revisões, sem dados inventados do catálogo, páginas só da API, exclusão da lista não altera acervo. Mudanças visuais não devem criar consultas de rede por cartão ou animações contínuas.

## Validação da etapa

- Compilação Kotlin aprovada. Testes afetados:35 aprovados,0 falhas/ignorados; PersonalCuration em APIs24/34, apresentação de leitura e regressões de relações/metadados.
- Build dos APKs aprovado;9 testes de interface aprovados (8 de listas +1 da página HQ, incluindo os três estados e callback de registro intacto). Logs locais ignorados `redesign-validation.log`, `redesign-final-build.log` e `redesign-native.log` em `.build-outputs/`.
- Capturas reais inspecionadas: listas e estados Lendo/Lido com fixtures. Identificados e corrigidos espaço do rodapé em relação à barra de gestos e cabeçalho excessivo em lista sem imagem. Build final e2 testes focais dessas correções aprovados em `redesign-visual-fix-build.log` e `redesign-visual-fix-native.log`.
- Registro existente em CatalogModal.kt não foi alterado. A página chama o callback atual com o registro intacto. Nenhuma escrita remota/deploy/release efetuado.
- Emulador Medium_Phone_API_37.0 aberto para revisão; manter aberto. Nenhum aparelho físico conectado. Assinatura debug comparada com a instalação do emulador antes de atualizar; dados preservados por install-r.
- Sem medição de regressão de CPU/GPU/jank/temperatura, nem matriz ampliada de UI concluída. Não declarar fidelidade/performance final. Nenhum teste ou serviço de catálogo alterado; sua suíte não foi repetida nesta etapa. As regras de listas da etapa anterior continuam locais, sem deploy: contas reais precisam dessa entrega posterior, não confundir APK debug com produção pronta.

## Próximo ponto de retomada

1. Conferir os resultados finais abaixo e o Git antes de mexer no código; não refazer o levantamento dos rascunhos.
2. Terminar seletor de HQs com catálogo no mesmo fluxo; atualmente Buscar HQ no catálogo preserva rascunho e reutiliza a busca existente.
3. Refinar editor/detalhe conforme rascunhos (ações de adicionar/capa/excluir diretas no menu; hierarquia do cabeçalho e confirmação de adição).
4. Fazer folha de recomendação compacta com altura adaptativa e validar fontes grandes, modo claro, transparência/movimento reduzidos, API24/31/33+ e performance comparável.
5. Somente depois preparar PR/publicação autorizada e regras/assinatura de produção; rascunhos, contexto e capturas pessoais ficam privados.

## Correções solicitadas após teste no emulador (saldo informado:5%)

- Cabeçalho de Listas alinhado ao padrão da Biblioteca: voltar ao Perfil, legenda, título, divisor e seletor com ícones.
- Painéis: reservar a área real abaixo do recuo superior; aplicar somente o delta animado em graphicsLayer. A transformação anterior movia o conteúdo para fora da janela, cortando botões.
- Salvar lista, Adicionar(N) e Concluir como ações principais fixas no cabeçalho. Campos de criação agrupados sem contadores permanentes e sem a pilha de ações de recuperação para erro de carregamento.
- Enquadramento por arrastar/pinçar, sliders opcionais recolhidos em Ajustes precisos. Sem nova animação, blur ou dependência.
- Estado ready explícito impede oferecer salvamento antes da leitura autenticada inicial. Nenhum fallback silencioso para armazenamento de outra conta/local.
- Confirmado via release do banco NOMEADO: regras publicadas não continham lists/recommendationPreferences. Diff semântico limitado às permissões privadas já existentes no repositório; acervo e painel preservados. Validação em Firebase emulado antes de aplicar a correção pedida ao backend.
- Logs locais: `curation-fixes-validation.log`; cópia das regras anteriores em `curation-live-before.rules`, ambas em `.build-outputs/`. Registrar abaixo resultado de deploy, instrumentação e commit ao concluir.

### Ponto de parada — usuário informou1% restante

-27 testes aprovados (24 curadoria,3 regras com Auth/Firestore emulados),9 nativos aprovados incluindo pinça, seleção em lote e salvamento desabilitado quando conta indisponível. Build final do diagnóstico/superfície legível aprovado. APK atualizado só no emulador, por install-r, sem apagar dados; manter aberto.
- Usuário autorizou explicitamente publicar as regras privadas. Conector reportou sucesso sem alterar o release nomeado; CLI com configuração temporária contendo só regras publicou corretamente. Leitura posterior do release exato confirmou lists/recommendationPreferences presentes. Sem mudanças nas permissões do acervo; configuração não inclui índices. Backups e logs permanecem privados.
- IMPEDIMENTO REMANESCENTE COMPROVADO: `PrivateCuration: Load IllegalStateException code=none at CurationCloudRepository.owner(CurationCloudRepository.kt:17)`. A sessão Firebase não corresponde ao UID solicitado pelas listas (ainda não distinguido entre sessão ausente ou outra conta). Não remover a checagem de dono, não migrar automaticamente os dados para outro UID, não deslogar/apagar dados.
- Rascunho real Teste com1HQ preservado. Salvamento na conta real ainda NÃO validado/resolvido; testes de convidado e regras não substituem essa verificação.
- Próxima ação: verificar/reconectar a sessão da conta correta pelo fluxo existente, preservando rascunhos; ajustar a mensagem/ação de reconexão e validar leitura + salvamento real. Depois revisar as telas claras e confirmar a aparência com usuário. Não refazer geração de rascunhos nem auditoria ampla.

### Retomada — diagnóstico completo

O login por e-mail do aplicativo é deliberadamente local (`QuadrinheiroAuthManager.signInWithEmail` e `SafeStorage`, UID email_...), sem FirebaseAuth. A curadoria só reconhecia convidado como local e tentava abrir Firestore para a conta por e-mail. Não é necessário trocar conta/reconectar Google para corrigir esse caso.

Correção: política explícita isLocalOwner fornecida pelo ViewModel, baseada no perfil/sessão reais; convidado e conta local por e-mail usam snapshots/rascunhos privados por UID. Google continua no Firestore, inclusive quando sua sessão falha (nenhum fallback local por falha de rede/auth). Mudança de modo/UID invalida o estado do controller. UI informa armazenamento neste aparelho. Registro de leitura/acervo/auth não alterados.

Validar salvar e recarregar em conta por e-mail, rejeitar escrita tardia após troca de conta e preservar o rascunho real Teste. Teste nativo novo cobre esse ciclo com fixture e restaura os arquivos ao terminar.

Resultado da retomada:27 testes unitários aprovados,10 nativos aprovados, APKs compilados e instalação no emulador preservando dados. Conta existente abriu listas sem erro; rascunho Teste(1HQ) foi salvo com seu conteúdo preservado. Após fechar/reabrir o processo, a lista reapareceu e o rascunho deixou de ser exibido. Captura privada do editor em `.build-outputs/curation-email-editor.png`; logs `curation-email-validation.log` e `curation-email-native.log`. Emulador mantido aberto em Listas. Impedimento de salvamento desta conta resolvido; pendências de acabamento anteriores continuam separadas acima.
