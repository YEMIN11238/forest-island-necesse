package necesse.entity.levelEvent.explosionEvent;

import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.gfx.GameResources;

public class CannonBallExplosionEvent extends ExplosionEvent implements Attacker {
   public CannonBallExplosionEvent() {
      this(0.0F, 0.0F, new GameDamage(100.0F), null);
   }

   public CannonBallExplosionEvent(float x, float y, GameDamage damage, Mob owner) {
      super(x, y, 80, damage, false, 0.0F, owner);
      this.sendCustomData = false;
      this.sendOwnerData = true;
   }

   @Override
   protected void playExplosionEffects() {
      SoundManager.playSound(GameResources.explosionHeavy, SoundEffect.effect(this.x, this.y).volume(2.0F).pitch(1.3F));
      this.level.getClient().startCameraShake(this.x, this.y, 400, 50, 3.0F, 3.0F, true);
   }
}
