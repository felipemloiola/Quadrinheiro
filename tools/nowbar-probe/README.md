# Teste isolado da Now Bar

Este módulo de diagnóstico usa `com.aistudio.quadrinheiro.nowbarprobe`, outra assinatura e outro UID. Não substitui o Quadrinheiro, não acessa seu acervo, não usa Firebase/rede e não pede sobreposição, acesso a notificações de outros apps ou exclusão de dados. Compila o mesmo adaptador Samsung do app.

```powershell
gradle -PincludeNowBarProbe=true :nowbar-probe:assembleDebug
```

Antes de instalar, confira com `aapt2 dump badging` se o pacote é **exatamente** o pacote de teste acima. Use `adb -s <dispositivo> install -t <apk>` sem `-r` na primeira instalação, para impedir substituição de um app existente.

Permita notificações e compare os botões **Teste Android padrão** e **Teste Samsung Now Bar** em Android 17. **Teste Samsung simples** omite `ProgressStyle` e a solicitação nativa de promoção, para isolar a extensão. Os dados são fictícios (24/120 páginas). **Encerrar teste** cancela a notificação do módulo. A identificação do pacote, chaves de assinatura e preferências do app de produção permanecem independentes.

No teste físico inicial em S23/One UI 9/Android 17, os dois primeiros formatos apareceram como notificações comuns; o formato simples não ficou visível para o usuário. Não há garantia de Now Bar. Conteúdo permitido na tela bloqueada e flags aceitas no framework não bastaram para demonstrar suporte.

A flag Samsung é uma extensão não documentada: pode ser recusada pelo sistema ou mudar após uma atualização. O adaptador mantém a notificação padrão quando isso ocorrer. Não altera a política de APIs ocultas do aparelho e não usa um pacote de outro aplicativo.
