package necesse.inventory.item;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.gameNetworkData.GNDItem;
import necesse.engine.network.gameNetworkData.GNDItemInventoryItem;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.ItemRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.gameTexture.GameSprite;
import necesse.inventory.InventoryItem;

public class WorkSpriteAttackItem extends Item {
   public WorkSpriteAttackItem() {
      super(1);
      this.attackAnimTime.setBaseValue(250);
   }

   @Override
   public GameMessage getNewLocalization() {
      return new StaticMessage("WORK_ATTACK");
   }

   @Override
   public GameSprite getAttackSprite(InventoryItem item, PlayerMob player) {
      GNDItem gndInvItem = item.getGndData().getItem("invItem");
      if (gndInvItem instanceof GNDItemInventoryItem) {
         InventoryItem swingItem = ((GNDItemInventoryItem)gndInvItem).invItem;
         return new GameSprite(swingItem.item.getItemSprite(swingItem, player), 24);
      }

      int itemID = item.getGndData().getInt("itemID");
      if (itemID != -1) {
         Item spriteItem = ItemRegistry.getItem(itemID);
         if (spriteItem != null) {
            return new GameSprite(spriteItem.getItemSprite(item, player), 24);
         }
      }

      return null;
   }

   @Override
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      attackProgress *= 2.0F;
      float rotationOffset = (float)Math.sin(attackProgress * Math.PI * 2.0) * 10.0F;
      if (attackProgress > 0.5F) {
         rotationOffset = -rotationOffset;
      }

      drawOptions.pointRotation(attackDirX, attackDirY, rotationOffset);
      if (attackProgress > 1.0F) {
         attackProgress--;
      }

      int xOffset = (int)(attackDirX * Math.sin(attackProgress * Math.PI) * 5.0) - (int)(attackDirX * 5.0F);
      int yOffset = (int)(attackDirY * Math.sin(attackProgress * Math.PI) * 5.0) - (int)(attackDirY * 5.0F);
      drawOptions.drawOffset(xOffset, yOffset);
   }

   public static InventoryItem setup(InventoryItem attackItem, InventoryItem swingItem, boolean inverted) {
      GNDItemMap gndData = attackItem.getGndData();
      if (swingItem == null) {
         gndData.setInt("itemID", -1);
      } else {
         gndData.setItem("invItem", new GNDItemInventoryItem(swingItem));
      }

      gndData.setBoolean("inverted", inverted);
      return attackItem;
   }

   public static InventoryItem setup(InventoryItem attackItem, int itemID, boolean inverted) {
      GNDItemMap gndData = attackItem.getGndData();
      gndData.setInt("itemID", itemID);
      gndData.setBoolean("inverted", inverted);
      return attackItem;
   }

   public static InventoryItem setup(InventoryItem attackItem, Item swingItem, boolean inverted) {
      return setup(attackItem, swingItem == null ? -1 : swingItem.getID(), inverted);
   }
}
