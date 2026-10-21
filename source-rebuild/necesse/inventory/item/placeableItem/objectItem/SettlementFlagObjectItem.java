package necesse.inventory.item.placeableItem.objectItem;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketChangeObject;
import necesse.engine.network.packet.PacketOpenContainer;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.Zoning;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerInventorySlot;
import necesse.inventory.container.SettlementNameContainer;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.ObjectPlaceOption;
import necesse.level.gameObject.SettlementFlagObject;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.OneWorldNPCVillageData;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementFlagObjectItem extends ObjectItem {
   public SettlementFlagObjectItem(SettlementFlagObject object) {
      super(object);
      this.itemCooldownTime.setBaseValue(2000);
   }

   @Override
   public String canPlace(Level level, ObjectPlaceOption po, PlayerMob player, Line2D playerPositionLine, InventoryItem item, GNDItemMap mapContent) {
      if (level.isServer() && player != null) {
         Server server = level.getServer();
         SettlementsWorldData settlementsData = SettlementsWorldData.getSettlementsData(server);
         ServerClient client = player.getServerClient();
         int max = server.world.settings.maxSettlementsPerPlayer;
         if (max >= 0) {
            long current = settlementsData.streamSettlements()
               .filter(e -> e.getOwnerAuth() == client.authentication && !e.isTileWithinBounds(po.tileX, po.tileY))
               .count();
            if (current >= max) {
               return "maxsettlements";
            }
         }

         ServerSettlementData serverData = settlementsData.getServerDataAtTile(level.getIdentifier(), po.tileX, po.tileY);
         int flagTier = serverData != null ? serverData.getFlagTier() : 0;
         if (!settlementsData.canPlaceSettlementFlagAt(level.getIdentifier(), po.tileX, po.tileY, flagTier)) {
            return "closesettlement";
         }

         OneWorldNPCVillageData villageData = OneWorldNPCVillageData.getVillageData(level, false);
         if (villageData != null && !villageData.canPlaceSettlementFlagAt(po.tileX, po.tileY, flagTier)) {
            return "closevillage";
         }
      }

      return super.canPlace(level, po, player, playerPositionLine, item, mapContent);
   }

   @Override
   public InventoryItem onAttemptPlace(Level level, int x, int y, PlayerMob player, InventoryItem item, GNDItemMap mapContent, String error) {
      boolean sendUpdate = false;
      switch (error) {
         case "maxsettlements":
            Server server = level.getServer();
            player.getServerClient().sendChatMessage(new LocalMessage("misc", "maxsettlementsreached", "count", server.world.settings.maxSettlementsPerPlayer));
            sendUpdate = true;
            break;
         case "closesettlement":
            player.getServerClient().sendChatMessage(new LocalMessage("misc", "tooclosesettlement"));
            sendUpdate = true;
            break;
         case "closevillage":
            player.getServerClient().sendChatMessage(new LocalMessage("misc", "tooclosevillage"));
            sendUpdate = true;
      }

      if (sendUpdate) {
         ObjectPlaceOption po = this.getPlaceOptionFromMap(mapContent);
         if (po != null) {
            player.getServerClient().sendPacket(new PacketChangeObject(level, 0, po.tileX, po.tileY));
         }

         player.endAttackHandler(false);
         return item;
      } else {
         return super.onAttemptPlace(level, x, y, player, item, mapContent, error);
      }
   }

   @Override
   public boolean onPlaceObject(GameObject object, Level level, int layerID, int tileX, int tileY, int rotation, ServerClient client, InventoryItem item) {
      if (client != null) {
         Server server = level.getServer();
         if (server.isSingleplayer() || server.isHosted()) {
            SettlementsWorldData settlementsData = SettlementsWorldData.getSettlementsData(server);
            long current = settlementsData.streamSettlements()
               .filter(e -> e.getOwnerAuth() == client.authentication && !e.isTileWithinBounds(tileX, tileY))
               .count();
            if (current > 0L) {
               client.sendChatMessage(new LocalMessage("misc", "multisettlementstip"));
            }
         }
      }

      boolean success = super.onPlaceObject(object, level, layerID, tileX, tileY, rotation, client, item);
      if (client != null) {
         SettlementsWorldData worldData = SettlementsWorldData.getSettlementsData(level);
         ServerSettlementData serverData = worldData.getOrCreateServerData(level, tileX, tileY);
         serverData.networkData.setFlagTile(tileX, tileY);
         worldData.updateSettlement(serverData);
         serverData.networkData.setOwner(client);
         serverData.clearOutsideBounds();
         serverData.networkData.markDirty(true);
         if (client.achievementsLoaded()) {
            client.achievements().START_SETTLEMENT.markCompleted(client);
         }

         if (!serverData.networkData.isSettlementNameSet()) {
            ContainerRegistry.openAndSendContainer(
               client, new PacketOpenContainer(ContainerRegistry.SETTLEMENT_NAME_CONTAINER, SettlementNameContainer.getContainerContent(serverData.uniqueID))
            );
         }
      }

      return success;
   }

   @Override
   public void drawPlacePreview(Level level, int x, int y, GameCamera camera, PlayerMob player, InventoryItem item, PlayerInventorySlot slot) {
      super.drawPlacePreview(level, x, y, camera, player, item, slot);
      String canPlace = this.canPlace(level, x, y, player, null, item, null);
      if (canPlace == null) {
         ObjectPlaceOption po = this.getBestPlaceOption(level, x, y, item, player, null, false);
         if (po != null) {
            int regionX = level.regionManager.getRegionCoordByTile(po.tileX);
            int regionY = level.regionManager.getRegionCoordByTile(po.tileY);
            int regionTileX = level.regionManager.getTileCoordByRegion(regionX);
            int regionTileY = level.regionManager.getTileCoordByRegion(regionY);
            Rectangle levelRectangle = new Rectangle(
               GameMath.getLevelCoordinate(regionTileX),
               GameMath.getLevelCoordinate(regionTileY),
               GameMath.getLevelCoordinate(16),
               GameMath.getLevelCoordinate(16)
            );
            SharedTextureDrawOptions options = Zoning.getRectangleDrawOptions(
               levelRectangle, new Color(1.0F, 1.0F, 1.0F, 0.3F), new Color(1.0F, 1.0F, 1.0F, 0.0F), 32, camera
            );
            options.draw();
         }
      }
   }
}
