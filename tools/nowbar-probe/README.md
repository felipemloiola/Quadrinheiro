# Teste isolado da Now Bar

Este módulo de diagnóstico usa `com.aistudio.quadrinheiro.nowbarprobe`, outra assinatura e outro UID. Não substitui o Quadrinheiro, não acessa seu acervo, não usa Firebase/rede e não pede sobreposição, acesso a notificações de outros apps ou exclusão de dados. Compila o mesmo builder público de notificações do app. O adaptador Samsung experimental existe somente neste módulo, para comparação histórica.

```powershell
gradle -PincludeNowBarProbe=true :nowbar-probe:assembleDebug
```

Antes de instalar, confira com `aapt2 dump badging` se o pacote é **exatamente** o pacote de teste acima. Use `adb -s <dispositivo> install -t <apk>` sem `-r` na primeira instalação, para impedir substituição de um app existente.

Permita notificações e compare os botões **Teste Android padrão** e **Teste Samsung Now Bar** em Android 17. **Teste Samsung simples** omite `ProgressStyle` e a solicitação nativa de promoção, para isolar a extensão. Os dados são fictícios (24/120 páginas). **Encerrar teste** cancela a notificação do módulo. A identificação do pacote, chaves de assinatura e preferências do app de produção permanecem independentes.

No S23/One UI 9/Android 17, a notificação padrão apareceu na Now Bar após ativar **Configurações → Opções do desenvolvedor → Mais configurações → Apps → Notif. ao vivo para todos apps**. A hierarquia de acessibilidade confirmou título e 20% dentro de `com.samsung.android.app.aodservice:id/nowbar_card_container`, e o usuário confirmou visualmente. Antes de ativar esse controle, os formatos padrão e experimental apareciam como notificações comuns. As flags privadas não resolveram a restrição do firmware.

Esse controle é global e pode afetar outros apps compatíveis. Não o modifique sem autorização explícita; restaure o valor inicial quando o usuário autorizar apenas um teste temporário. No teste inicial ele foi restaurado e, após nova autorização, deixado ativado. O app não tenta modificar essa configuração nem trata permissão Android como confirmação de suporte Samsung.

A flag Samsung é uma extensão não documentada: pode ser recusada pelo sistema ou mudar após uma atualização. O adaptador mantém a notificação padrão quando isso ocorrer. Não altera a política de APIs ocultas do aparelho e não usa um pacote de outro aplicativo.
