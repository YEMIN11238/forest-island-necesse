package necesse.entity.mobs.buffs.staticBuffs;

import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;

public class PyromancyOnFireBuff extends Buff {
   public FloatUpgradeValue fireDamage = new FloatUpgradeValue(0.0F, 0.2F).setBaseValue(8.0F).setUpgradedValue(1.0F, 8.0F);

   public PyromancyOnFireBuff() {
      this.canCancel = false;
      this.isImportant = true;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.FIRE_DAMAGE_FLAT, this.fireDamage.getValue(buff.getUpgradeTier()));
      buff.setModifier(BuffModifiers.INCOMING_DAMAGE_MOD, 1.04F);
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      if (buff.owner.isVisible()) {
         Mob owner = buff.owner;
         owner.getLevel()
            .entityManager
            .addParticle(
               owner.x + (float)(GameRandom.globalRandom.nextGaussian() * 6.0),
               owner.y + (float)(GameRandom.globalRandom.nextGaussian() * 8.0),
               Particle.GType.IMPORTANT_COSMETIC
            )
            .movesConstant(owner.dx / 10.0F, owner.dy / 10.0F)
            .flameColor()
            .givesLight(0.0F, 0.5F)
            .height(16.0F);
      }
   }

   @Override
   public int getStackSize(ActiveBuff buff) {
      return 10;
   }
}
