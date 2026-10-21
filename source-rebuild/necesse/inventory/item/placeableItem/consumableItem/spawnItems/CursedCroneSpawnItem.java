package necesse.inventory.item.placeableItem.consumableItem.spawnItems;

import java.awt.geom.Line2D;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketMobChat;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.placeableItem.consumableItem.ConsumableItem;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.CursedCroneArenasLevelData;

public class CursedCroneSpawnItem extends ConsumableItem {
   public CursedCroneSpawnItem() {
      super(1, true);
      this.itemCooldownTime.setBaseValue(2000);
      this.setItemCategory("consumable", "bossitems");
      this.dropsAsMatDeathPenalty = true;
      this.keyWords.add("boss");
      this.rarity = Item.Rarity.LEGENDARY;
      this.worldDrawSize = 32;
      this.incinerationTimeMillis = 30000;
   }

   @Override
   public String canPlace(Level level, int x, int y, PlayerMob player, Line2D playerPositionLine, InventoryItem item, GNDItemMap mapContent) {
      if (level instanceof IncursionLevel) {
         return "inincursion";
      } else {
         return level.entityManager.mobs.streamInRegionsShape(GameUtils.rangeTileBounds(x, y, 150), 0).anyMatch(m -> m.getStringID().equals("thecursedcrone"))
            ? "alreadyspawned"
            : null;
      }
   }

   @Override
   public InventoryItem onPlace(Level level, int x, int y, PlayerMob player, int seed, InventoryItem item, GNDItemMap mapContent) {
      if (level.isServer()) {
         if (level instanceof IncursionLevel) {
            GameMessage summonError = ((IncursionLevel)level).canSummonBoss("thecursedcrone");
            if (summonError != null) {
               if (player != null && player.isServerClient()) {
                  player.getServerClient().sendChatMessage(summonError);
               }

               return item;
            }
         }

         GameMessage error = CursedCroneArenasLevelData.startFight(level, player.getTileX(), player.getTileY());
         if (error != null) {
            player.getServerClient().sendChatMessage(error);
         } else {
            System.out.println("Cursed Crone has been summoned at " + level.getIdentifier() + ".");
            if (this.isSingleUse(player)) {
               item.setAmount(item.getAmount() - 1);
            }
         }
      }

      return item;
   }

   @Override
   public InventoryItem onAttemptPlace(Level level, int x, int y, PlayerMob player, InventoryItem item, GNDItemMap mapContent, String error) {
      if (level.isServer() && error.equals("notinarena")) {
         level.getServer().network.sendPacket(new PacketMobChat(player.getUniqueID(), "itemtooltip", "callernoarena"), player.getServerClient());
      }

      return item;
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "spiriturntip1"));
      tooltips.add(Localization.translate("itemtooltip", "spiriturntip2"));
      return tooltips;
   }

   @Override
   public String getTranslatedTypeName() {
      return Localization.translate("item", "relic");
   }
}
