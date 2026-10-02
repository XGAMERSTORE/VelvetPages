// Velvet Companion v0.4.2 camera/model sizing fix.
(function(){
  'use strict';
  let done=false;

  function fitModel(){
    if(done || typeof avatar==='undefined' || !avatar || !camera || !controls || !container) return false;
    try{
      avatar.updateMatrixWorld(true);
      const box=new THREE.Box3().setFromObject(avatar);
      const size=new THREE.Vector3(), center=new THREE.Vector3();
      box.getSize(size); box.getCenter(center);
      const major=Math.max(size.x,size.y,size.z);
      if(!isFinite(major) || major<=0.00001) return false;

      // Previous builds treated Y as model height. Michelle's source asset has another dominant axis,
      // which inflated her by several times. Normalize from the longest real dimension instead.
      const targetMajor=1.62;
      const correction=targetMajor/major;
      if(isFinite(correction) && correction>0.0001 && correction<10000){
        avatar.scale.multiplyScalar(correction);
        avatar.updateMatrixWorld(true);
      }

      // Center around origin without guessing which source axis represented height.
      const b2=new THREE.Box3().setFromObject(avatar), c2=new THREE.Vector3(), s2=new THREE.Vector3();
      b2.getCenter(c2); b2.getSize(s2);
      avatar.position.x-=c2.x;
      avatar.position.z-=c2.z;
      avatar.position.y-=b2.min.y;
      avatar.updateMatrixWorld(true);

      try{ if(typeof mixer!=='undefined' && mixer) mixer.stopAllAction(); }catch(e){}

      // Deliberately wider portrait framing. Keep the full character with large safety margins.
      const logicalH=Math.max(1.4,Math.max(s2.x,s2.y,s2.z));
      controls.target.set(0,logicalH*0.46,0);
      camera.position.set(0,logicalH*0.50,5.8);
      camera.near=.03;
      camera.far=80;
      camera.updateProjectionMatrix();
      controls.minDistance=3.8;
      controls.maxDistance=10;
      controls.update();
      done=true;
      statusEl.textContent='Michelle 3D • celá postava • online';
      return true;
    }catch(e){ console.error('v042 fit',e); return false; }
  }

  window.resetCam=function(){
    done=false;
    fitModel();
  };

  const timer=setInterval(()=>{ if(fitModel()) clearInterval(timer); },180);
  setTimeout(()=>fitModel(),2500);
})();
