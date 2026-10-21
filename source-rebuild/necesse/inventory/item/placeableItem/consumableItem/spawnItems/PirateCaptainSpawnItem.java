package necesse.inventory.item.placeableItem.consumableItem.spawnItems;

import java.awt.geom.Line2D;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketChatMessage;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.hostile.pirates.PirateCaptainMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.placeableItem.consumableItem.ConsumableItem;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;

public class PirateCaptainSpawnItem extends ConsumableItem {
   public PirateCaptainSpawnItem() {
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
      }

      int tileX;
      int tileY;
      if (player == null) {
         tileX = GameMath.getTileCoordinate(x);
         tileY = GameMath.getTileCoordinate(y);
      } else {
         tileX = player.getTileX();
         tileY = player.getTileY();
      }

      Region regionByTile = level.regionManager.getRegionByTile(tileX, tileY, false);
      return regionByTile != null && regionByTile.isPirateVillageRegion ? null : "notpiratevillage";
   }

   @Override
   public InventoryItem onAttemptPlace(Level level, int x, int y, PlayerMob player, InventoryItem item, GNDItemMap mapContent, String error) {
      if (level.isServer() && player != null && player.isServerClient() && error.equals("inincursion")) {
         player.getServerClient().sendChatMessage(new LocalMessage("misc", "cannotsummoninincursion"));
      } else if (level.isServer() && player != null && player.isServerClient() && error.equals("notpiratevillage")) {
         player.getServerClient().sendChatMessage(new LocalMessage("misc", "hastobeinpiratevillage"));
      }

      return super.onAttemptPlace(level, x, y, player, item, mapContent, error);
   }

   @Override
   public InventoryItem onPlace(Level level, int x, int y, PlayerMob player, int seed, InventoryItem item, GNDItemMap mapContent) {
      if (level.isServer()) {
         if (level instanceof IncursionLevel) {
            GameMessage summonError = ((IncursionLevel)level).canSummonBoss("piratecaptain");
            if (summonError != null) {
               if (player != null && player.isServerClient()) {
                  player.getServerClient().sendChatMessage(summonError);
               }

               return item;
            }
         }

         System.out.println("Pirate Captain has been summoned at " + level.getIdentifier() + ".");
         float angle = GameRandom.globalRandom.nextInt(360);
         float nx = (float)Math.cos(Math.toRadians(angle));
         float ny = (float)Math.sin(Math.toRadians(angle));
         float distance = 384.0F;
         PirateCaptainMob mob = (PirateCaptainMob)MobRegistry.getMob("piratecaptain", level);
         mob.markItemSummoned();
         level.entityManager.addMob(mob, player.getX() + (int)(nx * distance), player.getY() + (int)(ny * distance));
         level.getServer().network.sendToClientsWithEntity(new PacketChatMessage(new LocalMessage("misc", "bosssummon", "name", mob.getLocalization())), mob);
         if (level instanceof IncursionLevel) {
            ((IncursionLevel)level).onBossSummoned(mob);
         }
      }

      if (this.isSingleUse(player)) {
         item.setAmount(item.getAmount() - 1);
      }

      return item;
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "messageinabottletip1"));
      tooltips.add(Localization.translate("itemtooltip", "messageinabottletip2"));
      return tooltips;
   }

   @Override
   public String getTranslatedTypeName() {
      return Localization.translate("item", "relic");
   }
}
