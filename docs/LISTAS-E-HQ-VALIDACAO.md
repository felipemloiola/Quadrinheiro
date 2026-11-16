# Listas e página da HQ — 16/11/2026

## Comportamento

- O destaque mantém o banner grande aprovado, mas limita as miniaturas a 64 × 96 dp. Uma única HQ não aumenta a altura da coleção.
- Painéis respeitam a barra de status e mantêm ações no cabeçalho. Adicionar HQs no detalhe abre diretamente a seleção.
- Escolher capa é um botão. Escolha da HQ e ajuste são etapas sequenciais; arrastar e pinch alteram um rascunho, aplicado somente ao confirmar. Sliders permanecem como alternativa acessível.
- Seletores usam uma única camada de rótulos. O indicador preserva o motor Backdrop, sem capturar novamente os próprios textos.
- A página da HQ consulta detalhes automaticamente, mesmo com dados básicos locais. A identificação por busca exige uma edição única compatível; resultados ambíguos não recebem créditos de outra publicação.
- O banner prefere arte horizontal da própria edição. O fallback usa análise visual limitada a uma amostra de 96 × 192 pixels, cache de 32 focos e processamento fora da thread principal. É uma heurística de composição, sem reconhecimento semântico.
- A imagem desaparece por uma máscara de transparência gradual, sem vinheta branca sobreposta. A rolagem revela discretamente a atmosfera existente; movimento e transparência reduzidos são respeitados.
- Tocar a capa abre uma imagem inteira sobre fundo preto, com pinch, deslocamento e botão de fechar.
- O registro de leitura existente foi preservado.

## Listas públicas

Publicação exige conta Firebase/Google, confirmação no editor e salvamento. O login por e-mail existente é local; seus dados continuam nessa conta e não são migrados ao conectar outra conta.

A transação salva a lista privada e uma cópia pública separada em `publicLists`. Notas dos itens e revisões privadas não entram nessa cópia; o modelo não contém progresso de leitura. Tornar privada ou excluir retira a cópia da consulta pública na mesma transação. Backups importados ficam privados.

Consultas públicas exigem `published == true` e limite de até 20 documentos. Escrita exige autenticação, propriedade e revisão correspondente ao documento privado. Documentos privados continuam acessíveis apenas ao dono.

As regras foram publicadas com autorização explícita no banco nomeado do projeto, e a versão ativa foi comparada integralmente com `firestore.rules`. Nenhuma lista real foi publicada automaticamente.

## Catálogo e imagens

A [documentação oficial do Metron](https://github.com/Metron-Project/metron/blob/master/api/README.md) descreve capas variantes, sem um campo de prévia de páginas internas. Imagens associadas de uma edição podem servir como arte, mas não são identificadas como páginas da história. Não há promessa de informações que a fonte não fornece.

## Verificação

- 24 testes do serviço de catálogo aprovados.
- 47 testes unitários e de regras aprovados, incluindo Robolectric API 24/34 e Firebase Auth/Firestore emulados no projeto demo.
- 12 testes de interface aprovados no emulador API 37: seleção múltipla, tamanho do card, pinch e aplicação, persistência por conta, cancelamento e capa ampliada.
- APK do app e APK de testes compilados; atualização no emulador sem limpar dados. Emulador mantido aberto.
- Revisão visual manual do tema claro, lista real existente, detalhe e capa ampliada. Sem teste físico no S23 nesta rodada.

Não foram executadas medições comparativas de p95/p99, consumo ou aquecimento em aparelhos físicos. O caminho novo não adiciona captura Backdrop nem animação contínua, mas isso não substitui essas medições. Instrumentação nativa em Android 7/12/13 permanece fora desta rodada.

## Reversão

As listas antigas decodificam como privadas. Uma reversão do aplicativo após publicar listas exige retirar primeiro as cópias públicas; o app antigo desconhece essa coleção. A cópia anterior das regras foi preservada localmente durante a publicação. Não revogar regras públicas deixando publicações ativas sem avaliar a retirada dessas cópias.
