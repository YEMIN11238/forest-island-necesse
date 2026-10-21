package necesse.entity.levelEvent.explosionEvent;

import necesse.engine.sound.SoundSettings;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.gfx.GameResources;

public class DynamiteExplosionEvent extends BombExplosionEvent {
   public DynamiteExplosionEvent() {
      this(0.0F, 0.0F, 40, new GameDamage(80.0F), true, false, 0.0F, null);
   }

   public DynamiteExplosionEvent(float x, float y, int range, GameDamage damage, boolean destroysObjects, boolean destroyTiles, float toolTier, Mob owner) {
      super(x, y, range, damage, destroysObjects, destroyTiles, toolTier, owner);
   }

   @Override
   protected SoundSettings getExplosionSound() {
      return new SoundSettings(GameResources.explosionHeavy).volume(2.5F).fallOffDistance(1800);
   }
}
