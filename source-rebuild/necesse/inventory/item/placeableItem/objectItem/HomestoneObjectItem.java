package necesse.inventory.item.placeableItem.objectItem;

import java.awt.geom.Line2D;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.util.GameBlackboard;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.ObjectPlaceOption;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.NetworkSettlementData;

public class HomestoneObjectItem extends ObjectItem {
   public HomestoneObjectItem(GameObject object) {
      super(object);
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "placeinanysettlement"));
      return tooltips;
   }

   @Override
   public String canPlace(Level level, ObjectPlaceOption po, PlayerMob player, Line2D playerPositionLine, InventoryItem item, GNDItemMap mapContent) {
      String error = super.canPlace(level, po, player, playerPositionLine, item, mapContent);
      if (error != null) {
         return error;
      }

      boolean hasSettlement = SettlementsWorldData.getSettlementsData(level).hasSettlementAtTile(level, po.tileX, po.tileY);
      if (!hasSettlement) {
         return "notsettlement";
      }

      if (level.isServer() && player != null && player.isServerClient()) {
         NetworkSettlementData networkData = SettlementsWorldData.getSettlementsData(level).getNetworkDataAtTile(level.getIdentifier(), po.tileX, po.tileY);
         if (networkData != null && !networkData.doesClientHaveAccess(player.getServerClient())) {
            return "noaccess";
         }
      }

      return null;
   }

   @Override
   public InventoryItem onAttemptPlace(Level level, int x, int y, PlayerMob player, InventoryItem item, GNDItemMap mapContent, String error) {
      if (error.equals("noaccess")) {
         player.getServerClient().sendChatMessage(new LocalMessage("misc", "settlementnoaccess"));
         player.endAttackHandler(false);
         return item;
      } else {
         return super.onAttemptPlace(level, x, y, player, item, mapContent, error);
      }
   }
}
