package necesse.level.gameObject.happinessObject;

import java.awt.Color;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.HappinessObject;
import necesse.level.gameObject.TableDecorationObject;

public class WoodenDuckObject extends TableDecorationObject implements HappinessObject {
   public WoodenDuckObject(String textureName, ToolType toolType, int holderDrawXOffset, int holderDrawYOffset) {
      super(textureName, toolType, new Color(85, 182, 125), 14, 16, holderDrawXOffset, holderDrawYOffset);
      this.rarity = Item.Rarity.RARE;
   }

   public WoodenDuckObject(String textureName, int holderDrawXOffset, int holderDrawYOffset) {
      this(textureName, ToolType.ALL, holderDrawXOffset, holderDrawYOffset);
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(this.getHappinessObjectTooltip());
      return tooltips;
   }
}
