package necesse.entity.mobs.hostile;

import necesse.entity.mobs.WormMobBody;
import necesse.level.maps.levelBuffManager.LevelModifiers;

public class HostileWormMobBody<T extends HostileWormMobHead<B, T>, B extends HostileWormMobBody<T, B>> extends WormMobBody<T, B> {
   public HostileWormMobBody(int health) {
      super(health);
      this.isHostile = true;
      this.setTeam(-2);
   }

   @Override
   public float getOutgoingDamageModifier() {
      float modifier = super.getOutgoingDamageModifier();
      if (this.getLevel() != null) {
         modifier *= this.getLevel().buffManager.getModifier(LevelModifiers.ENEMY_DAMAGE);
      }

      return modifier;
   }
}
