package necesse.level.gameObject.container;

import java.awt.Color;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.toolItem.ToolType;

public class MimicStorageBoxInventoryObject extends StorageBoxInventoryObject {
   public MimicStorageBoxInventoryObject(String textureName, int slots, ToolType toolType, Color mapColor) {
      super(textureName, slots, toolType, mapColor);
   }

   public MimicStorageBoxInventoryObject(String textureName, int slots, Color mapColor, String... category) {
      super(textureName, slots, mapColor, category);
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(GameUtils.getDevQuoteTooltip("itemtooltip", "mimicchestquote", "Stormes"));
      return tooltips;
   }
}
