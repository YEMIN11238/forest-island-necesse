package necesse.level.gameObject.happinessObject;

import java.awt.Color;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.HappinessObject;
import necesse.level.gameObject.furniture.ChairObject;

public class SheepChairObject extends ChairObject implements HappinessObject {
   public SheepChairObject(String textureName, Color mapColor, String... category) {
      super(textureName, ToolType.ALL, mapColor, category);
      this.rarity = Item.Rarity.RARE;
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(this.getHappinessObjectTooltip());
      return tooltips;
   }
}
