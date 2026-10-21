package necesse.inventory.item.toolItem.spearToolItem;

import necesse.engine.sound.SoundSettings;
import necesse.gfx.GameResources;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.SpearWeaponsLootTable;

public class VulturesTalonToolItem extends SpearToolItem {
   public VulturesTalonToolItem() {
      super(1050, SpearWeaponsLootTable.spearWeapons);
      this.rarity = Item.Rarity.RARE;
      this.attackAnimTime.setBaseValue(400);
      this.attackDamage.setBaseValue(40.0F).setUpgradedValue(1.0F, 70.000015F);
      this.attackRange.setBaseValue(150);
      this.knockback.setBaseValue(50);
      this.width = 12.0F;
      this.canBeUsedForRaids = true;
      this.raidTicketsModifier = 0.5F;
      this.useForRaidsOnlyIfObtained = true;
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.vulturesTalon).volume(0.3F);
   }
}
