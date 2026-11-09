/* Quadrinheiro · Notas. Dados no navegador; fotos no IndexedDB. */
(()=>{
'use strict';
const app=window.QuadrinheiroPanel;
if(!app)return;
const dbName='quadrinheiro-notas-images-v1',storeName='images';
const validImage=data=>typeof data==='string'&&/^data:image\/(?:jpeg|png|webp|gif);base64,[A-Za-z0-9+/=]+$/.test(data);
let dbPromise=null,renderToken=0,typingTimer=null;
const kinds=['Ideia','Rascunho','Referência','Pesquisa'];
const byId=id=>document.getElementById(id);
const esc=app.escapeHtml;
function notes(){const s=app.getState();if(!Array.isArray(s.notes))s.notes=[];return s.notes}
function uid(){return typeof crypto!=='undefined'&&crypto.randomUUID?crypto.randomUUID():'note-'+Date.now().toString(36)+'-'+Math.random().toString(36).slice(2,10)}
function timeNow(){return new Date().toISOString()}
function dateLabel(iso){if(!iso)return 'Agora';const dt=new Date(iso);return Number.isNaN(+dt)?'Agora':new Intl.DateTimeFormat('pt-BR',{day:'2-digit',month:'short',year:'numeric'}).format(dt)}
function openDb(){
 if(!('indexedDB' in window))return Promise.reject(new Error('Armazenamento de imagens indisponível neste navegador.'));
 if(!dbPromise){dbPromise=new Promise((resolve,reject)=>{
  const request=indexedDB.open(dbName,1);
  request.onupgradeneeded=()=>{const db=request.result;if(!db.objectStoreNames.contains(storeName))db.createObjectStore(storeName,{keyPath:'id'})};
  request.onsuccess=()=>{const db=request.result;db.onversionchange=()=>{db.close();dbPromise=null};resolve(db)};
  request.onerror=()=>reject(request.error||new Error('Não foi possível acessar imagens.'));
  request.onblocked=()=>reject(new Error('O armazenamento de imagens está bloqueado por outra aba.'));
 }).catch(e=>{dbPromise=null;throw e})}
 return dbPromise
}
async function getImage(id){
 const db=await openDb();
 return new Promise((resolve,reject)=>{
  const req=db.transaction(storeName,'readonly').objectStore(storeName).get(id);
  req.onsuccess=()=>resolve(req.result||null);
  req.onerror=()=>reject(req.error)
 })
}
async function imageList(){
 const db=await openDb();
 return new Promise((resolve,reject)=>{
  const req=db.transaction(storeName,'readonly').objectStore(storeName).getAll();
  req.onsuccess=()=>resolve(req.result||[]);
  req.onerror=()=>reject(req.error)
 })
}
async function writeImages(action){
 const db=await openDb();
 return new Promise((resolve,reject)=>{
  const tx=db.transaction(storeName,'readwrite');
  tx.oncomplete=()=>resolve();
  tx.onerror=()=>reject(tx.error||new Error('Falha ao salvar imagens.'));
  tx.onabort=()=>reject(tx.error||new Error('Armazenamento de imagens interrompido.'));
  try{action(tx.objectStore(storeName))}catch(e){tx.abort();reject(e)}
 })
}
async function saveImage(record){await writeImages(store=>store.put(record))}
async function removeImage(id){await writeImages(store=>store.delete(id))}
async function exportImages(){
 const selected=new Set(notes().flatMap(note=>Array.isArray(note.photos)?note.photos.map(x=>x.id):[]));
 if(!selected.size)return [];
 const stored=await imageList();
 const found=stored.filter(item=>selected.has(item.id)&&validImage(item.dataUrl));
 if(found.length!==selected.size)throw new Error('Algumas fotos não estão disponíveis neste navegador. O backup não foi exportado para evitar perda silenciosa.');
 return found
}
async function importImages(records){
 if(!Array.isArray(records))throw new Error('Imagens do backup inválidas.');
 if(records.length>250)throw new Error('O backup contém imagens demais.');
 for(const item of records){
  if(!item||typeof item.id!=='string'||item.id.length>120||typeof item.noteId!=='string'||!validImage(item.dataUrl)||item.dataUrl.length>12000000)throw new Error('Uma imagem do backup é inválida.');
 }
 if(!records.length){try{await writeImages(store=>store.clear())}catch{};return}
 await writeImages(store=>{store.clear();for(const item of records)store.put({id:item.id,noteId:item.noteId,name:String(item.name||'Imagem').slice(0,160),dataUrl:item.dataUrl})})
}
function thumbFor(container,id){
 getImage(id).then(item=>{if(!item||!validImage(item.dataUrl)||!container.isConnected)return;const image=container.querySelector('img');if(image)image.src=item.dataUrl}).catch(()=>{})
}
function render(){
 const grid=byId('notesGrid'),counter=byId('notesCount'),search=byId('notesSearch'),kind=byId('notesFilter');
 if(!grid||!counter)return;
 const all=notes(),query=(search.value||'').trim().toLocaleLowerCase('pt-BR'),filter=kind.value;
 const result=all.filter(n=>(filter==='Todas'||n.kind===filter)&&(!query||[n.title,n.body,n.kind].join(' ').toLocaleLowerCase('pt-BR').includes(query))).sort((a,b)=>Number(!!b.pinned)-Number(!!a.pinned)||String(b.updatedAt||'').localeCompare(String(a.updatedAt||'')));
 counter.textContent=result.length+' de '+all.length+' notas · salvas neste navegador';
 if(!result.length){
  grid.innerHTML='<div class="notes-empty panel"><span class="notes-empty-glyph" aria-hidden="true">'+APP.icon("notes")+'</span><h2>'+(all.length?'Nenhuma nota encontrada':'Um lugar para suas ideias.')+'</h2><p>'+(all.length?'Experimente outra busca ou categoria.':'Guarde inspirações, capturas de tela, referências e rascunhos sobre o Quadrinheiro.')+'</p><button type="button" class="btn primary" data-new-note>Escrever uma nota</button></div>';
  return
 }
 grid.innerHTML=result.map(note=>{
  const photos=Array.isArray(note.photos)?note.photos:[];
  const preview=String(note.body||'').replace(/\s+/g,' ').trim();
  return '<article class="panel notes-card" data-note-open="'+esc(note.id)+'" tabindex="0" role="button" aria-label="Abrir nota '+esc(note.title||'Sem título')+'">'+
    '<div class="notes-card-top"><span class="notes-type">'+esc(kinds.includes(note.kind)?note.kind:'Ideia')+'</span><button type="button" class="notes-pin'+(note.pinned?' is-pinned':'')+'" data-note-pin="'+esc(note.id)+'" aria-label="'+(note.pinned?'Desafixar':'Fixar')+' nota" title="'+(note.pinned?'Desafixar':'Fixar')+'">⌑</button></div>'+
    '<h3>'+esc(note.title||'Sem título')+'</h3>'+
    (preview?'<p class="notes-snippet">'+esc(preview.slice(0,240))+(preview.length>240?'…':'')+'</p>':'<p class="notes-snippet notes-placeholder">Toque para escrever seu rascunho.</p>')+
    (photos.length?'<div class="notes-cover" data-note-cover="'+esc(photos[0].id)+'"><img alt="Imagem anexada à nota" loading="lazy"><span>'+photos.length+' foto'+(photos.length>1?'s':'')+'</span></div>':'')+
    '<div class="notes-meta"><time>'+esc(dateLabel(note.updatedAt||note.createdAt))+'</time>'+(note.pinned?'<span>Fixada</span>':'<span>Editar nota ↗</span>')+'</div></article>'
 }).join('');
 const token=++renderToken;
 grid.querySelectorAll('[data-note-cover]').forEach(box=>{if(token===renderToken)thumbFor(box,box.dataset.noteCover)})
}
function createNote(){
 const stamp=timeNow();
 const note={id:uid(),title:'',body:'',kind:'Ideia',pinned:false,createdAt:stamp,updatedAt:stamp,photos:[]};
 notes().unshift(note);app.save();render();openNote(note.id)
}
function openNote(id){
 const note=notes().find(x=>x.id===id);
 if(!note)return;
 if(!Array.isArray(note.photos))note.photos=[];
 const choose=kinds.map(k=>'<option value="'+esc(k)+'"'+(note.kind===k?' selected':'')+'>'+esc(k)+'</option>').join('');
 const body='<div class="notes-editor">'+
  '<div class="notes-editor-row"><label class="notes-field-label" for="noteKind">Categoria</label><select id="noteKind" class="field" data-note-field="kind">'+choose+'</select></div>'+
  '<label class="notes-field-label" for="noteTitle">Título</label><input id="noteTitle" data-note-field="title" class="field notes-title-input" maxlength="160" placeholder="Dê um título à sua ideia" value="'+esc(note.title||'')+'">'+
  '<label class="notes-field-label" for="noteBody">Conteúdo</label><textarea id="noteBody" data-note-field="body" class="field notes-body-input" maxlength="30000" placeholder="Ideias, mudanças, referências, rascunhos…">'+esc(note.body||'')+'</textarea>'+
  '<div class="notes-attachments-heading"><strong>Fotos e referências visuais</strong><span id="notePhotoCount" class="small muted"></span></div>'+
  '<div class="notes-photo-grid" id="notePhotoList"></div>'+
  '<div class="notes-drop-zone" id="notesDropZone" tabindex="0" role="button" aria-label="Adicionar fotos à nota"><span aria-hidden="true">＋</span><strong>Adicionar fotos</strong><small>Selecione, arraste ou cole imagens aqui · até 10 fotos por nota</small></div>'+
  '<input id="noteImageInput" type="file" accept="image/*" multiple hidden>'+
  '<p class="notes-storage-message" id="notesUploadStatus">Fotos salvas somente neste navegador. O backup JSON inclui os anexos.</p>'+
  '</div>';
 const foot='<span class="notes-save-status" id="notesSaveStatus">Salvo automaticamente</span><div class="notes-editor-actions"><button type="button" class="btn small ghost danger" id="deleteNote">Excluir</button><button type="button" class="btn small" id="pinNote">'+(note.pinned?'Desafixar':'Fixar')+'</button><button type="button" class="btn primary" id="doneNote">Concluir</button></div>';
 app.modal(note.title||'Nova nota',body,foot);
 byId('modalBg').classList.add('notes-editor-open');
 const modalBody=byId('modalBody');
 const sync=()=>{
  note.updatedAt=timeNow();
  app.save();
  const saved=byId('notesSaveStatus');if(saved)saved.textContent='Salvo neste navegador';
  clearTimeout(typingTimer);typingTimer=setTimeout(render,220)
 };
 modalBody.querySelectorAll('[data-note-field]').forEach(field=>field.addEventListener('input',()=>{
  const prop=field.dataset.noteField;note[prop]=field.value;sync()
 }));
 byId('pinNote').onclick=()=>{note.pinned=!note.pinned;byId('pinNote').textContent=note.pinned?'Desafixar':'Fixar';sync()};
 byId('doneNote').onclick=()=>{app.closeModal();render()};
 byId('deleteNote').onclick=()=>removeNote(note.id);
 const input=byId('noteImageInput'),drop=byId('notesDropZone');
 drop.onclick=()=>input.click();
 drop.onkeydown=e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();input.click()}};
 input.onchange=async()=>{await attachPhotos(note,Array.from(input.files||[]));input.value=''};
 drop.addEventListener('dragover',event=>{event.preventDefault();drop.classList.add('dragging')});
 drop.addEventListener('dragleave',()=>drop.classList.remove('dragging'));
 drop.addEventListener('drop',async event=>{event.preventDefault();drop.classList.remove('dragging');await attachPhotos(note,Array.from(event.dataTransfer?.files||[]))});
 modalBody.addEventListener('paste',async event=>{
  const images=Array.from(event.clipboardData?.items||[]).filter(item=>item.kind==='file'&&item.type.startsWith('image/')).map(item=>item.getAsFile()).filter(Boolean);
  if(images.length){event.preventDefault();await attachPhotos(note,images)}
 });
 modalBody.addEventListener('click',async event=>{
  const b=event.target.closest('[data-remove-note-photo]');if(!b)return;
  const photoId=b.dataset.removeNotePhoto;if(!confirm('Remover esta foto da nota?'))return;
  try{await removeImage(photoId);note.photos=note.photos.filter(x=>x.id!==photoId);sync();paintPhotos(note)}
  catch(e){app.notify(e.message||'Falha ao remover a foto.',true)}
 });
 paintPhotos(note)
}
async function paintPhotos(note){
 const list=byId('notePhotoList'),count=byId('notePhotoCount');
 if(!list||!count)return;
 count.textContent=note.photos.length+'/10';
 list.innerHTML=note.photos.map(photo=>'<figure class="notes-photo" data-note-image="'+esc(photo.id)+'"><img alt="'+esc(photo.name||'Imagem da nota')+'"><button type="button" data-remove-note-photo="'+esc(photo.id)+'" aria-label="Remover '+esc(photo.name||'imagem')+'">×</button><figcaption>'+esc(photo.name||'Foto')+'</figcaption></figure>').join('');
 const expected=note.id;
 for(const photo of note.photos){
  const target=Array.from(list.querySelectorAll('[data-note-image]')).find(box=>box.dataset.noteImage===photo.id);
  if(!target)continue;
  const img=target.querySelector('img');
  try{const item=await getImage(photo.id);if(item&&validImage(item.dataUrl)&&img.isConnected&&notes().some(n=>n.id===expected))img.src=item.dataUrl}catch{}
 }
}
function readFileAsDataURL(file){
 return new Promise((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(reader.result);reader.onerror=()=>reject(new Error('Falha ao ler a imagem.'));reader.readAsDataURL(file)})
}
function loadBitmap(file){
 return new Promise((resolve,reject)=>{
  const url=URL.createObjectURL(file),image=new Image();
  image.onload=()=>{URL.revokeObjectURL(url);resolve(image)};
  image.onerror=()=>{URL.revokeObjectURL(url);reject(new Error('Formato de foto não suportado por este navegador.'))};
  image.src=url
 })
}
async function optimizePhoto(file){
 if(!file||!file.type.startsWith('image/'))throw new Error('Selecione somente arquivos de imagem.');
 if(file.size>15*1024*1024)throw new Error('A foto excede 15 MB.');
 if(file.type==='image/svg+xml')throw new Error('Arquivos SVG não são aceitos por segurança.');
 if(['image/png','image/jpeg','image/webp','image/gif'].includes(file.type)&&file.size<=900000){
  const original=await readFileAsDataURL(file);
  if(validImage(original))return original
 }
 const image=await loadBitmap(file),maxDim=1920,ratio=Math.min(1,maxDim/Math.max(image.naturalWidth,image.naturalHeight));
 const canvas=document.createElement('canvas');canvas.width=Math.max(1,Math.round(image.naturalWidth*ratio));canvas.height=Math.max(1,Math.round(image.naturalHeight*ratio));
 const ctx=canvas.getContext('2d');if(!ctx)throw new Error('O navegador não conseguiu preparar a imagem.');
 ctx.fillStyle='#ffffff';ctx.fillRect(0,0,canvas.width,canvas.height);ctx.drawImage(image,0,0,canvas.width,canvas.height);
 const data=canvas.toDataURL('image/jpeg',0.86);
 if(!validImage(data))throw new Error('Não foi possível converter a foto.');
 return data
}
async function attachPhotos(note,files){
 if(!files.length)return;
 const status=byId('notesUploadStatus');
 if(!status)return;
 const remaining=10-note.photos.length;
 if(remaining<=0){app.notify('Cada nota pode ter até 10 fotos.',true);return}
 if(files.length>remaining)app.notify('O limite é de 10 fotos por nota; algumas foram ignoradas.',true);
 let successes=0;
 for(const file of files.slice(0,remaining)){
  try{
   status.textContent='Preparando '+file.name+'…';
   const dataUrl=await optimizePhoto(file),id=uid();
   await saveImage({id,noteId:note.id,name:String(file.name||'Foto').slice(0,160),dataUrl});
   note.photos.push({id,name:String(file.name||'Foto').slice(0,160)});
   note.updatedAt=timeNow();app.save();successes++;
  }catch(error){app.notify(error.message||'Não foi possível adicionar a imagem.',true)}
 }
 if(successes){status.textContent=successes+' foto(s) adicionada(s) e salvas no navegador.';paintPhotos(note);render()}
 else status.textContent='Nenhuma imagem foi adicionada. Verifique formato, tamanho e permissões do navegador.'
}
async function removeNote(id){
 const note=notes().find(n=>n.id===id);if(!note||!confirm('Excluir esta nota e todas as fotos anexadas?'))return;
 notes().splice(notes().indexOf(note),1);app.save();app.closeModal();render();
 for(const photo of note.photos||[]){try{await removeImage(photo.id)}catch{}}
 app.notify('Nota excluída.')
}
document.addEventListener('click',event=>{
 if(event.target.closest('[data-new-note]')){createNote();return}
 const pin=event.target.closest('[data-note-pin]');if(pin){event.stopPropagation();const note=notes().find(n=>n.id===pin.dataset.notePin);if(note){note.pinned=!note.pinned;note.updatedAt=timeNow();app.save();render()}return}
 const card=event.target.closest('[data-note-open]');if(card)openNote(card.dataset.noteOpen)
});
document.addEventListener('keydown',event=>{
 if((event.key==='Enter'||event.key===' ')&&event.target.matches('[data-note-open]')){event.preventDefault();openNote(event.target.dataset.noteOpen)}
});
byId('notesSearch')?.addEventListener('input',render);
byId('notesFilter')?.addEventListener('change',render);
byId('addNote')?.addEventListener('click',createNote);
byId('notesImportHint')?.addEventListener('click',()=>byId('importBtn')?.click());
window.QuadrinheiroNotes={render,exportImages,importImages};
render();
})();
