/* Quadrinheiro: painel privado com Google Auth e Firestore nomeado.
   Configuração pública do aplicativo Web; dados privados protegidos por regras server-side.
   Coleção real implantada: /dashboardUsers/{uid}. */
(()=>{
'use strict';
const APP=window.QuadrinheiroPanel,NOTES=()=>window.QuadrinheiroNotes;
const panel=document.getElementById('settings');
if(!APP||!panel)return;
const column=panel.querySelector('.settings-column:last-child');
if(!column)return;
const box=document.createElement('section');
box.className='panel settings-card';
box.setAttribute('aria-labelledby','cloudHeading');
box.innerHTML=
 '<div class="settings-card-head"><span class="settings-card-icon" aria-hidden="true">'+APP.icon('shield')+'</span><div><h2 id="cloudHeading">Seus dados na nuvem</h2><p>A conta Google mantém seus registros no Firestore, sem cópias locais.</p></div></div>'+
 '<div class="cloud-status" id="cloudStatus" role="status" aria-live="polite">Verificando configuração…</div>'+
 '<p class="settings-note" id="cloudAccount"></p>'+
 '<div class="settings-inline-actions cloud-actions">'+
 '<button type="button" class="btn small primary" id="cloudLogin" disabled>Entrar com Google</button>'+
 '<button type="button" class="btn small ghost" id="cloudLogout" hidden>Sair da conta</button>'+
 '</div>'+
 '<div id="cloudDecisions" hidden>'+
 '<p class="settings-note" id="cloudDecisionHint">Escolha como começar. Dados existentes nunca são substituídos automaticamente sem verificação.</p>'+
 '<div class="settings-inline-actions cloud-actions">'+
 '<button type="button" class="btn small primary" id="cloudLoad">Carregar da nuvem</button>'+
 '<button type="button" class="btn small ghost" id="cloudUpload">Enviar dados deste aparelho</button>'+
 '</div></div>'+
 '<p class="settings-note" id="cloudSetup">Conta Google autenticada e dados privados no Firestore. <a class="plain-link" href="CONFIGURAR-SYNC-FIREBASE.md" target="_blank" rel="noopener noreferrer">Informações de segurança ↗</a></p>'+
 '<p class="settings-note">Notas, fotos, tarefas e preferências ficam no Firestore. Sem conexão, aguarde a sincronização antes de fechar a aba.</p>';
column.insertBefore(box,column.firstChild);
const $=id=>document.getElementById(id);
const workspace=$('workspace');
let allowEmails=[],authDenyReason='';
function showGate(mode,message,kind='normal'){
 document.documentElement.dataset.auth=mode;
 if(workspace)workspace.inert=mode!=='authenticated';
 const splash=$('authSplashMessage');
 if(splash&&['checking','loading'].includes(mode))splash.textContent=message||'Carregando seu espaço…';
 if($('loginStatus')){$('loginStatus').textContent=message||'';$('loginStatus').dataset.kind=kind}
 if($('loginGoogle'))$('loginGoogle').disabled=!auth||mode!=='signed-out';
 if($('loginRetry'))$('loginRetry').hidden=!user||mode==='authenticated'||mode==='conflict';
 if($('loginSwitch'))$('loginSwitch').hidden=!user||mode==='authenticated';
}
function showMigration(value,exists){
 if($('loginMigration'))$('loginMigration').hidden=!value;
 if($('loginUseCloud'))$('loginUseCloud').disabled=!exists;
}
const notice=(message,isError=false)=>{
 $('cloudStatus').textContent=message;
 $('cloudStatus').dataset.status=isError?'error':'normal';
 const mode=document.documentElement.dataset.auth;
 if(['checking','loading'].includes(mode)&&$('authSplashMessage'))$('authSplashMessage').textContent=message;
 if(!['authenticated','signed-out','setup'].includes(mode)&&$('loginStatus')){
  $('loginStatus').textContent=message;
  $('loginStatus').dataset.kind=isError?'error':'normal';
 }
};
const CONFIG_URL='cloud-config.json';
const SDK='https://www.gstatic.com/firebasejs/13.2.0/';
const MAX_DOC_BYTES=850000;
const CHUNK_SIZE=150000;
let services=null,auth=null,db=null,user=null,remoteDoc=null,remoteRef=null,active=false,dirty=false;
let subscribed=null,timer=null,uploading=false,queued=false,remoteHash='',lastRemoteVersion='';
let sdk=null,retries=0,applyingRemote=false,pendingPhotoUploads=0,migratingLegacy=false;
function cloudState(){
 const s=APP.getState();
 return {
  schema:1,
  tasks:s.tasks||[],
  decisions:s.decisions||[],
  measurements:s.measurements||[],
  nextActions:s.nextActions||[],
  notes:s.notes||[],
  uiSettings:window.QuadrinheiroSettings?.getPrefs()||{},
  sidebarMode:window.QuadrinheiroSidebar?.getMode()||'expanded'
 };
}
function hash(data){return JSON.stringify(data)}
function safeMessage(err){
 const name=String(err?.code||'');
 if(name==='permission-denied'||name==='firestore/permission-denied')return 'Acesso negado: confira as regras publicadas no banco Firestore do painel.';
 if(name.includes('invalid-api-key')||name.includes('api-key-not-valid'))return 'A chave Web do Firebase foi recusada. Confira a API key atual no Firebase Console em Configurações do projeto → Aplicativo Web.';
 if(name.includes('unauthorized-domain'))return 'Inclua felipemloiola.github.io nos domínios autorizados do Firebase Auth.';
 if(name.includes('popup-blocked'))return 'Permita a janela pop-up do Google neste navegador.';
 if(name.includes('popup-closed-by-user'))return 'A entrada com Google foi cancelada.';
 if(name.includes('quota-exceeded'))return 'O limite do serviço foi atingido.';
 return String(err?.message||'Não foi possível acessar a nuvem.').slice(0,220)
}
function cloudDoc(s){return sdk.doc(db,'dashboardUsers',user.uid,...s)}
function photoMeta(photoId){return cloudDoc(['photos',photoId])}
function photoPart(photoId,index){return cloudDoc(['photos',photoId,'pieces',String(index)])}
function imageIds(data){return new Set((data.notes||[]).flatMap(n=>(n.photos||[]).map(p=>p.id)))}
function validId(v){return typeof v==='string'&&/^[a-zA-Z0-9_-]{5,120}$/.test(v)}
function setDecisionVisible(value){$('cloudDecisions').hidden=!value}
function resetSession(){
 clearTimeout(timer);
 if(subscribed){subscribed();subscribed=null}
 user=null;active=false;dirty=false;queued=false;uploading=false;migratingLegacy=false;remoteDoc=null;remoteRef=null;
 APP.setUserScope?.(null);
 NOTES()?.resetSession?.();
 window.QuadrinheiroSettings?.resetSession?.();
 window.QuadrinheiroSidebar?.resetSession?.();
 $('cloudLogout').hidden=true;
 if($('sidebarSignOut'))$('sidebarSignOut').hidden=true;
 $('cloudLogin').hidden=true;
 $('cloudAccount').textContent='';
 setDecisionVisible(false);showMigration(false,false);
}
async function openFirebase(config){
 if(!sdk){
  const [app,authModule,firestore]=await Promise.all([
    import(SDK+'firebase-app.js'),
    import(SDK+'firebase-auth.js'),
    import(SDK+'firebase-firestore.js')
  ]);
  sdk={...app,...authModule,...firestore};
 }
 allowEmails=(Array.isArray(config.allowedEmails)?config.allowedEmails:[]).map(v=>String(v).trim().toLowerCase()).filter(Boolean);
 services=sdk.initializeApp({
  apiKey:config.apiKey,
  authDomain:config.authDomain,
  projectId:config.projectId,
  appId:config.appId
 },'QuadrinheiroProjectDashboard');
 auth=sdk.getAuth(services);
 // Somente a credencial da conta fica persistida no dispositivo.
 // Os dados do projeto usam cache Firestore em RAM (sem IndexedDB).
 await sdk.setPersistence(auth,sdk.browserLocalPersistence);
 if(auth.authStateReady)await auth.authStateReady();
 db=config.databaseId&&config.databaseId!=='(default)'?
   sdk.getFirestore(services,config.databaseId):sdk.getFirestore(services);
 $('cloudLogin').disabled=false;
 showGate('checking','Verificando sua sessão Google…');
 sdk.onAuthStateChanged(auth,async signed=>{
  if(!signed){
   resetSession();const reason=authDenyReason;authDenyReason='';
   showGate('signed-out',reason||'Entre com sua conta Google para continuar.',reason?'error':'normal');
   return
  }
  let providerIsGoogle=signed.providerData?.some(p=>p.providerId==='google.com')===true;
  try{
   const token=await sdk.getIdTokenResult(signed);
   providerIsGoogle=providerIsGoogle||token?.signInProvider==='google.com'||token?.claims?.firebase?.sign_in_provider==='google.com';
  }catch(err){showGate('error',safeMessage(err),'error');return}
  if(!providerIsGoogle||!signed.emailVerified){
   authDenyReason='Este painel exige uma conta Google com e-mail verificado.';
   await sdk.signOut(auth);return
  }
  if(allowEmails.length&&!allowEmails.includes(String(signed.email||'').toLowerCase())){
   authDenyReason='Esta conta Google não está autorizada a acessar o painel.';await sdk.signOut(auth);return
  }
  user=signed;APP.setUserScope?.(user.uid);
  $('cloudLogout').hidden=false;$('cloudLogin').hidden=true;
  if($('sidebarSignOut'))$('sidebarSignOut').hidden=false;
  $('cloudAccount').textContent='Conta: '+(user.email||'Google')+' · seus dados ficam restritos ao seu UID.';
  showGate('loading','Carregando sua área de trabalho…');
  notice('Conferindo registros privados…');
  try{await inspectRemote()}
  catch(err){showGate('error',safeMessage(err),'error');notice(safeMessage(err),true)}
 });
 sdk.getRedirectResult(auth).catch(err=>{if(!user)showGate('error',safeMessage(err),'error')});
}
// Transferência de dados existentes de versões antigas, sem perder notas ou fotos.
// Se já há dados na nuvem, a versão antiga vira um backup remoto separado.
async function migrateLegacy(legacy,remoteExists){
 const images=await NOTES().readLegacyImages(legacy.data);
 const data={...legacy.data,uiSettings:legacy.uiSettings||{},sidebarMode:legacy.sidebarMode||'expanded'};
 if(new TextEncoder().encode(hash(data)).length>MAX_DOC_BYTES)throw Error('Backup antigo muito grande para a migração automática.');
 active=true;migratingLegacy=true;
 try{
  for(const image of images)await uploadImage(image);
  if(remoteExists){
   const backup=sdk.doc(db,'dashboardUsers',user.uid,'legacyBackups','import-'+Date.now());
   await sdk.setDoc(backup,{ownerId:user.uid,state:data,migratedAt:sdk.serverTimestamp()});
  }else{
   APP.replaceCloudState(data);
   await NOTES().upsertCloudImages(images);
   if(legacy.uiSettings)window.QuadrinheiroSettings?.importPrefs(legacy.uiSettings);
   if(legacy.sidebarMode)window.QuadrinheiroSidebar?.setMode(legacy.sidebarMode);
   dirty=true;
   await push();
   if(dirty)throw Error('Não foi possível concluir a migração antiga para o Firestore.');
  }
  await NOTES().clearLegacyImages();
  APP.clearLegacySnapshot();
  return !remoteExists
 }finally{
  // A área privada só será desbloqueada depois de concluir a leitura remota.
  migratingLegacy=false;active=false;
 }
}
async function inspectRemote(){
 if(!user)return;
 const expectedUid=user.uid;
 const ref=sdk.doc(db,'dashboardUsers',expectedUid);
 const snapshot=await sdk.getDocFromServer(ref);
 if(!user||user.uid!==expectedUid)return;
 remoteRef=ref;
 remoteDoc=snapshot.exists()?snapshot.data().state:null;
 const legacy=APP.getLegacySnapshot?.();
 let migrated=false,migrationWarning='';
 if(legacy){
  try{migrated=await migrateLegacy(legacy,!!remoteDoc)}
  catch(err){
   migrationWarning='Os registros antigos ainda estão neste dispositivo porque a migração falhou: '+safeMessage(err);
   if(!remoteDoc){showGate('error',migrationWarning,'error');notice(migrationWarning,true);return}
  }
 }
 if(!user||user.uid!==expectedUid)return;
 if(remoteDoc){
  await hydrate(remoteDoc);
  activate(remoteDoc,'Dados carregados diretamente do Firestore.');
  if(migrationWarning)notice(migrationWarning,true);
  return
 }
 if(migrated){
  activate(cloudState(),'Registros antigos migrados com sucesso para o Firestore.');
  return
 }
 await createCloudWorkspace();
}
function activate(data,description){
 if(!user||!remoteRef)return;
 active=true;dirty=false;retries=0;remoteHash=hash(data);remoteDoc=data;
 setDecisionVisible(false);showMigration(false,false);
 showGate('authenticated','');
 notice(description||'Nuvem conectada e sincronizando.');
 if(subscribed)subscribed();
 subscribed=sdk.onSnapshot(remoteRef,async snapshot=>{
  if(!active||!snapshot.exists()||snapshot.metadata.hasPendingWrites||snapshot.metadata.fromCache||dirty||uploading)return;
  const incoming=snapshot.data().state;
  if(!incoming||hash(incoming)===remoteHash)return;
  try{await hydrate(incoming);remoteHash=hash(incoming);remoteDoc=incoming;notice('Alterações recebidas de outro dispositivo.')}
  catch(err){notice(safeMessage(err),true)}
 },err=>notice(safeMessage(err),true));
}
async function hydrate(data){
 if(!data||!Array.isArray(data.tasks)||!Array.isArray(data.notes)||!Array.isArray(data.decisions)||!Array.isArray(data.measurements))throw Error('O backup da nuvem é inválido.');
 applyingRemote=true;
 try{
  await downloadPhotos(data);
  APP.replaceCloudState(data);
  if(data.uiSettings)window.QuadrinheiroSettings?.importPrefs(data.uiSettings);
  if(['collapsed','expanded'].includes(data.sidebarMode))window.QuadrinheiroSidebar?.setMode(data.sidebarMode);
 }finally{applyingRemote=false}
}
async function downloadPhotos(data){
 const refs=imageIds(data);
 if(!refs.size)return;
 const existing=await NOTES().getLocalImageRecords();
 const have=new Set(existing.map(x=>x.id));
 const missing=[...refs].filter(id=>!have.has(id));
 const imported=[];
 for(const id of missing){
  if(!validId(id))throw Error('Identificador de imagem inválido.');
  const meta=await sdk.getDocFromServer(photoMeta(id));
  if(!meta.exists())throw Error('Uma foto ainda não foi publicada. Tente sincronizar novamente.');
  const count=meta.data().parts;
  if(!Number.isInteger(count)||count<1||count>100)throw Error('Quantidade de partes da imagem inválida.');
  const values=[];
  for(let i=0;i<count;i++){
   const part=await sdk.getDocFromServer(photoPart(id,i));
   if(!part.exists())throw Error('Parte de imagem ausente na nuvem.');
   values.push(part.data().value||'');
  }
  const dataUrl=values.join('');
  if(!/^data:image\/(?:jpeg|png|webp|gif);base64,[A-Za-z0-9+/=]+$/.test(dataUrl))throw Error('Formato de imagem inválido.');
  imported.push({id,noteId:'',name:meta.data().name||'Imagem',dataUrl});
 }
 if(imported.length)await NOTES().upsertCloudImages(imported);
}
async function uploadPhotos(data){
 const refs=imageIds(data);
 if(!refs.size)return;
 const records=await NOTES().getLocalImageRecords();
 const byId=new Map(records.map(x=>[x.id,x]));
 for(const id of refs){
  if(!validId(id))throw Error('Identificador de imagem inválido.');
  const meta=photoMeta(id);
  if((await sdk.getDocFromServer(meta)).exists())continue;
  const local=byId.get(id);
  if(!local)throw Error('Uma foto existe na nota, mas não neste aparelho. Carregue primeiro os dados da nuvem.');
  const payload=local.dataUrl;
  const chunks=[];
  for(let i=0;i<payload.length;i+=CHUNK_SIZE)chunks.push(payload.slice(i,i+CHUNK_SIZE));
  if(chunks.length<1||chunks.length>100)throw Error('Imagem excedeu o limite para sincronização.');
  for(let i=0;i<chunks.length;i++){
   await sdk.setDoc(photoPart(id,i),{ownerId:user.uid,value:chunks[i]});
  }
  await sdk.setDoc(meta,{ownerId:user.uid,name:String(local.name||'Imagem').slice(0,160),parts:chunks.length,updatedAt:sdk.serverTimestamp()});
 }
}
// Anexos chegam ao Firestore antes de aparecerem como anexados à nota.
async function uploadImage(record){
 if(!active||!user||!remoteRef)throw Error('Entre com Google e aguarde o carregamento do Firestore.');
 if(pendingPhotoUploads>0&&record?.id===undefined)throw Error('Selecione uma imagem válida.');
 if(!record||!validId(record.id)||typeof record.dataUrl!=='string'||!/^data:image\/(?:jpeg|png|webp|gif);base64,[A-Za-z0-9+/=]+$/.test(record.dataUrl))throw Error('Imagem inválida.');
 pendingPhotoUploads++;
 try{
  const photoId=record.id;
  const current=await sdk.getDocFromServer(photoMeta(photoId));
  if(current.exists())return;
  const chunks=[];
  for(let i=0;i<record.dataUrl.length;i+=CHUNK_SIZE)chunks.push(record.dataUrl.slice(i,i+CHUNK_SIZE));
  if(!chunks.length||chunks.length>100)throw Error('Imagem excede o tamanho suportado pelo Firestore.');
  for(let i=0;i<chunks.length;i++)
   await sdk.setDoc(photoPart(photoId,i),{ownerId:user.uid,value:chunks[i]});
  await sdk.setDoc(photoMeta(photoId),{ownerId:user.uid,name:String(record.name||'Imagem').slice(0,160),parts:chunks.length,updatedAt:sdk.serverTimestamp()});
 }finally{pendingPhotoUploads--}
}
async function deleteImage(id){
 if(!active||!user||!validId(id))throw Error('Não foi possível remover a imagem no Firestore.');
 pendingPhotoUploads++;
 try{
  const meta=await sdk.getDocFromServer(photoMeta(id));
  if(!meta.exists())return;
  const count=meta.data().parts;
  if(!Number.isInteger(count)||count<1||count>100)throw Error('Metadados da foto inválidos.');
  for(let i=0;i<count;i++)await sdk.deleteDoc(photoPart(id,i));
  await sdk.deleteDoc(photoMeta(id));
 }finally{pendingPhotoUploads--}
}
async function push(){
 if(!active||!user||!remoteRef)return;
 if(uploading){queued=true;return}
 uploading=true;
 const payload=cloudState();
 try{
  notice('Salvando alterações privadas…');
  const raw=hash(payload);
  if(new TextEncoder().encode(raw).length>MAX_DOC_BYTES)throw Error('Registros muito grandes para um documento. Exporte um backup JSON.');
  await uploadPhotos(payload);
  await sdk.setDoc(remoteRef,{ownerId:user.uid,state:payload,updatedAt:sdk.serverTimestamp()});
  remoteHash=hash(payload);remoteDoc=payload;
  retries=0;
  dirty=hash(cloudState())!==remoteHash;
  notice(dirty?'Novas alterações aguardando salvamento.':'Tudo salvo na nuvem.');
 }catch(err){
  dirty=true;retries++;notice(safeMessage(err),true);
 }finally{
  uploading=false;
  if(queued||(dirty&&retries<3)){queued=false;if(active)timer=setTimeout(push,4000)}
 }
}
function notifyLocalChange(){
 if(!active||applyingRemote||migratingLegacy)return;
 dirty=true;retries=0;
 clearTimeout(timer);
 timer=setTimeout(push,650);
 notice('Alterações locais aguardando sincronização…');
}
async function createCloudWorkspace(){
 if(!user||!remoteRef)return;
 active=true;dirty=true;
 notice('Criando espaço privado no Firestore…');
 await push();
 if(!dirty)activate(cloudState(),'Espaço criado no Firestore.');
 else{
  active=false;
  showGate('error','Não foi possível criar seus dados na nuvem. Verifique a conexão e tente novamente.','error');
 }
}
async function googleSignIn(){
 if(!sdk||!auth)return;
 showGate('loading','Abrindo sua conta Google…');
 try{
  const provider=new sdk.GoogleAuthProvider();
  provider.setCustomParameters({prompt:'select_account'});
  await sdk.signInWithPopup(auth,provider);
 }catch(err){
  if(String(err?.code||'').includes('popup-blocked')&&sdk.signInWithRedirect){
   await sdk.signInWithRedirect(auth,new sdk.GoogleAuthProvider());
  }else showGate('signed-out',safeMessage(err),'error')
 }
}
$('loginGoogle')?.addEventListener('click',googleSignIn);
$('loginRetry')?.addEventListener('click',async()=>{
 if(!user)return;showGate('loading','Conectando novamente…');
 try{await inspectRemote()}catch(err){showGate('error',safeMessage(err),'error')}
});
$('loginSwitch')?.addEventListener('click',async()=>{if(auth)await sdk.signOut(auth)});
$('cloudLogin').addEventListener('click',googleSignIn);
// A mesma saída funciona em Ajustes e na barra lateral. Nenhum dado é apagado.
async function signOutFromDashboard(event){
 event?.preventDefault?.();
 if(!auth||!user){
  showGate('signed-out','Sessão encerrada. Entre novamente com Google.');
  return
 }
 const controls=[$('cloudLogout'),$('sidebarSignOut')].filter(Boolean);
 controls.forEach(button=>button.disabled=true);
 try{
  // A saída nunca deve parecer sem resposta: tentativa rápida de salvar,
  // seguida de confirmação explícita caso haja trabalho não sincronizado.
  if(active&&dirty&&!uploading&&pendingPhotoUploads===0){
   clearTimeout(timer);
   await Promise.race([push(),new Promise(resolve=>setTimeout(resolve,5000))]);
  }
  if(active&&(dirty||uploading||pendingPhotoUploads>0)){
   const ok=window.confirm('Existem alterações ainda não confirmadas na nuvem. Sair agora descartará essas alterações. Deseja sair mesmo assim?');
   if(!ok){notice('Saída cancelada: aguarde o salvamento dos registros.',true);return}
  }
  await sdk.signOut(auth);
  // Garante o bloqueio imediato mesmo que o observador do Firebase demore.
  resetSession();
  showGate('signed-out','Você saiu da conta. Entre com Google para continuar.');
 }catch(err){
  const message='Não foi possível sair da conta: '+safeMessage(err);
  notice(message,true);
  window.alert(message);
 }finally{
  controls.forEach(button=>button.disabled=false);
 }
}
$('cloudLogout').addEventListener('click',signOutFromDashboard);
$('sidebarSignOut')?.addEventListener('click',signOutFromDashboard);
window.addEventListener('beforeunload',event=>{
 if(active&&(dirty||uploading||pendingPhotoUploads)){
  event.preventDefault();event.returnValue='';
 }
});
window.addEventListener('online',()=>{if(active&&dirty){clearTimeout(timer);timer=setTimeout(push,500)}});
document.addEventListener('visibilitychange',()=>{if(!document.hidden&&active&&dirty){clearTimeout(timer);timer=setTimeout(push,750)}});
window.QuadrinheiroCloud={notifyLocalChange,uploadImage,deleteImage,getStatus:()=>({configured:!!auth,connected:active,pending:dirty||uploading||pendingPhotoUploads>0,user:user?.email||null})};
(async()=>{
 try{
  const response=await fetch(CONFIG_URL+'?v=20261109c',{cache:'no-store'});
  if(!response.ok)throw Error('Não foi possível ler cloud-config.json');
  const conf=await response.json();
  if(!conf.enabled||!conf.apiKey||!conf.projectId||!conf.appId||!conf.authDomain){
   showGate('setup','A configuração do Firebase Web ainda não está completa.','error');
   notice('Sincronização privada ainda não configurada. O painel continua bloqueado.',true);
   return
  }
  if(!Array.isArray(conf.allowedEmails)||!conf.allowedEmails.length){
   showGate('setup','Cadastre seu endereço Google autorizado em cloud-config.json antes de habilitar o acesso.','error');
   notice('A lista de contas Google autorizadas está vazia.',true);
   return
  }
  await openFirebase(conf);
 }catch(err){showGate('setup',safeMessage(err),'error');notice(safeMessage(err),true)}
})();
})();