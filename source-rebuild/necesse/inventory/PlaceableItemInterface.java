package necesse.inventory;

import java.awt.Rectangle;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.client.Client;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.camera.GameCamera;
import necesse.level.maps.Level;

public interface PlaceableItemInterface {
   static int getPlaceRange(ItemAttackerMob attackerMob) {
      return (int)((3.5F + (attackerMob == null ? 0.0F : attackerMob.buffManager.getModifier(BuffModifiers.BUILD_RANGE))) * 32.0F);
   }

   default int getPlaceRange(InventoryItem item, ItemAttackerMob attackerMob) {
      return getPlaceRange(attackerMob);
   }

   void drawPlacePreview(Level var1, int var2, int var3, GameCamera var4, PlayerMob var5, InventoryItem var6, PlayerInventorySlot var7);

   default void drawMapPlacePreview(
      Level level,
      int x,
      int y,
      TickManager tickManager,
      Client client,
      int drawX,
      int drawY,
      double tileScale,
      Rectangle drawBounds,
      boolean isMinimap,
      PlayerMob player,
      InventoryItem item,
      PlayerInventorySlot slot
   ) {
   }
}
