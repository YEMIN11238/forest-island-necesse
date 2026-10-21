package necesse.entity.mobs.buffs.staticBuffs;

import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;

public class ThornsPotionBuff extends Buff {
   @Override
   public boolean isPotionBuff() {
      return true;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }

   @Override
   public void onWasHit(ActiveBuff buff, MobWasHitEvent event) {
      super.onWasHit(buff, event);
      if (!event.wasPrevented && buff.owner.isServer()) {
         Mob attackOwner = event.attacker != null ? event.attacker.getAttackOwner() : null;
         boolean hasOwnerInChain = attackOwner != null && attackOwner.isInAttackOwnerChain(buff.owner);
         if (attackOwner != null && !hasOwnerInChain) {
            float dx = attackOwner.getX() - buff.owner.getX();
            float dy = attackOwner.getY() - buff.owner.getY();
            float damage = event.damage;
            if (attackOwner.isPlayer) {
               damage /= 2.0F;
            }

            attackOwner.isServerHit(new GameDamage(damage, 0.0F), dx, dy, 50.0F, buff.owner);
         }
      }
   }

   @Override
   public void updateLocalDisplayName() {
      this.displayName = new LocalMessage("item", this.getStringID());
   }
}
