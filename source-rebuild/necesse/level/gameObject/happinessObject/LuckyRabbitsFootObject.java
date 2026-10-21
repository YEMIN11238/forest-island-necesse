package necesse.level.gameObject.happinessObject;

import java.awt.Color;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.HappinessObject;
import necesse.level.gameObject.TableDecorationObject;

public class LuckyRabbitsFootObject extends TableDecorationObject implements HappinessObject {
   public LuckyRabbitsFootObject(String textureName, ToolType toolType, int holderDrawXOffset, int holderDrawYOffset) {
      super(textureName, toolType, new Color(182, 115, 53), 14, 16, holderDrawXOffset, holderDrawYOffset);
      this.rarity = Item.Rarity.RARE;
   }

   public LuckyRabbitsFootObject(String textureName, int holderDrawXOffset, int holderDrawYOffset) {
      this(textureName, ToolType.ALL, holderDrawXOffset, holderDrawYOffset);
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(this.getHappinessObjectTooltip());
      return tooltips;
   }
}
