package necesse.level.maps.levelData.settlementData.settler;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Supplier;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.TicketSystemList;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.humanShop.ExplorerHumanMob;
import necesse.gfx.HumanLook;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class ExplorerSettler extends Settler {
   public static ArrayList<String> spawnBiomes = new ArrayList<>(Arrays.asList("desert", "desertvillage"));
   public static double spawnChance = GameMath.getAverageSuccessRuns(120.0);

   public ExplorerSettler() {
      super("explorerhuman");
   }

   @Override
   public GameMessage getAcquireTip() {
      return new LocalMessage("settlement", "explorertip");
   }

   @Override
   public void setDefaultArmor(HumanDrawOptions drawOptions, int settlerSeed, HumanLook look, boolean customLook) {
      drawOptions.helmet(new InventoryItem("safarihat"));
      drawOptions.chestplate(new InventoryItem("safarishirt"));
      drawOptions.boots(new InventoryItem("safarishoes"));
   }

   @Override
   public double getSpawnChance(Server server, ServerClient client, Level level) {
      return level.isCave && spawnBiomes.contains(level.getBiome(client.playerMob.getTileX(), client.playerMob.getTileY()).getStringID()) ? spawnChance : 0.0;
   }

   @Override
   public void spawnAtClient(Server server, ServerClient client, Level level) {
      if (client.characterStats().mob_kills.getKills("voidwizard") > 0) {
         if (!level.entityManager
            .mobs
            .streamInRegionsInTileRange(client.playerMob.getX(), client.playerMob.getY(), Settler.SETTLER_SPAWN_AREA.maxSpawnDistance * 2)
            .anyMatch(m -> m.getStringID().equals(this.mobStringID))) {
            ExplorerHumanMob mob = (ExplorerHumanMob)MobRegistry.getMob(this.mobStringID, level);
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
      if ((isRandomEvent || !this.doesSettlementHaveThisSettler(data)) && data.storyObjectives.hasCompletedStoryObjectiveOrHigher("defeatpiratecaptain")) {
         ticketSystem.addObject(100, this.getNewRecruitMob(data));
      }
   }
}
