/* Quadrinheiro: sync opcional, privado, entre navegadores via Firebase Auth + Firestore.
   Não possui tokens de conta ou credenciais administrativas. O Firebase Web config é público.
   Requer ativação da configuração web, Google sign-in e deployment de regras. */
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
 '<div class="settings-card-head"><span class="settings-card-icon" aria-hidden="true">'+APP.icon('shield')+'</span><div><h2 id="cloudHeading">Sincronização entre dispositivos</h2><p>Salve alterações privadas e continue em outro aparelho.</p></div></div>'+
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
 '<p class="settings-note" id="cloudSetup">Para ativar, configure o aplicativo Firebase Web, autenticação Google e regras de acesso. <a class="plain-link" href="CONFIGURAR-SYNC-FIREBASE.md" target="_blank" rel="noopener noreferrer">Instruções ↗</a></p>'+
 '<p class="settings-note">As fotos também são sincronizadas, em partes privadas no Firestore. Dados e configurações locais permanecem disponíveis mesmo sem conexão.</p>';
column.insertBefore(box,column.firstChild);
const $=id=>document.getElementById(id);
const notice=(message,isError=false)=>{
 $('cloudStatus').textContent=message;
 $('cloudStatus').dataset.status=isError?'error':'normal';
};
const CONFIG_URL='cloud-config.json';
const SDK='https://www.gstatic.com/firebasejs/13.2.0/';
const LAST_PREFIX='quadrinheiro-cloud-snapshot-';
const MAX_DOC_BYTES=850000;
const CHUNK_SIZE=150000;
let services=null,auth=null,db=null,user=null,remoteDoc=null,remoteRef=null,active=false,dirty=false;
let subscribed=null,timer=null,uploading=false,queued=false,remoteHash='',lastRemoteVersion='';
let sdk=null;
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
 if(name==='permission-denied'||name==='firestore/permission-denied')return 'Acesso negado: publique as regras privadas do Firestore antes de sincronizar.';
 if(name.includes('unauthorized-domain'))return 'Inclua felipemloiola.github.io nos domínios autorizados do Firebase Auth.';
 if(name.includes('popup-blocked'))return 'Permita a janela pop-up do Google neste navegador.';
 if(name.includes('popup-closed-by-user'))return 'A entrada com Google foi cancelada.';
 if(name.includes('quota-exceeded'))return 'O limite do serviço foi atingido.';
 return String(err?.message||'Não foi possível acessar a nuvem.').slice(0,220)
}
function cloudDoc(s){return sdk.doc(db,'projectDashboards',user.uid,...s)}
function photoMeta(photoId){return cloudDoc(['photos',photoId])}
function photoPart(photoId,index){return cloudDoc(['photos',photoId,'pieces',String(index)])}
function imageIds(data){return new Set((data.notes||[]).flatMap(n=>(n.photos||[]).map(p=>p.id)))}
function validId(v){return typeof v==='string'&&/^[a-zA-Z0-9_-]{5,120}$/.test(v)}
function localStamp(){try{return localStorage.getItem(LAST_PREFIX+user.uid)||''}catch{return ''}}
function saveStamp(data){try{localStorage.setItem(LAST_PREFIX+user.uid,hash(data))}catch{}}
function setDecisionVisible(value){$('cloudDecisions').hidden=!value}
function resetSession(){
 clearTimeout(timer);if(subscribed){subscribed();subscribed=null}
 user=null;active=false;dirty=false;queued=false;uploading=false;remoteDoc=null;remoteRef=null;
 $('cloudLogout').hidden=true;
 $('cloudLogin').hidden=false;
 $('cloudAccount').textContent='';
 setDecisionVisible(false);
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
 services=sdk.initializeApp({
  apiKey:config.apiKey,
  authDomain:config.authDomain,
  projectId:config.projectId,
  appId:config.appId
 },'QuadrinheiroProjectDashboard');
 auth=sdk.getAuth(services);
 db=config.databaseId&&config.databaseId!=='(default)'?
   sdk.getFirestore(services,config.databaseId):sdk.getFirestore(services);
 $('cloudLogin').disabled=false;
 notice('Pronto para entrar com Google.');
 sdk.onAuthStateChanged(auth,async signed=>{
  if(!signed){resetSession();notice('Entre com Google para sincronizar seus registros.');return}
  user=signed;
  $('cloudLogout').hidden=false;
  $('cloudLogin').hidden=true;
  $('cloudAccount').textContent='Conta: '+(user.email||'Google')+' · seus dados ficam restritos ao seu UID.';
  notice('Conferindo registros privados…');
  try{await inspectRemote()}catch(err){notice(safeMessage(err),true)}
 });
}
async function inspectRemote(){
 if(!user)return;
 remoteRef=sdk.doc(db,'projectDashboards',user.uid);
 const snapshot=await sdk.getDoc(remoteRef);
 remoteDoc=snapshot.exists()?snapshot.data().state:null;
 const own=cloudState();
 if(remoteDoc&&hash(remoteDoc)===hash(own)){
  activate(remoteDoc,'Dados já correspondem à nuvem.');return
 }
 if(remoteDoc&&localStamp()===hash(own)){
  await hydrate(remoteDoc);activate(remoteDoc,'Dados atualizados da nuvem.');return
 }
 if(remoteDoc&&!localStorage.getItem('quadrinheiro-painel-v1')){
  await hydrate(remoteDoc);activate(remoteDoc,'Dados carregados da nuvem.');return
 }
 setDecisionVisible(true);
 if(remoteDoc)notice('Dados locais e da nuvem diferem. Escolha qual versão manter.');
 else notice('Primeiro acesso: envie os registros deste aparelho à nuvem.');
 $('cloudLoad').disabled=!remoteDoc;
 $('cloudDecisionHint').textContent=remoteDoc?
  'Escolha “Carregar” para usar a versão da nuvem; ou “Enviar” para substituí-la pelos dados deste aparelho. Faça um backup JSON antes de substituir.' :
  'Não existe um backup na nuvem. Use “Enviar dados deste aparelho” para criar um.';
}
function activate(data,description){
 active=true;dirty=false;remoteHash=hash(data);saveStamp(data);
 setDecisionVisible(false);notice(description||'Nuvem conectada e sincronizando.');
 if(subscribed)subscribed();
 subscribed=sdk.onSnapshot(remoteRef,async snapshot=>{
  if(!active||!snapshot.exists()||snapshot.metadata.hasPendingWrites||dirty||uploading)return;
  const incoming=snapshot.data().state;
  if(!incoming||hash(incoming)===remoteHash)return;
  remoteHash=hash(incoming);
  try{await hydrate(incoming);saveStamp(incoming);notice('Alterações recebidas de outro dispositivo.')}
  catch(err){notice(safeMessage(err),true)}
 },err=>notice(safeMessage(err),true));
}
async function hydrate(data){
 if(!data||!Array.isArray(data.tasks)||!Array.isArray(data.notes)||!Array.isArray(data.decisions)||!Array.isArray(data.measurements))throw Error('O backup da nuvem é inválido.');
 await downloadPhotos(data);
 APP.replaceCloudState(data);
 if(data.uiSettings)window.QuadrinheiroSettings?.importPrefs(data.uiSettings);
 if(['collapsed','expanded'].includes(data.sidebarMode))window.QuadrinheiroSidebar?.setMode(data.sidebarMode);
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
  const meta=await sdk.getDoc(photoMeta(id));
  if(!meta.exists())throw Error('Uma foto ainda não foi publicada. Tente sincronizar novamente.');
  const count=meta.data().parts;
  if(!Number.isInteger(count)||count<1||count>100)throw Error('Quantidade de partes da imagem inválida.');
  const values=[];
  for(let i=0;i<count;i++){
   const part=await sdk.getDoc(photoPart(id,i));
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
  if((await sdk.getDoc(meta)).exists())continue;
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
  remoteHash=hash(payload);saveStamp(payload);
  dirty=hash(cloudState())!==remoteHash;
  notice(dirty?'Novas alterações aguardando salvamento.':'Tudo salvo na nuvem.');
 }catch(err){
  dirty=true;notice(safeMessage(err),true);
 }finally{
  uploading=false;
  if(queued||dirty){queued=false;if(active)timer=setTimeout(push,4000)}
 }
}
function notifyLocalChange(){
 if(!active)return;
 dirty=true;
 clearTimeout(timer);
 timer=setTimeout(push,1400);
 notice('Alterações locais aguardando sincronização…');
}
async function loadCloud(){
 if(!user||!remoteDoc)return;
 try{
  notice('Carregando dados privados…');
  await hydrate(remoteDoc);
  activate(remoteDoc,'Dados da nuvem carregados e sincronização ativa.');
 }catch(err){notice(safeMessage(err),true)}
}
async function uploadLocal(){
 if(!user||!remoteRef)return;
 if(remoteDoc&&!confirm('Substituir a versão da nuvem pelos dados atuais deste aparelho? Essa ação afeta outros dispositivos. Faça antes um backup JSON.'))return;
 active=true;dirty=true;setDecisionVisible(false);
 await push();
 if(!dirty)activate(cloudState(),'Dados enviados à nuvem e sincronização ativa.');
 else {active=false;setDecisionVisible(true)}
}
$('cloudLogin').addEventListener('click',async()=>{
 try{
  const provider=new sdk.GoogleAuthProvider();
  await sdk.signInWithPopup(auth,provider);
 }catch(err){notice(safeMessage(err),true)}
});
$('cloudLogout').addEventListener('click',async()=>{
 try{await sdk.signOut(auth);notice('Conta desconectada. Os registros locais permanecem neste navegador.')}
 catch(err){notice(safeMessage(err),true)}
});
$('cloudLoad').addEventListener('click',loadCloud);
$('cloudUpload').addEventListener('click',uploadLocal);
window.QuadrinheiroCloud={notifyLocalChange,getStatus:()=>({configured:!!auth,connected:active,user:user?.email||null})};
(async()=>{
 try{
  const response=await fetch(CONFIG_URL+'?v=20261109a',{cache:'no-store'});
  if(!response.ok)throw Error('Não foi possível ler cloud-config.json');
  const conf=await response.json();
  if(!conf.enabled||!conf.apiKey||!conf.projectId||!conf.appId||!conf.authDomain){
   notice('Sincronização privada ainda não configurada. Seus dados continuam salvos neste navegador.');
   return
  }
  await openFirebase(conf);
 }catch(err){notice(safeMessage(err),true)}
})();
})();