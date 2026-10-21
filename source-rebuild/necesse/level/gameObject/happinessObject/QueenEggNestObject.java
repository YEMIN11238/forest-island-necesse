package necesse.level.gameObject.happinessObject;

import java.awt.Color;
import java.awt.Rectangle;
import necesse.engine.registries.ObjectRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.HappinessObject;
import necesse.level.gameObject.StaticMultiObject;

public class QueenEggNestObject extends StaticMultiObject implements HappinessObject {
   public QueenEggNestObject(int multiX, int multiY, int multiWidth, int multiHeight, int[] multiIDs, Rectangle fullCollision) {
      super(multiX, multiY, multiWidth, multiHeight, multiIDs, fullCollision, "queeneggnest");
      this.rarity = Item.Rarity.EPIC;
      this.mapColor = new Color(98, 104, 113);
      this.objectHealth = 50;
      this.toolType = ToolType.ALL;
      this.isLightTransparent = true;
      this.hoverHitbox = new Rectangle(0, -32, 32, 64);
      this.setItemCategory("objects", "decorations");
      this.setCraftingCategory("objects", "decorations");
   }

   public static int[] registerQueenEggNest() {
      int[] ids = new int[2];
      Rectangle collision = new Rectangle(16, 8, 32, 16);
      ids[0] = ObjectRegistry.registerObject("queeneggnest", new QueenEggNestObject(0, 0, 2, 1, ids, collision), 750.0F, true, false);
      ids[1] = ObjectRegistry.registerObject("queeneggnest2", new QueenEggNestObject(1, 0, 2, 1, ids, collision), 0.0F, false);
      return ids;
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(this.getHappinessObjectTooltip());
      return tooltips;
   }
}
