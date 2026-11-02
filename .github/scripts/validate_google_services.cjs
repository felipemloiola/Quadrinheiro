const fs = require('node:fs');
const { X509Certificate } = require('node:crypto');

const PACKAGE = 'com.aistudio.quadrinheiro.lqglas';
const WEB_CLIENT = /^\d+-[a-zA-Z0-9_-]+\.apps\.googleusercontent\.com$/;

function validateGoogleServices(config, releaseSha1) {
  const project = config?.project_info;
  if (!project || !/^[a-z][a-z0-9-]{4,28}[a-z0-9]$/.test(project.project_id || '') ||
      !/^\d+$/.test(project.project_number || '')) {
    throw new Error('Configuração Firebase inválida: use o google-services.json do projeto, não o ID do banco Firestore.');
  }
  if (project.project_id !== 'gen-lang-client-0375936605') {
    throw new Error('A configuração deve pertencer ao projeto gen-lang-client-0375936605, usado pelo banco do Quadrinheiro.');
  }
  const matches = (config.client || []).filter(client =>
    client.client_info?.android_client_info?.package_name === PACKAGE);
  if (matches.length !== 1) throw new Error(`Cadastre exatamente um app Android com o pacote ${PACKAGE}.`);
  const client = matches[0];
  if (!client.client_info.mobilesdk_app_id?.startsWith(`1:${project.project_number}:android:`) ||
      !client.api_key?.some(key => /^AIza[\w-]{35}$/.test(key.current_key || ''))) {
    throw new Error('O app Firebase não possui App ID/API key válidos no google-services.json.');
  }
  const oauth = client.oauth_client || [];
  const web = oauth.filter(item => item.client_type === 3);
  if (web.length !== 1 || !WEB_CLIENT.test(web[0].client_id || '')) {
    throw new Error('Ative Google em Firebase Authentication e baixe novamente google-services.json: falta um cliente OAuth Web (client_type 3).');
  }
  if (releaseSha1) {
    const normalize = value => (value || '').replaceAll(':', '').toUpperCase();
    if (!oauth.some(item => item.client_type === 1 &&
        item.android_info?.package_name === PACKAGE &&
        normalize(item.android_info.certificate_hash) === normalize(releaseSha1))) {
      throw new Error('Cadastre o SHA-1 da assinatura permanente de release no app Firebase e baixe novamente google-services.json.');
    }
  }
  return { projectId: project.project_id, packageName: PACKAGE };
}

if (require.main === module) {
  try {
    const path = process.argv[2] || 'app/google-services.json';
    if (!fs.existsSync(path)) throw new Error('Cadastre GOOGLE_SERVICES_JSON_BASE64 nos GitHub Actions Secrets com a configuração real do Firebase.');
    const certPath = process.argv[3];
    const sha1 = certPath ? new X509Certificate(fs.readFileSync(certPath)).fingerprint : undefined;
    validateGoogleServices(JSON.parse(fs.readFileSync(path, 'utf8')), sha1);
    console.log('Configuração Firebase/OAuth Web e assinatura Android verificadas.');
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}

module.exports = { validateGoogleServices };
