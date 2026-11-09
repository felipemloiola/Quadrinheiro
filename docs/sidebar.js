/* Quadrinheiro · Sidebar desktop expansa/recolhida; preferência local. */
(()=>{
'use strict';
const KEY='quadrinheiro-sidebar-v1';
const root=document.documentElement;
const toggle=document.getElementById('sidebarToggle');
if(!toggle)return;
function read(){try{return localStorage.getItem(KEY)==='collapsed'?'collapsed':'expanded'}catch{return 'expanded'}}
let mode=read();
function paint(){
 root.dataset.sidebar=mode;
 const expanded=mode==='expanded';
 toggle.setAttribute('aria-expanded',String(expanded));
 toggle.setAttribute('aria-label',expanded?'Recolher barra lateral':'Expandir barra lateral');
 toggle.title=expanded?'Recolher barra lateral':'Expandir barra lateral';
 const icon=toggle.querySelector('svg');
 // Mesmo símbolo de painel dividido nos dois estados, conforme a referência.
 if(icon)icon.innerHTML='<rect x="3.3" y="4" width="17.4" height="16" rx="2.6"/><path d="M9.6 4v16"/>';

}
function setMode(value){
 mode=value==='collapsed'?'collapsed':'expanded';
 try{localStorage.setItem(KEY,mode)}catch{}
 paint();
 window.QuadrinheiroCloud?.notifyLocalChange();
}
toggle.addEventListener('click',()=>setMode(mode==='expanded'?'collapsed':'expanded'));
window.QuadrinheiroSidebar={getMode:()=>mode,setMode};
paint();
})();