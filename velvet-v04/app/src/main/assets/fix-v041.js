// Velvet Companion v0.4.1 runtime fixes: robust avatar normalization, camera fit, Puter auth.
(function(){
  'use strict';
  let fitted = false;

  function addSystem(text){
    try { bubble(text,'s'); } catch(e) { console.log(text); }
  }

  function normalizeMichelleAndFit(){
    try {
      if(!avatar || !camera || !controls || !container) return false;
      avatar.updateMatrixWorld(true);
      let box = new THREE.Box3().setFromObject(avatar);
      let size = new THREE.Vector3();
      box.getSize(size);
      if(!isFinite(size.y) || size.y <= 0.0001) return false;

      // Michelle should be human-sized in our scene. Correct any bogus skinned-mesh bbox scaling.
      const targetHeight = 1.76;
      const correction = targetHeight / size.y;
      if(isFinite(correction) && correction > 0.001 && correction < 1000){
        avatar.scale.multiplyScalar(correction);
        avatar.updateMatrixWorld(true);
      }

      // Re-ground and center after normalization.
      box = new THREE.Box3().setFromObject(avatar);
      const center = new THREE.Vector3();
      const finalSize = new THREE.Vector3();
      box.getCenter(center); box.getSize(finalSize);
      avatar.position.x -= center.x;
      avatar.position.z -= center.z;
      avatar.position.y -= box.min.y;
      avatar.updateMatrixWorld(true);

      // Disable the bundled Samba clip at boot; our companion gestures own the rig.
      if(typeof mixer !== 'undefined' && mixer){ try { mixer.stopAllAction(); } catch(e){} }

      fitMichelleCamera();
      fitted = true;
      statusEl.textContent = 'Michelle 3D • celá postava • online';
      return true;
    } catch(e){ console.error('Michelle normalize/fit', e); return false; }
  }

  window.fitMichelleCamera = function(){
    try{
      if(!avatar || !camera || !controls) return;
      avatar.updateMatrixWorld(true);
      const box = new THREE.Box3().setFromObject(avatar);
      const size = new THREE.Vector3(), center = new THREE.Vector3();
      box.getSize(size); box.getCenter(center);
      const h = Math.max(.5, size.y), w = Math.max(.3, size.x);
      const vFov = THREE.MathUtils.degToRad(camera.fov);
      const distH = (h * .58) / Math.tan(vFov/2);
      const hFov = 2*Math.atan(Math.tan(vFov/2)*camera.aspect);
      const distW = (w * .65) / Math.tan(hFov/2);
      const dist = Math.max(distH, distW, 2.6) * 1.12;
      const target = new THREE.Vector3(0, Math.max(.75, h*.52), 0);
      controls.target.copy(target);
      camera.position.set(0, target.y + h*.04, dist);
      camera.near = Math.max(.02, dist/150);
      camera.far = Math.max(50, dist*15);
      camera.updateProjectionMatrix();
      controls.minDistance = Math.max(1.4, dist*.45);
      controls.maxDistance = Math.max(7, dist*2.5);
      controls.update();
    }catch(e){ console.error('fit camera',e); }
  };

  // Replace the old fixed camera reset.
  window.resetCam = function(){ fitMichelleCamera(); };

  function aiPanel(){ return document.getElementById('ai'); }
  function aiState(){ return document.getElementById('aiState'); }

  async function refreshPuterState(){
    const s = aiState();
    try{
      if(!window.puter || !puter.auth){ if(s) s.textContent='Puter se nenačetl • zkontroluj internet'; return false; }
      const signed = !!puter.auth.isSignedIn();
      if(!signed){ if(s) s.textContent='FREE AI: nepřipojena'; return false; }
      let name='';
      try{ const u=await puter.auth.getUser(); name=(u&&(u.username||u.email||u.name))||''; }catch(e){}
      if(s) s.textContent='FREE AI: připojena ✓'+(name?' • '+name:'');
      const b=document.getElementById('puterConnect'); if(b) b.textContent='AI PŘIPOJENA ✓';
      return true;
    }catch(e){ if(s) s.textContent='FREE AI: stav nelze ověřit'; return false; }
  }

  window.connectPuterAI = async function(){
    const s=aiState(), b=document.getElementById('puterConnect');
    if(b) b.disabled=true;
    if(s) s.textContent='Otevírám Puter přihlášení…';
    try{
      if(!window.puter || !puter.auth) throw new Error('Puter.js se nenačetl');
      if(!puter.auth.isSignedIn()){
        await puter.auth.signIn({attempt_temp_user_creation:true});
      }
      const ok=await refreshPuterState();
      if(ok) addSystem('FREE AI je připojena ✓');
    }catch(e){
      console.error('Puter sign in',e);
      if(s) s.textContent='Přihlášení se nepovedlo • klepni znovu';
      addSystem('Puter přihlášení se nepovedlo. Zkus tlačítko PŘIPOJIT FREE AI znovu.');
    }finally{ if(b)b.disabled=false; }
  };

  function installConnectButton(){
    const p=aiPanel(); if(!p || document.getElementById('puterConnect')) return;
    const g=document.createElement('div'); g.className='group';
    g.innerHTML='<button id="puterConnect" class="btn p" style="width:100%;font-size:13px;padding:13px">PŘIPOJIT FREE AI</button><div class="tiny" style="margin-top:7px">Otevře bezpečné Puter přihlášení. Žádný OpenAI API klíč nepotřebuješ.</div>';
    p.insertBefore(g,p.children[2]||null);
    document.getElementById('puterConnect').onclick=connectPuterAI;
    refreshPuterState();
  }

  // Replace message sending: explicitly authenticate before invoking AI.
  window.sendMessage = async function(force){
    const msg=(force||input.value).trim(); if(!msg)return;
    input.value=''; bubble(msg,'u'); remember('user',msg); $('send').disabled=true; statusEl.textContent='FREE AI • přemýšlím…';
    try{
      if(!window.puter || !puter.ai || !puter.auth) throw new Error('Puter není načtený');
      if(!puter.auth.isSignedIn()){
        statusEl.textContent='FREE AI • potřebuje přihlášení';
        openPanel('ai');
        addSystem('Nejdřív klepni na PŘIPOJIT FREE AI.');
        return;
      }
      const r=await puter.ai.chat(promptFor(msg),{model:'gpt-5-nano'});
      let text=typeof r==='string'?r:(r&&r.message&&r.message.content)||r?.content||String(r);
      text=text.replace(/^```json\s*/,'').replace(/```$/,'').trim();
      const s=text.indexOf('{'), e=text.lastIndexOf('}'); if(s>=0&&e>s)text=text.slice(s,e+1);
      handle(JSON.parse(text));
    }catch(e){
      console.error(e); const a=infer(msg); handle({reply:'Teď se AI nepodařilo spojit. Pohyb ale provedu lokálně ♡',action:a,emotion:'neutral'});
      addSystem('FREE AI není připojená. Otevři AI → PŘIPOJIT FREE AI.');
    }finally{ $('send').disabled=false; }
  };

  // Existing handlers captured the old sendMessage binding by name, which resolves dynamically.
  installConnectButton();
  const waiter=setInterval(function(){
    installConnectButton();
    if(!fitted && typeof avatar!=='undefined' && avatar){ if(normalizeMichelleAndFit()) clearInterval(waiter); }
  },180);
  setTimeout(function(){ if(!fitted) normalizeMichelleAndFit(); },2500);
})();
