# Configuração do login Google

O banco `ai-studio-android-quadrinh-d0141987-7d2c-491d-b885-fc910c102b71`
é um banco nomeado do Firestore. Esse nome não é um project ID nem um cliente
OAuth e deve continuar exclusivamente em `firestore_database_id`.

No projeto Firebase `gen-lang-client-0375936605`, usado pelo AI Studio:

1. Selecione ou cadastre o app Android `com.aistudio.quadrinheiro.lqglas`.
2. Registre o SHA-1 e SHA-256 do certificado permanente de release. O workflow
   verifica o certificado contra `docs/release-signing-sha256.txt`. Registre
   também os certificados de debug se desejar testar login no build local.
3. Ative Google em Authentication > Sign-in method, escolhendo o e-mail de suporte.
4. Baixe novamente `google-services.json` e coloque-o em `app/` localmente.
   O arquivo deve conter o cliente OAuth Web (`client_type: 3`), usado como
   `serverClientId`, e o cliente Android para o pacote/certificado de release.
5. Cadastre seu conteúdo em base64 no secret `GOOGLE_SERVICES_JSON_BASE64` do
   repositório. Não publique chaves de conta de serviço nem tokens de acesso.

O plugin Google Services gera `default_web_client_id`, `google_app_id` e os
demais recursos Firebase a partir desse arquivo. O arquivo local é ignorado
pelo Git. O workflow agora rejeita configurações ausentes, pacote incorreto,
cliente Web ausente ou certificado Android diferente antes de publicar APKs.

Validação local (Node 20+):

```sh
node --test .github/scripts/validate_google_services.test.cjs
node .github/scripts/validate_google_services.cjs app/google-services.json
```

Uma validação de arquivo não confirma que o provedor está ativo no servidor,
que a configuração de consentimento OAuth está publicada, nem que há permissão
para usuários externos. A validação final exige entrar com uma conta Google no
APK assinado. O próprio usuário deve selecionar/autenticar sua conta.

Referências oficiais: [Firebase Auth Android](https://firebase.google.com/docs/auth/android/google-signin)
e [Google Services](https://developers.google.com/android/guides/google-services-plugin).
