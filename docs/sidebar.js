/* Quadrinheiro · Sidebar: preferência remota, somente em memória na sessão. */
(()=>{
'use strict';
const root=document.documentElement;
const toggle=document.getElementById('sidebarToggle');
if(!toggle)return;
let mode='expanded';
function paint(){
 root.dataset.sidebar=mode;
 const expanded=mode==='expanded';
 toggle.setAttribute('aria-expanded',String(expanded));
 toggle.setAttribute('aria-label',expanded?'Recolher barra lateral':'Expandir barra lateral');
 toggle.title=expanded?'Recolher barra lateral':'Expandir barra lateral';
 const label=toggle.querySelector('.sidebar-toggle-text');
 if(label)label.textContent=expanded?'Recolher menu':'Expandir menu';
 const icon=toggle.querySelector('svg');
 // Mesmo símbolo de painel dividido nos dois estados, conforme a referência.
 if(icon)icon.innerHTML='<rect x="3.3" y="4" width="17.4" height="16" rx="2.6"/><path d="M9.6 4v16"/>';

}
function setMode(value){
 mode=value==='collapsed'?'collapsed':'expanded';
 paint();
 window.QuadrinheiroCloud?.notifyLocalChange();
}
toggle.addEventListener('click',()=>setMode(mode==='expanded'?'collapsed':'expanded'));
function resetSession(){mode='expanded';paint()}
window.QuadrinheiroSidebar={getMode:()=>mode,setMode,resetSession};
paint();
})();