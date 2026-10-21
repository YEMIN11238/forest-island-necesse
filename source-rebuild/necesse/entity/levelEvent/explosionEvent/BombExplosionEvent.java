package necesse.entity.levelEvent.explosionEvent;

import necesse.engine.sound.SoundSettings;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.gfx.GameResources;

public class BombExplosionEvent extends ExplosionEvent implements Attacker {
   public BombExplosionEvent() {
      this(0.0F, 0.0F, 40, new GameDamage(80.0F), true, false, 0.0F, null);
   }

   public BombExplosionEvent(float x, float y, int range, GameDamage damage, boolean destroysObjects, boolean destroysTiles, float toolTier, Mob owner) {
      super(x, y, range, damage, destroysObjects, destroysTiles, toolTier, owner);
   }

   @Override
   protected GameDamage getTotalObjectDamage(float mod) {
      float objectMod = (float)Math.pow(mod, 0.7F) * 10.0F;
      return super.getTotalObjectDamage(mod).modDamage(objectMod);
   }

   @Override
   protected void playExplosionEffects() {
      super.playExplosionEffects();
      this.level.getClient().startCameraShake(this.x, this.y, 300, 40, 3.0F, 3.0F, true);
   }

   @Override
   protected SoundSettings getExplosionSound() {
      return new SoundSettings(GameResources.explosionLight).volume(2.5F).fallOffDistance(1500);
   }
}
