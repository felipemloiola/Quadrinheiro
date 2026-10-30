# APK assinado e atualizações

## Configuração única
O backup PRIVADO da assinatura foi entregue separadamente. Nunca envie a chave, a senha ou o Base64 para commits, issues, logs ou Releases.

No GitHub: Settings > Secrets and variables > Actions > New repository secret:
- ANDROID_KEYSTORE_BASE64: conteúdo do arquivo com esse nome no backup privado.
- ANDROID_KEYSTORE_PASSWORD: conteúdo do arquivo de senha no backup privado.

A chave tem alias upload e formato PKCS12. Preserve uma cópia segura e permanente do backup. O certificado público esperado está em release-signing-sha256.txt. Não troque a chave depois da primeira instalação.

Opcional: GOOGLE_SERVICES_JSON_BASE64, contendo o google-services.json do aplicativo no Firebase codificado em Base64. Sem essa configuração, não considere o login Google e a sincronização em nuvem prontos para produção. Configure também as impressões digitais do certificado de release no Firebase/Google; não use uma conta de serviço dentro do APK.

## Primeiro APK
Depois dos Secrets, em Actions execute Build signed APK and publish update > Run workflow. O workflow falha explicitamente se faltarem Secrets, se a chave estiver errada, se testes/compilação falharem ou se a verificação da assinatura falhar. Apenas depois dessas etapas publica uma Release pública com quadrinheiro.apk, update.json e SHA256SUMS.txt.

O primeiro run pode falhar antes dos Secrets; execute um novo run após cadastrá-los. APK não foi gerado apenas por publicar estes arquivos.

## Integração reproduzível
Antes de compilar, o workflow executa .github/scripts/configure_android_release.py. Ele integra o aviso de atualização ao tema do app e o botão à raiz de Ajustes, sem mudar gestos, tab bar, dados ou assinatura de debug do AI Studio. Essas integrações são aplicadas no workspace de build, sem commit automático na main. Para builds locais com o atualizador, execute esse mesmo script antes do Gradle.

## Próximas versões
Mudanças em app/ na main e execução manual acionam o workflow. O versionCode é 1000 + github.run_number; o versionName é 1.0.run_number. Não recrie o workflow reiniciando a numeração. A publicação recusa tags já existentes e versões anteriores.

O APK de release verifica novas versões ao abrir o aplicativo, no máximo a cada seis horas, e oferece verificação manual em Ajustes. Debug/prévia não recebe atualizações de release. Adiar silencia a mesma versão por 24 horas; verificação manual ignora esse adiamento. Não há serviço rodando enquanto o app está fechado.

O aviso abre o APK oficial no navegador. Depois do download, o usuário abre o arquivo e autoriza a instalação no Android; não há promessa de instalação silenciosa. O atualizador só aceita metadados do repositório esperado, applicationId/certificado esperados e versionCode maior. A assinatura do APK é verificada no CI e novamente pelo Android ao instalar. O checksum é publicado para conferência externa; o app não valida os bytes baixados pelo navegador.

## Validação necessária
- Build de release e testes do workflow concluídos.
- Primeira instalação em aparelho real.
- Verificação manual sem atualização disponível.
- Publicar a próxima versão e confirmar aviso automático/manual.
- Instalar por cima e confirmar preservação da coleção, sessão e preferências.
- Validar comportamento offline e abrir/fechar o aviso.
- Validar Firebase/Google com o certificado de release, se configurado.
