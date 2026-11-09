/* Quadrinheiro · Ajustes. Paleta extraída de app/src/main/java/com/example/ui/theme/Theme.kt. */
(()=>{
'use strict';
const APP=window.QuadrinheiroPanel;
const root=document.documentElement;
const key='quadrinheiro-ui-settings-v1';
const oldThemeKey='quadrinheiro-theme-v2';
const options=[
 {id:'BLUE',name:'Azul',light:'#2E7CF6',dark:'#4C94FF'},
 {id:'WHITE',name:'Branco',light:'#52525B',dark:'#FFFFFF',lightSecondary:'#8E8E93',darkSecondary:'#D4D4D8'},
 {id:'BLACK',name:'Preto',light:'#000000',dark:'#D4D4D8',lightSecondary:'#3F3F46',darkSecondary:'#8E8E93'},
 {id:'RED',name:'Vermelho',light:'#D9383A',dark:'#FF453A'},
 {id:'GREEN',name:'Verde',light:'#28A745',dark:'#32D74B'},
 {id:'PURPLE',name:'Roxo',light:'#8B5CF6',dark:'#BF5AF2'},
 {id:'ORANGE',name:'Laranja',light:'#E67E22',dark:'#FF9F0A'}
];
const defaults={themeMode:'system',accent:'BLUE',density:'comfortable',reduceMotion:false,reduceTransparency:false,autoSync:true};
function oneOf(value,values,fallback){return values.includes(value)?value:fallback}
function validate(input){const s=input&&typeof input==='object'&&!Array.isArray(input)?input:{};return {
 themeMode:oneOf(s.themeMode,['system','light','dark'],defaults.themeMode),
 accent:oneOf(s.accent,options.map(o=>o.id),defaults.accent),
 density:oneOf(s.density,['comfortable','compact'],defaults.density),
 reduceMotion:s.reduceMotion===true,
 reduceTransparency:s.reduceTransparency===true,
 autoSync:s.autoSync!==false
}}
function load(){
 try{
  const stored=localStorage.getItem(key);
  if(stored)return validate(JSON.parse(stored));
  const legacy=localStorage.getItem(oldThemeKey);
  return validate({...defaults,themeMode:oneOf(legacy,['dark','light'],defaults.themeMode)})
 }catch{return {...defaults}}
}
let preferences=load();
function persist(){try{localStorage.setItem(key,JSON.stringify(preferences))}catch{APP?.notify('O navegador não permitiu salvar os ajustes.',true)}}
function rgb(hex){const s=hex.replace('#','');return [0,2,4].map(n=>parseInt(s.slice(n,n+2),16))}
function hex(rgbValue){return '#'+rgbValue.map(x=>Math.round(x).toString(16).padStart(2,'0')).join('').toUpperCase()}
function mixed(a,b,amount){const x=rgb(a),y=rgb(b);return hex(x.map((n,i)=>n+(y[i]-n)*amount))}
function luminance(color){return rgb(color).map(c=>c/255).map(c=>c<=.04045?c/12.92:((c+.055)/1.055)**2.4).reduce((n,c,i)=>n+c*[.2126,.7152,.0722][i],0)}
function colorFor(accent,mode){const target=options.find(o=>o.id===accent)||options[0];const active=target[mode];const secondary=target[mode+'Secondary']||mixed(active,mode==='dark'?'#FFFFFF':'#000000',mode==='dark'?.28:.15);return {active,secondary,onAccent:luminance(active)>.23?'#141418':'#FFFFFF'}}
const systemQuery=window.matchMedia?.('(prefers-color-scheme: dark)');
function currentTheme(){return preferences.themeMode==='system'?(systemQuery?.matches?'dark':'light'):preferences.themeMode}
function updateBrandFavicon(theme,accent){
 // Os três contornos são os mesmos da marca Android, pintados com theme.accent.
 const bg=theme==='dark'?'#101010':'#F3F4F7';
 const icon='<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108">'+
   '<rect width="108" height="108" rx="25" fill="'+bg+'"/>'+
   '<g fill="'+accent+'">'+
   '<path d="M44.8 26.3 28.9 32.9 25 54l6.6 21.1 7.9-5.3 3.3-9.2-2.7-7.9 3.3-13.2z"/>'+
   '<path d="M49.4 26.3h17.8l13.2 9.2v15.2H63.9l-2-5.9-8.6-6-5.2.7z"/>'+
   '<path d="M63.9 55.3h15.2l-2.7 8.6 9.3 13.9-2 5.9-14.5-5.3-13.9 2-15.2-3.3 6-11.2 7.9-2.7z"/>'+
   '</g></svg>';
 const url='data:image/svg+xml,'+encodeURIComponent(icon);
 for(const link of document.querySelectorAll('link[rel="icon"],link[rel="shortcut icon"]'))link.href=url;
}
function apply(){
 const theme=currentTheme(),accent=colorFor(preferences.accent,theme);
 root.dataset.theme=theme;
 root.dataset.themeMode=preferences.themeMode;
 root.dataset.accent=preferences.accent;
 root.dataset.density=preferences.density;
 root.dataset.motion=preferences.reduceMotion?'reduced':'system';
 root.dataset.transparency=preferences.reduceTransparency?'reduced':'system';
 root.style.setProperty('--accent',accent.active);
 root.style.setProperty('--accent-2',accent.secondary);
 root.style.setProperty('--accent-contrast',accent.onAccent);
 updateBrandFavicon(theme,accent.active);
 const themeColor=document.querySelector('meta[name="theme-color"]');if(themeColor)themeColor.content=theme==='dark'?'#000000':'#F2F2F7'
}
function update(changes,{announce=true}={}){
 preferences=validate({...preferences,...changes});
 persist();apply();render();
 if(announce)APP?.notify('Ajustes salvos.');
}
function escape(str){return APP?.escapeHtml?APP.escapeHtml(str):String(str)}
function render(){
 const page=document.getElementById('settings');
 if(!page)return;
 const modes=[['system','Sistema','Seguir aparelho'],['light','Claro','Fundo claro'],['dark','Escuro','Fundo escuro']];
 const wrap=document.getElementById('settingsThemeOptions');
 if(wrap)wrap.innerHTML=modes.map(([mode,label,hint])=>'<button class="settings-appearance-option" type="button" data-mode="'+mode+'" aria-pressed="'+(preferences.themeMode===mode)+'"><span class="settings-theme-preview '+mode+'" aria-hidden="true"><i></i><span><i></i><i></i></span></span><strong>'+label+'</strong><span class="small muted">'+hint+'</span></button>').join('');
 const isDark=currentTheme()==='dark';
 const colors=document.getElementById('settingsColorOptions');
 if(colors)colors.innerHTML=options.map(o=>{
  const c=colorFor(o.id,isDark?'dark':'light');
  return '<button class="settings-color" data-accent-choice="'+o.id+'" type="button" aria-label="Selecionar '+o.name+'" aria-pressed="'+(preferences.accent===o.id)+'" title="'+o.name+' · '+c.active+'"><span class="settings-color-swatch" style="--swatch:'+c.active+';--check-color:'+c.onAccent+'"><i aria-hidden="true"></i></span><span class="settings-color-name">'+o.name+'</span></button>';
 }).join('');
 const current=options.find(x=>x.id===preferences.accent)||options[0];
 const colorInfo=document.getElementById('settingsAccentInfo');
 if(colorInfo)colorInfo.textContent=current.name+' · '+colorFor(current.id,isDark?'dark':'light').active+' · cores definidas no aplicativo Android';
 const appearanceInfo=document.getElementById('settingsThemeInfo');
 if(appearanceInfo)appearanceInfo.textContent='Aparência atual: '+(isDark?'Escura':'Clara')+(preferences.themeMode==='system'?' (seguindo o sistema)':'');
 const density=document.getElementById('settingsDensity');
 if(density)density.checked=preferences.density==='compact';
 const motion=document.getElementById('settingsMotion');
 if(motion)motion.checked=preferences.reduceMotion;
 const transparency=document.getElementById('settingsTransparency');
 if(transparency)transparency.checked=preferences.reduceTransparency;
 const sync=document.getElementById('settingsAutoSync');
 if(sync)sync.checked=preferences.autoSync;
 const summary=document.getElementById('settingsSyncSummary');
 if(summary)summary.textContent=preferences.autoSync?'A consulta ao GitHub é feita ao abrir o painel e periodicamente enquanto estiver ativo.':'Sincronização automática desligada. O botão no cabeçalho continua funcionando.';
 const state=APP?.getState?.();
 const storage=document.getElementById('settingsStorageStats');
 if(storage&&state){
  const rows=[['Tarefas',state.tasks?.length||0],['Notas',state.notes?.length||0],['Fotos anexadas',(state.notes||[]).reduce((sum,n)=>sum+(n.photos||[]).length,0)],['Decisões',state.decisions?.length||0],['Medições',state.measurements?.length||0]];
  storage.innerHTML=rows.map(([label,value])=>'<div class="settings-storage-row"><span>'+escape(label)+'</span><strong>'+value+'</strong></div>').join('')
 }
}
function importPrefs(data){preferences=validate(data);persist();apply();render()}
function reset(){
 if(!confirm('Restaurar apenas as preferências visuais e de sincronização? Tarefas, notas, fotos e decisões serão preservadas.'))return;
 preferences={...defaults};persist();apply();render();APP?.notify('Ajustes restaurados, sem alterar seus registros.')
}
const page=document.getElementById('settings');
if(page)page.innerHTML=`
<div class="head-row"><div><div class="eyebrow">Personalização</div><h1 class="page-title">Ajustes</h1><p class="page-desc">Personalize o painel do Quadrinheiro e controle como seus dados são apresentados.</p></div></div>
<div class="settings-layout">
  <div class="settings-column">
    <section class="panel settings-card" aria-labelledby="settingsAppearanceTitle">
      <div class="settings-card-head"><span class="settings-card-icon" aria-hidden="true">${APP.icon("spark")}</span><div><h2 id="settingsAppearanceTitle">Aparência</h2><p>Temas e cores iguais aos do aplicativo.</p></div></div>
      <h3 class="settings-field-title">Modo de aparência</h3><div class="settings-appearance-options" id="settingsThemeOptions" role="group" aria-label="Modo de aparência"></div>
      <p class="settings-appearance-meta" id="settingsThemeInfo"></p><hr class="settings-option-divider">
      <h3 class="settings-field-title">Cor de destaque</h3><div class="settings-colors" id="settingsColorOptions" role="group" aria-label="Cor de destaque"></div>
      <p class="settings-appearance-meta" id="settingsAccentInfo"></p>
    </section>
    <section class="panel settings-card" aria-labelledby="settingsComfortTitle">
      <div class="settings-card-head"><span class="settings-card-icon" aria-hidden="true">${APP.icon("shield")}</span><div><h2 id="settingsComfortTitle">Visualização e acessibilidade</h2><p>Adapte a interface à sua preferência.</p></div></div>
      <label class="settings-row" for="settingsDensity"><div><strong>Layout compacto</strong><p>Mostra mais informações por tela.</p></div><input class="settings-switch" type="checkbox" id="settingsDensity" role="switch"></label>
      <label class="settings-row" for="settingsMotion"><div><strong>Reduzir animações</strong><p>Desativa transições visuais sem alterar funcionalidades.</p></div><input class="settings-switch" type="checkbox" id="settingsMotion" role="switch"></label>
      <label class="settings-row" for="settingsTransparency"><div><strong>Reduzir transparência</strong><p>Utiliza superfícies sólidas.</p></div><input class="settings-switch" type="checkbox" id="settingsTransparency" role="switch"></label>
    </section>
  </div>
  <div class="settings-column">
    <section class="panel settings-card" aria-labelledby="settingsGithubTitle">
      <div class="settings-card-head"><span class="settings-card-icon" aria-hidden="true">${APP.icon("branch")}</span><div><h2 id="settingsGithubTitle">GitHub</h2><p>Atualização das informações públicas.</p></div></div>
      <label class="settings-row" for="settingsAutoSync"><div><strong>Sincronização automática</strong><p>Consulta commits, releases e estrutura ao abrir o painel e periodicamente.</p></div><input class="settings-switch" type="checkbox" id="settingsAutoSync" role="switch"></label>
      <p class="settings-note" id="settingsSyncSummary"></p>
      <div class="settings-inline-actions"><button type="button" class="btn small" data-settings-action="sync">Sincronizar agora ↻</button></div>
    </section>
    <section class="panel settings-card" aria-labelledby="settingsDataTitle">
      <div class="settings-card-head"><span class="settings-card-icon" aria-hidden="true">${APP.icon("folder")}</span><div><h2 id="settingsDataTitle">Dados e backups</h2><p>Registros armazenados no navegador.</p></div></div>
      <div class="settings-storage-table" id="settingsStorageStats"></div>
      <div class="settings-inline-actions"><button type="button" class="btn small primary" data-settings-action="export">Exportar backup ↗</button><button type="button" class="btn small ghost" data-settings-action="import">Importar backup ↗</button></div>
      <p class="settings-note">O backup inclui tarefas, notas, fotos, decisões, medições e preferências. Para transferir a outro aparelho, é necessário importar o arquivo.</p>
    </section>
    <section class="panel settings-card" aria-labelledby="settingsResetTitle">
      <div class="settings-card-head"><span class="settings-card-icon" aria-hidden="true">${APP.icon("refresh")}</span><div><h2 id="settingsResetTitle">Restaurar preferências</h2><p>Volta à aparência padrão sem apagar dados.</p></div></div>
      <button type="button" class="btn small ghost" data-settings-action="reset">Restaurar ajustes padrão</button>
      <p class="settings-note">Não remove tarefas, notas, fotos, decisões nem medições.</p>
    </section>
  </div>
</div>`;
page?.addEventListener('click',event=>{
 const mode=event.target.closest('[data-mode]');
 if(mode){update({themeMode:mode.dataset.mode});return}
 const accent=event.target.closest('[data-accent-choice]');
 if(accent){update({accent:accent.dataset.accentChoice});return}
 const action=event.target.closest('[data-settings-action]')?.dataset.settingsAction;
 if(action==='export')document.getElementById('exportBtn')?.click();
 if(action==='import')document.getElementById('importBtn')?.click();
 if(action==='reset')reset();
 if(action==='sync')document.getElementById('syncBtn')?.click();
});
page?.addEventListener('change',event=>{
 const controls={settingsDensity:()=>update({density:event.target.checked?'compact':'comfortable'}),settingsMotion:()=>update({reduceMotion:event.target.checked}),settingsTransparency:()=>update({reduceTransparency:event.target.checked}),settingsAutoSync:()=>{const was=preferences.autoSync;update({autoSync:event.target.checked});if(!was&&preferences.autoSync)document.getElementById('syncBtn')?.click()}};
 controls[event.target.id]?.()
});
systemQuery?.addEventListener?.('change',()=>{if(preferences.themeMode==='system'){apply();render()}});
window.QuadrinheiroSettings={getPrefs:()=>({...preferences}),importPrefs,render,apply,colors:options.map(o=>({...o}))};
apply();render();
})();
