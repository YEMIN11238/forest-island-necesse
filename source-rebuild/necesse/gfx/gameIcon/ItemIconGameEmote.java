package necesse.gfx.gameIcon;

import java.awt.Color;
import java.awt.Dimension;
import necesse.engine.registries.ItemRegistry;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.gameTexture.GameSprite;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;

public class ItemIconGameEmote extends GameEmote {
   protected String itemStringID;

   public ItemIconGameEmote(String itemStringID) {
      this.itemStringID = itemStringID;
   }

   @Override
   public Dimension getDrawBoundingBox(int size) {
      return new Dimension(size, size);
   }

   @Override
   public void drawIcon(int drawX, int drawY, boolean isHovering, Color defaultColor, int size) {
      Item item = ItemRegistry.getItem(this.itemStringID);
      GameSprite sprite = item.getItemSprite(new InventoryItem(item), null);
      if (sprite != null) {
         TextureDrawOptionsEnd drawOptions = sprite.initDraw();
         if (size > 0) {
            drawOptions.size(size, true);
         }

         drawOptions.draw(drawX, drawY);
      }
   }
}
