package necesse.entity.mobs;

import necesse.level.maps.levelBuffManager.LevelModifiers;

public class HostileSimpleChainChildMob<T extends Mob, C extends HostileSimpleChainChildMob<T, C>> extends SimpleChainChildMob<T, C> {
   public HostileSimpleChainChildMob(int health) {
      super(health);
      this.isHostile = true;
      this.setTeam(-2);
      this.canDespawn = true;
   }

   @Override
   public float getOutgoingDamageModifier() {
      float modifier = super.getOutgoingDamageModifier();
      if (this.getLevel() != null) {
         modifier *= this.getLevel().buffManager.getModifier(LevelModifiers.ENEMY_DAMAGE);
      }

      return modifier;
   }

   @Override
   public float getSpeedModifier() {
      float modifier = super.getSpeedModifier();
      if (this.getLevel() != null) {
         modifier *= this.getLevel().buffManager.getModifier(LevelModifiers.ENEMY_SPEED);
      }

      return modifier;
   }

   @Override
   public float getMaxHealthModifier() {
      float modifier = super.getMaxHealthModifier();
      if (this.getLevel() != null) {
         modifier *= this.getLevel().buffManager.getModifier(LevelModifiers.ENEMY_MAX_HEALTH);
      }

      return modifier;
   }
}
