package necesse.entity.levelEvent.explosionEvent;

import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.gfx.GameResources;

public class CaptainCannonBallExplosionEvent extends ExplosionEvent implements Attacker {
   public CaptainCannonBallExplosionEvent() {
      this(0.0F, 0.0F, new GameDamage(10.0F), null);
   }

   public CaptainCannonBallExplosionEvent(float x, float y, GameDamage damage, Mob owner) {
      super(x, y, 100, damage, false, 0.0F, owner);
      this.hitsOwner = false;
      this.sendCustomData = false;
      this.sendOwnerData = true;
   }

   @Override
   protected void playExplosionEffects() {
      SoundManager.playSound(GameResources.explosionHeavy, SoundEffect.effect(this.x, this.y).volume(1.5F).pitch(1.2F));
      this.level.getClient().startCameraShake(this.x, this.y, 400, 50, 3.0F, 3.0F, true);
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("piratecap", 4);
   }
}
