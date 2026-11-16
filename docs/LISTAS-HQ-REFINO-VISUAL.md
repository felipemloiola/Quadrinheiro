# Refinamento de listas e HQ — 16/11/2026

## Revisão visual e de movimento

| Antes | Depois | Motivo |
| --- | --- | --- |
| Editor branco com ações Material genéricas e capa encostada no botão | Materiais e botões do motor Liquid Glass existente, campos com `inputWellSurface`, separação de 12/20 dp | Hierarquia e linguagem visual do aplicativo |
| Transparência do painel deixava textos anteriores aparecerem | Base legível sob o material óptico | Evitar sobreposição de conteúdo e perda de contraste |
| Ajuste era uma prévia estreita com controles de texto | Estúdio escuro como o editor de perfil, máscara 2:1, grade, centralização, zoom e miniatura | Contexto da imagem e controle direto com os dedos |
| A capa ampliada cobria toda a largura, sem transição | Zoom a partir da posição da capa, margens laterais, contexto desfocado/escurecido e saída reversa | Continuidade espacial; animação em `graphicsLayer` de 240/150 ms |
| Hero era descartado ao sair da área da lista | Arte persistente fora da `LazyColumn`, com alpha/parallax restritos à camada gráfica | Não reiniciar a imagem ao voltar ao topo |
| Miniaturas repetiam fade de 280 ms após recomposição | Imagem em cache desenhada sem repetir o fade | Remover o piscar ao rolar |
| Fade concentrado no final da arte | Máscara gradual com mais pontos ao longo do banner | Integração com o fundo claro/escuro sem uma faixa de corte |

Movimento reduzido usa o `LocalAppMotion` existente. Transparência reduzida mantém superfícies opacas; Android anterior à API 31 mantém o contexto escurecido sem depender do blur de `RenderEffect`. Não há biblioteca nova nem captura adicional do fundo por botão.

## Sinopse

A página agora considera tanto a sinopse salva quanto a resposta online. O nome `synopsisPtBr` não é tratado como prova de idioma: há detecção de italiano para o caso informado. A tradução visível tem prioridade sobre o original e usa o texto completo até o limite de 10.000 caracteres, dividido em partes menores, sem cortar em 550 caracteres. Falha em uma parte não produz uma tradução parcial. Sair da página cancela o trabalho; requisições têm timeout.

Na edição real usada pelo usuário, a sinopse foi observada em português até o trecho final que identifica as edições 7–14. Não se alteraram o texto original armazenado nem o registro de leitura.

## Fontes e edições

A apresentação da busca escolhe um representante com mais dados para versões equivalentes de uma obra/volume, incluindo nomes Absolute Batman / Batman Absoluto / Batman Assoluto. Isso não mistura números distintos, revistas mensais e encadernados, nem reescreve ISBNs e páginas do acervo.

Até três encadernados com metadados incompletos de busca recebem consulta detalhada, concorrente e limitada. A ficha tenta identificar a edição exata primeiro. Para uma edição relacionada da mesma história, usa evidência de série, número de volume, conteúdo reunido ou nomes compartilhados, e identifica a origem como **Créditos de edição relacionada**. Páginas, ISBN e publicação da edição salva são preservados. Não inventa personagens ou equipe.

Na HQ real Batman Absoluto (2024) 2, personagens e equipe foram carregados a partir do recurso relacionado do Metron. O registro permaneceu em 90/160 páginas. Nenhuma alteração no serviço de catálogo ou em regras de produção foi necessária nesta rodada.

## Limites da validação

Builds e testes locais cobrem o editor, persistência, gestos, dados multilíngues, preservação de edições e ampliação. Revisão manual no emulador API 37, tema claro e conta local real; fixtures de testes são isoladas e restauradas. O emulador permanece aberto.

Não houve medição comparativa de GPU, p95/p99, aquecimento ou bateria em aparelho físico. A representação por obra depende da qualidade dos metadados; publicações sem identificação suficiente permanecem separadas. As regras de segurança e o fluxo de registro de leitura não foram modificados.

Resultados: 76 testes unitários aprovados, 24 testes do serviço de catálogo aprovados, 12 testes de interface na rodada ampla e quatro na validação final após os ajustes visuais. O teste novo de ampliação controla o relógio para verificar crescimento a partir da origem e reversão no fechamento; os demais verificam margens, gesto de recorte, confirmação e preservação do registro.
