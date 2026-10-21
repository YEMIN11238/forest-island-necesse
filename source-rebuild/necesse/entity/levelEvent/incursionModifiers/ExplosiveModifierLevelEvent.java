package necesse.entity.levelEvent.incursionModifiers;

import java.util.HashSet;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.WaitForSecondsEvent;
import necesse.entity.levelEvent.explosionEvent.ExplosiveModifierChargeUpLevelEvent;
import necesse.entity.levelEvent.explosionEvent.ExplosiveModifierExplosionLevelEvent;
import necesse.entity.manager.MobDeathListenerEntityComponent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.level.maps.levelBuffManager.LevelModifiers;

public class ExplosiveModifierLevelEvent extends LevelEvent implements MobDeathListenerEntityComponent {
   public ExplosiveModifierLevelEvent() {
      super(true);
      this.shouldSave = true;
   }

   @Override
   public void onLevelMobDied(final Mob mob, Attacker attacker, HashSet<Attacker> attackers) {
      if (!mob.isPlayer && mob.isHostile && !mob.isBoss() && mob.shouldSendSpawnPacket()) {
         ExplosiveModifierChargeUpLevelEvent event = new ExplosiveModifierChargeUpLevelEvent(mob.getX(), mob.getY(), 1500.0F);
         this.getLevel().entityManager.events.add(event);
         this.level.entityManager.events.addHidden(new WaitForSecondsEvent(1.5F) {
            @Override
            public void onWaitOver() {
               GameDamage bombDamage = new GameDamage(175.0F);
               Mob owner = mob;
               if (this.getLevel().buffManager.getModifier(LevelModifiers.MODIFIERS_AFFECT_ENEMIES)) {
                  bombDamage = bombDamage.modDamage(this.level.buffManager.getModifier(LevelModifiers.ENEMY_DAMAGE));
                  owner = null;
               }

               ExplosiveModifierExplosionLevelEvent eventx = new ExplosiveModifierExplosionLevelEvent(mob.x, mob.y, 75, bombDamage, false, 0.0F, owner);
               this.getLevel().entityManager.events.add(eventx);
            }
         });
      }
   }
}
