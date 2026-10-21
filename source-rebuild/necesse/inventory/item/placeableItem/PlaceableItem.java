package necesse.inventory.item.placeableItem;

import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;
import java.util.stream.Stream;
import necesse.engine.GlobalData;
import necesse.engine.input.Control;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketPlayerPlaceItem;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlaceableItemInterface;
import necesse.inventory.PlayerInventorySlot;
import necesse.inventory.item.Item;
import necesse.level.maps.Level;

public class PlaceableItem extends Item implements PlaceableItemInterface {
   private final boolean singleUse;
   protected boolean controllerIsTileBasedPlacing;

   public PlaceableItem(int stackSize, boolean singleUse) {
      super(stackSize);
      this.singleUse = singleUse;
   }

   @Override
   public Point getControllerAttackLevelPos(Level level, float aimDirX, float aimDirY, PlayerMob player, InventoryItem item) {
      Float normalize = GameMath.normalize(aimDirX, aimDirY);
      int range = this.getPlaceRange(item, player);
      if (this.controllerIsTileBasedPlacing) {
         Stream<Point> positions = GameUtils.streamTileCoordinatesBetweenPoints(
            level,
            new Float(player.x + normalize.x * 32.0F, player.y + normalize.y * 32.0F),
            new Float(player.x + normalize.x * range, player.y + normalize.y * range)
         );
         if (!Control.MOUSE1.isDown()) {
            player.currentAttackLastPlacePosition = null;
         }

         Point foundPosition = positions.filter(
               tilePos -> {
                  Point placePosx = new Point(tilePos.x * 32 + 16, tilePos.y * 32 + 16);
                  GNDItemMap mapx = new GNDItemMap();
                  this.setupAttackMapContent(mapx, level, placePosx.x, placePosx.y, player, -1, item);
                  return placePosx.equals(player.currentAttackLastPlacePosition)
                     ? true
                     : this.canPlace(level, placePosx.x, placePosx.y, player, null, item, mapx) == null;
               }
            )
            .findFirst()
            .orElse(null);
         boolean lastAttackOverlap = foundPosition != null && foundPosition.equals(player.currentAttackLastPlacePosition);
         player.constantAttack = this.getConstantUse(item) && !lastAttackOverlap;
         if (lastAttackOverlap) {
            return player.currentAttackLastPlacePosition;
         } else {
            return foundPosition != null
               ? new Point(foundPosition.x * 32 + 16, foundPosition.y * 32 + 16)
               : new Point((int)(player.x + aimDirX * range), (int)(player.y + aimDirY * range));
         }
      } else {
         Point currentAttackLastPlaceTilePosition = player.currentAttackLastPlacePosition != null
            ? new Point(
               GameMath.getTileCoordinate(player.currentAttackLastPlacePosition.x), GameMath.getTileCoordinate(player.currentAttackLastPlacePosition.y)
            )
            : null;

         for (int i = 32; i < range; i += 10) {
            Point placePos = new Point((int)(player.x + normalize.x * i), (int)(player.y + normalize.y * i));
            Point placePosTile = new Point(GameMath.getTileCoordinate(placePos.x), GameMath.getTileCoordinate(placePos.y));
            GNDItemMap map = new GNDItemMap();
            this.setupAttackMapContent(map, level, placePos.x, placePos.y, player, -1, item);
            if (placePosTile.equals(currentAttackLastPlaceTilePosition)) {
               player.constantAttack = false;
               return player.currentAttackLastPlacePosition;
            }

            if (this.canPlace(level, placePos.x, placePos.y, player, null, item, map) == null) {
               player.constantAttack = this.getConstantUse(item);
               return placePos;
            }
         }

         return super.getControllerAttackLevelPos(level, aimDirX, aimDirY, player, item);
      }
   }

   @Override
   public void drawControllerAimPos(GameCamera camera, Level level, PlayerMob player, InventoryItem item) {
   }

   @Override
   public String canAttack(Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item) {
      return attackerMob.isPlayer ? null : "";
   }

