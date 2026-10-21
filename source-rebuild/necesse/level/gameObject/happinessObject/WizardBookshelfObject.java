package necesse.level.gameObject.happinessObject;

import java.awt.Color;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.HappinessObject;
import necesse.level.gameObject.container.BookshelfObject;

public class WizardBookshelfObject extends BookshelfObject implements HappinessObject {
   public WizardBookshelfObject(String textureName, ToolType toolType, Color mapColor, String... category) {
      super(textureName, toolType, mapColor, category);
      this.rarity = Item.Rarity.RARE;
   }

   public WizardBookshelfObject(String textureName, Color mapColor, String... category) {
      this(textureName, ToolType.ALL, mapColor, category);
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(this.getHappinessObjectTooltip());
      return tooltips;
   }
}
