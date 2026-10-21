package necesse.entity.mobs.buffs.staticBuffs;

import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.level.maps.Level;

public class BossNearbyBuff extends VicinityBuff {
   public BossNearbyBuff() {
      this.isVisible = false;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setMaxModifier(BuffModifiers.MOB_SPAWN_RATE, 0.0F);
      buff.setMaxModifier(BuffModifiers.MOB_SPAWN_CAP, 0.0F);
   }

   public static void applyAround(Level level, float x, float y, int range) {
      level.entityManager.players.streamInRegionsShape(GameUtils.rangeBounds(x, y, range), 0).filter(p -> p.getDistance(x, y) <= range).forEach(p -> {
         ActiveBuff ab = new ActiveBuff(BuffRegistry.BOSS_NEARBY, p, 100, null);
         p.buffManager.addBuff(ab, false);
      });
   }

   public static void applyAround(Mob mob, int range) {
      applyAround(mob.getLevel(), mob.x, mob.y, range);
   }

   public static void applyAround(Mob mob) {
      applyAround(mob, 1600);
   }
}
