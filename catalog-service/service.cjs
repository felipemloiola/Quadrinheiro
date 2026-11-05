'use strict';
const cv=require('./providers.cjs');
const metron=require('./metron.cjs');
async function upstream(input,secrets,fetcher=fetch){
  const providerMetron = String(input.provider || '').toUpperCase() === 'METRO';
  const metronAllowed = ['search','catalog','issue','metron-issue','metron-character','metron-person'].includes(input.op) || ((input.op === 'covers') && (input.kind === 'metron-issue'));
  const wantsMetron = (providerMetron || String(input.provider || '').toUpperCase() === 'MULTI') && metronAllowed;
  if (wantsMetron && secrets.metron && String(secrets.metron).trim()) {
  try {
   const result=await metron.upstream(input,secrets.metron,fetcher);
   if(result && (!result.items || result.items.length))return result;
  }catch(error){if(input.op.startsWith('metron-'))throw error;}
 }
 return cv.upstream(input,secrets,fetcher);
}
module.exports={...cv,upstream};
