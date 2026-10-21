package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs;

import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.item.upgradeUtils.IntUpgradeValue;

public class SpideriteCrownSetBonusBuff extends SpideriteSetBonusBuff {
   public IntUpgradeValue maxSummons = new IntUpgradeValue().setBaseValue(2).setUpgradedValue(1.0F, 2);

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      super.init(buff, eventSubscriber);
      buff.setModifier(BuffModifiers.MAX_SUMMONS, this.maxSummons.getValue(buff.getUpgradeTier()));
   }
}
