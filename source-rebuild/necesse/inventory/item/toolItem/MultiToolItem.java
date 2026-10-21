package necesse.inventory.item.toolItem;

import necesse.engine.localization.Localization;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.ToolsLootTable;

public class MultiToolItem extends ToolDamageItem {
   public MultiToolItem(int enchantCost) {
      super(enchantCost, ToolsLootTable.tools);
      this.setItemCategory("equipment", "tools", "misc");
      this.keyWords.add("pickaxe, axe, shovel");
      this.toolType = ToolType.ALL;
      this.animAttacks = 2;
      this.width = 10.0F;
      this.attackAnimTime.setBaseValue(450);
      this.toolDps.setBaseValue(150);
      this.toolTier.setBaseValue(10.0F);
      this.attackDamage.setBaseValue(25.0F);
      this.attackRange.setBaseValue(50);
      this.knockback.setBaseValue(50);
      this.rarity = Item.Rarity.EPIC;
      this.enchantCost.setUpgradedValue(1.0F, 1400);
   }

   @Override
   protected void addToolTooltips(ListGameTooltips tooltips) {
      tooltips.add(Localization.translate("itemtooltip", "multitooltip"));
   }
}
