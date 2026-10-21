package necesse.inventory.item.miscItem;

import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameColor;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;
import necesse.level.maps.levelData.settlementData.settler.Settler;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.SettlerRomancePersonality;

public class PrettyFlowerItem extends Item {
   public PrettyFlowerItem() {
      super(1);
      this.rarity = Item.Rarity.UNCOMMON;
      this.setItemCategory("misc");
      this.setItemCategory(ItemCategory.craftingManager, "misc");
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "prettyflowertip1"));
      GameMessage qolRequirement = Settler.getQualityOfLife(SettlerRomancePersonality.MIN_QOL_LEVEL_TO_DATE).getDescription();
      StringTooltips qolTip = new StringTooltips(Localization.translate("itemtooltip", "prettyflowertip2", "qol", qolRequirement), GameColor.LIGHT_GRAY, 350);
      tooltips.add(qolTip);
      return tooltips;
   }
}
