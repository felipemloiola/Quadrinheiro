# Serviço do catálogo

Worker de leitura usado pelo Quadrinheiro. As credenciais do Comic Vine e da Superhero API ficam nos secrets do Cloudflare. O cliente Android recebe somente metadados e URLs públicas de imagens.

## Operações

| Operação | Resultado |
| --- | --- |
| `search`, `catalog` | Coleções e edições, por páginas limitadas |
| `issue`, `volume` | Sinopse, personagens, equipe, imagens e dados editoriais |
| `character`, `person` | Biografia e aparições registradas na fonte |
| `characters`, `superhero` | Busca de personagens |
| `covers` | Até seis capas reais da edição ou da coleção selecionada |
| `portraits` | Até seis imagens disponíveis do personagem |
| `translated` | Descrição em português via Workers AI |

`assets/characters.json` é um índice de nomes, aliases e URLs de retratos, derivado do [diretório público Superhero API](https://github.com/akabab/superhero-api). O app inclui uma pequena seleção inicial e atualiza o índice em segundo plano. Nenhum retrato é baixado em lote; as imagens carregam conforme aparecem na interface.

## Limites e disponibilidade

- Worker Free e Durable Object SQLite; não depende de Firebase Blaze.
- Cache compartilhado por até três dias, em 256 posições limitadas. Consultas simultâneas idênticas usam a mesma requisição.
- Até 180 chamadas por recurso do Comic Vine por hora e 60 consultas por IP por minuto. Tradução limitada a 100 mil caracteres de origem por dia, usando `@cf/meta/m2m100-1.2b`.
- Conteúdo inexistente não é fabricado. Coleções usam `characters`/`people`; edições usam `character_credits`/`person_credits`. Quando os créditos vêm da série, a resposta informa `creditsScope: series`.
- O banner usa uma imagem incorporada à descrição da obra, quando disponível. A capa principal não é tratada como página interna.
- Capas de edições diferentes da mesma coleção são identificadas como capas da coleção. Resoluções diferentes da mesma imagem são deduplicadas.

## Desenvolvimento

```sh
npm ci
npm test
npx wrangler login
npx wrangler secret put COMIC_VINE_API_KEY
npx wrangler secret put SUPERHERO_API_KEY
npm run deploy
```

Não coloque secrets em `wrangler.jsonc`, assets, arquivos Kotlin ou logs. A configuração de AI e Assets está no arquivo do Worker; testes usam respostas simuladas e não consomem as APIs externas.
