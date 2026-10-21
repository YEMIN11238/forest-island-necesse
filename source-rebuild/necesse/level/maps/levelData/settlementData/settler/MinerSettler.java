package necesse.level.maps.levelData.settlementData.settler;

import java.awt.Point;
import java.util.function.Supplier;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.TicketSystemList;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.humanShop.MinerHumanMob;
import necesse.gfx.HumanLook;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class MinerSettler extends Settler {
   public static double spawnChance = GameMath.getAverageSuccessRuns(120.0);

   public MinerSettler() {
      super("minerhuman");
   }

   @Override
   public GameMessage getAcquireTip() {
      return new LocalMessage("settlement", "minertip");
   }

   @Override
   public void setDefaultArmor(HumanDrawOptions drawOptions, int settlerSeed, HumanLook look, boolean customLook) {
      drawOptions.helmet(new InventoryItem("minerhat"));
      drawOptions.chestplate(new InventoryItem("minershirt"));
      drawOptions.boots(new InventoryItem("minerboots"));
   }

   @Override
   public double getSpawnChance(Server server, ServerClient client, Level level) {
      return !level.getIdentifier().equals(LevelIdentifier.CAVE_IDENTIFIER) && !level.getIdentifier().equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER)
         ? 0.0
         : spawnChance;
   }

   @Override
   public void spawnAtClient(Server server, ServerClient client, Level level) {
      if (client.characterStats().mob_kills.getKills("evilsprotector") > 0) {
         if (!level.entityManager
            .mobs
            .streamInRegionsInTileRange(client.playerMob.getX(), client.playerMob.getY(), Settler.SETTLER_SPAWN_AREA.maxSpawnDistance * 2)
            .anyMatch(m -> m.getStringID().equals(this.mobStringID))) {
            MinerHumanMob mob = (MinerHumanMob)MobRegistry.getMob(this.mobStringID, level);
            mob.setLost(true);
            Point spawnPos = this.getSpawnLocation(client, level, mob, Settler.SETTLER_SPAWN_AREA);
            if (spawnPos != null) {
               level.entityManager.addMob(mob, spawnPos.x, spawnPos.y);
            }
         }
      }
   }

   @Override
   public void addNewRecruitSettler(ServerSettlementData data, boolean isRandomEvent, TicketSystemList<Supplier<HumanMob>> ticketSystem) {
      if (isRandomEvent || !this.doesSettlementHaveThisSettler(data)) {
         ticketSystem.addObject(75, this.getNewRecruitMob(data));
      }
   }
}
