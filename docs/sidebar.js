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
 if(icon){icon.innerHTML=expanded
 ?'<rect x="3" y="4" width="18" height="16" rx="2"/><path d="M9 4v16"/><path d="m16 9 3 3-3 3"/>'
 :'<rect x="3" y="4" width="18" height="16" rx="2"/><path d="M9 4v16"/><path d="m17 9-3 3 3 3"/>'}
}
function setMode(value){
 mode=value==='collapsed'?'collapsed':'expanded';
 try{localStorage.setItem(KEY,mode)}catch{}
 paint();
}
toggle.addEventListener('click',()=>setMode(mode==='expanded'?'collapsed':'expanded'));
window.QuadrinheiroSidebar={getMode:()=>mode,setMode};
paint();
})();