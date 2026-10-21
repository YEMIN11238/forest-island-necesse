package necesse.entity.levelEvent.incursionModifiers;

import necesse.engine.registries.BuffRegistry;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.manager.MobHealthChangeListenerEntityComponent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;

public class FrenzyModifierLevelEvent extends LevelEvent implements MobHealthChangeListenerEntityComponent {
   public FrenzyModifierLevelEvent() {
      super(true);
      this.shouldSave = true;
   }

   @Override
   public void onLevelMobHealthChanged(Mob mob, int beforeHealth, int health, float knockbackX, float knockbackY, Attacker attacker) {
      if (!mob.isPlayer) {
         float quarterHealthThreshold = mob.getMaxHealth() * 0.4F;
         boolean hasFrenzy = mob.buffManager.hasBuff(BuffRegistry.FRENZY);
         if (health <= quarterHealthThreshold && !hasFrenzy) {
            mob.buffManager.addBuff(new ActiveBuff(BuffRegistry.FRENZY, mob, 9999.0F, null), true);
         } else if (hasFrenzy) {
            mob.buffManager.removeBuff(BuffRegistry.FRENZY, true);
         }
      }
   }
}
