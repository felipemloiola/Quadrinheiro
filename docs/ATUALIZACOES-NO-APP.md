# Atualizações dentro do aplicativo

Ao tocar em Atualizar, o Quadrinheiro baixa o APK pelo DownloadManager do Android,
mostra o progresso e guarda o identificador do download. Reabrir o aplicativo
retoma o acompanhamento do mesmo download, inclusive se ele terminou em segundo
plano. Não há abertura de navegador para baixar o arquivo.

Antes da instalação, o aplicativo copia o APK para seu cache privado e confere
SHA-256, tamanho máximo, applicationId, versionCode e o certificado permanente
de release. Apenas esse arquivo validado é compartilhado com o instalador do
Android por FileProvider, com permissão temporária de leitura.

O Android exige confirmação para instalar. Na primeira vez, pode exigir também
a permissão para o Quadrinheiro instalar aplicativos; ao retornar dessa tela, o
app continua a instalação. Cancelar a instalação permite tentar novamente. O
cancelamento do download remove o arquivo e o registro salvo. A assinatura e o
applicationId permanecem os mesmos, preservando os dados na atualização.

A versão anterior ainda contém o fluxo que abre o navegador. É necessário instalar
uma vez a versão que introduz este atualizador; somente depois dela o novo fluxo
pode ser usado para as próximas versões.

`UpdateVerifierTest` cobre integridade, limite de tamanho, identidade, versão e
assinatura. `InAppDownloadStateTest` cobre persistência e cancelamento.
`InAppUpdateIntegrationTest` usa o DownloadManager real no Android para baixar um
APK de release imutável, validar seu hash e certificado e verificar a URI de
instalação. O teste não instala o APK por cima do aplicativo de testes.