   @Override
   public InventoryItem onAttack(
      Level level,
      int x,
      int y,
      ItemAttackerMob attackerMob,
      int attackHeight,
      InventoryItem item,
      ItemAttackSlot slot,
      int animAttack,
      int seed,
      GNDItemMap mapContent
   ) {
      if (!attackerMob.isPlayer) {
         return item;
      }

      PlayerMob player = (PlayerMob)attackerMob;
      String error = this.canPlace(level, x, y, player, null, item, mapContent);
      if (level.isServer() && player.isServerClient() && this.shouldSendToOtherClients(level, x, y, player, item, error, mapContent)) {
         ServerClient serverClient = player.getServerClient();
         level.getServer()
            .network
            .sendToClientsWithEntityExcept(new PacketPlayerPlaceItem(level, serverClient, item, x, y, error, mapContent), serverClient.playerMob, serverClient);
      }

      if (error == null) {
         player.currentAttackLastPlacePosition = new Point(x, y);
         return this.onPlace(level, x, y, player, seed, item, mapContent);
      }

      if (level.isClient() && GlobalData.debugActive()) {
         System.out.println(this.getStringID() + " place failed: " + error);
      }

      return this.onAttemptPlace(level, x, y, player, item, mapContent, error);
   }

   public boolean shouldSendToOtherClients(Level level, int x, int y, PlayerMob player, InventoryItem item, String error, GNDItemMap mapContent) {
      return false;
   }

   public String canPlace(Level level, int x, int y, PlayerMob player, Line2D playerPositionLine, InventoryItem item, GNDItemMap mapContent) {
      return !this.isInPlaceRange(level, x, y, player, playerPositionLine, item) ? "outofrange" : null;
   }

   public boolean isInPlaceRange(Level level, int placeX, int placeY, PlayerMob player, Line2D playerPositionLine, InventoryItem item) {
      return this.isInPlaceRange(level, placeX, placeY, player, playerPositionLine, item, false);
   }

   public boolean isInPlaceRange(
      Level level, int placeX, int placeY, PlayerMob player, Line2D playerPositionLine, InventoryItem item, boolean ignoreServerClientExtraRange
   ) {
      Point placeDistancePoint = new Point(placeX, placeY);
      Point2D playerPositionPoint;
      if (playerPositionLine != null) {
         playerPositionPoint = GameMath.getClosestPointOnLine(playerPositionLine, placeDistancePoint, false);
      } else {
         playerPositionPoint = player.getPositionPoint();
      }

      int placeRange = this.getPlaceRange(item, player) + (player != null && !ignoreServerClientExtraRange && player.isServerClient() ? 32 : 0);
      return playerPositionPoint.distance(placeDistancePoint) <= placeRange;
   }

   public int getAttackHandlerPlaceCooldown(InventoryItem item, ItemAttackerMob attackerMob) {
      return ((PlayerMob)attackerMob).hasGodMode()
         ? 0
         : (int)(this.getFlatAttackAnimTime(item) * (1.0F / attackerMob.buffManager.getModifier(BuffModifiers.BUILDING_SPEED)));
   }

   public InventoryItem onPlace(Level level, int x, int y, PlayerMob player, int seed, InventoryItem item, GNDItemMap mapContent) {
      return item;
   }

   public void onOtherPlayerPlace(Level level, int x, int y, PlayerMob player, InventoryItem item, GNDItemMap mapContent) {
   }

   public InventoryItem onAttemptPlace(Level level, int x, int y, PlayerMob player, InventoryItem item, GNDItemMap mapContent, String error) {
      return item;
   }

   public void onOtherPlayerPlaceAttempt(Level level, int x, int y, PlayerMob player, InventoryItem item, GNDItemMap mapContent, String error) {
   }

   @Override
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      drawOptions.swingRotation(attackProgress);
   }

   @Override
   public boolean getConstantUse(InventoryItem item) {
      return true;
   }

   @Override
   public void drawPlacePreview(Level level, int x, int y, GameCamera camera, PlayerMob player, InventoryItem item, PlayerInventorySlot slot) {
   }

   public boolean isSingleUse(PlayerMob player) {
      return player != null && player.hasGodMode() ? false : this.singleUse;
   }
}
