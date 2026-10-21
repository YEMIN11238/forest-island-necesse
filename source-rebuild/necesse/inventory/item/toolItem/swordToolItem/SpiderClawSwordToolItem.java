package necesse.inventory.item.toolItem.swordToolItem;

import necesse.engine.sound.SoundSettings;
import necesse.gfx.GameResources;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.CloseRangeWeaponsLootTable;

public class SpiderClawSwordToolItem extends SwordToolItem {
   public SpiderClawSwordToolItem() {
      super(550, CloseRangeWeaponsLootTable.closeRangeWeapons);
      this.rarity = Item.Rarity.UNCOMMON;
      this.attackAnimTime.setBaseValue(100);
      this.attackDamage.setBaseValue(16.0F).setUpgradedValue(1.0F, 64.16669F);
      this.attackRange.setBaseValue(45);
      this.knockback.setBaseValue(30);
      this.resilienceGain.setBaseValue(1.5F).setUpgradedValue(1.0F, 1.5F).setUpgradedValue(10.0F, 2.0F);
      this.canBeUsedForRaids = true;
      this.raidTicketsModifier = 0.5F;
      this.useForRaidsOnlyIfObtained = true;
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.spiderclaw).volume(0.3F);
   }
}
