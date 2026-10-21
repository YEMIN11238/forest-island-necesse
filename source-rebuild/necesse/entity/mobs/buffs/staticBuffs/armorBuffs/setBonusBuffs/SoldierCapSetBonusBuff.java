package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs;

import java.util.Comparator;
import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobWasKilledEvent;
import necesse.entity.mobs.TrainingDummyMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.RicochetableProjectile;
import necesse.gfx.gameTooltips.ListGameTooltips;

public class SoldierCapSetBonusBuff extends SetBonusBuff {
   private final int ricochetRange = 160;

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }

   @Override
   public void onHasKilledTarget(ActiveBuff buff, MobWasKilledEvent event) {
      if (event.didKillingBlow(buff.owner)) {
         if (this.mobIsValidTarget(buff.owner, event.target)) {
            Mob validTarget = this.getValidTarget(buff.owner, event.target.x, event.target.y);
            if (validTarget != null) {
               if (event.attacker instanceof RicochetableProjectile) {
                  RicochetableProjectile projectile = (RicochetableProjectile)event.attacker;
                  Projectile newProjectile = projectile.getRicochetProjectile(event.target.x, event.target.y, validTarget.x, validTarget.y, validTarget);
                  validTarget.getLevel().entityManager.projectiles.add(newProjectile);
               }
            }
         }
      }
   }

   private Mob getValidTarget(Mob owner, float fromX, float fromY) {
      return GameUtils.streamTargets(owner, GameUtils.rangeBounds(fromX, fromY, 160))
         .filter(m -> this.mobIsValidTarget(owner, m))
         .filter(m -> m.getDistance(fromX, fromY) <= 160.0F)
         .min(Comparator.comparing(m -> m.getDistance(fromX, fromY)))
         .orElse(null);
   }

   protected boolean mobIsValidTarget(Mob owner, Mob potentialTarget) {
      return owner.isHostile || potentialTarget.isHostile || potentialTarget instanceof TrainingDummyMob;
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "soldiercapset"), 400);
      return tooltips;
   }
}
