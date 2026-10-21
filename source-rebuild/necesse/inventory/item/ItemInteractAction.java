package necesse.inventory.item;

import java.awt.Rectangle;
import java.util.Comparator;
import java.util.LinkedList;
import necesse.engine.Settings;
import necesse.engine.input.InputPosition;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.ui.HUD;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import necesse.level.maps.TilePosition;

public interface ItemInteractAction {
   default int getLevelInteractAttackAnimTime(InventoryItem item, ItemAttackerMob attackerMob) {
      return item.item.getAttackAnimTime(item, attackerMob);
   }

   default int getLevelInteractCooldownTime(InventoryItem item, ItemAttackerMob attackerMob) {
      return item.item.getAttackCooldownTime(item, attackerMob);
   }

   default int getMobInteractAnimTime(InventoryItem item, ItemAttackerMob attackerMob) {
      return this.getLevelInteractAttackAnimTime(item, attackerMob);
   }

   default int getMobInteractCooldownTime(InventoryItem item, ItemAttackerMob attackerMob) {
      return this.getLevelInteractCooldownTime(item, attackerMob);
   }

   default boolean canMobInteract(Level level, Mob mob, ItemAttackerMob attackerMob, InventoryItem item) {
      return false;
   }

   default void setupMobInteractMapContent(GNDItemMap map, Level level, Mob targetMob, ItemAttackerMob attackerMob, InventoryItem item) {
   }

   default InventoryItem onMobInteract(
      Level level, Mob targetMob, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, ItemAttackSlot slot, int seed, GNDItemMap mapContent
   ) {
      return item;
   }

   default void showMobInteract(Level level, Mob targetMob, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, int seed, GNDItemMap mapContent) {
   }

   default ItemControllerInteract getControllerInteract(
      Level level,
      PlayerMob player,
      InventoryItem item,
      boolean beforeObjectInteract,
      int interactDir,
      LinkedList<Rectangle> mobInteractBoxes,
      LinkedList<Rectangle> tileInteractBoxes
   ) {
      return beforeObjectInteract && !this.overridesObjectInteract(level, player, item)
         ? null
         : tileInteractBoxes.stream()
            .flatMap(r -> {
               LinkedList<TilePosition> tilePositions = new LinkedList<>();

               for (int i = 0; i < r.width; i++) {
                  for (int j = 0; j < r.height; j++) {
                     tilePositions.add(new TilePosition(level, r.x + i, r.y + j));
                  }
               }

               return tilePositions.stream();
            })
            .filter(tp -> {
               int levelX = tp.tileX * 32 + 16;
               int levelY = tp.tileY * 32 + 16;
               return this.canLevelInteract(level, levelX, levelY, player, item);
            })
            .min(Comparator.comparingDouble(tp -> player.getDistance(tp.tileX * 32 + 16, tp.tileY * 32 + 16)))
            .map(
               tp -> {
                  int levelX = tp.tileX * 32 + 16;
                  int levelY = tp.tileY * 32 + 16;
                  return new ItemControllerInteract(levelX, levelY) {
                     @Override
                     public DrawOptions getDrawOptions(GameCamera camera) {
                        Rectangle selectBox = new Rectangle(tp.tileX, tp.tileY, 1, 1);
                        return HUD.tileBoundOptions(camera, Settings.UI.controllerFocusBoundsColor, true, selectBox);
                     }

                     @Override
                     public void onCurrentlyFocused(GameCamera camera) {
                        GameTooltipManager.setTooltipsInteractFocus(
                           InputPosition.fromScenePos(WindowManager.getWindow().getInput(), camera.getDrawX(tp.tileX * 32), camera.getDrawY(tp.tileY * 32))
                        );
                        item.item.onMouseHoverTile(item, camera, player, this.levelX, this.levelY, tp, false);
                     }
                  };
               }
            )
            .orElse(null);
   }

   default boolean canLevelInteract(Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item) {
      return false;
   }

   default void setupLevelInteractMapContent(GNDItemMap map, Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item) {
   }

   default InventoryItem onLevelInteract(
      Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, ItemAttackSlot slot, int seed, GNDItemMap mapContent
   ) {
      return item;
   }

   default void showLevelInteract(Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, int seed, GNDItemMap mapContent) {
   }

   default boolean overridesObjectInteract(Level level, PlayerMob player, InventoryItem item) {
      return false;
   }

   default boolean getConstantInteract(InventoryItem item) {
      return false;
   }
}
