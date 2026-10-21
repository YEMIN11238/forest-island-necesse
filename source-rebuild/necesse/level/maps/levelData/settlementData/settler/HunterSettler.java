package necesse.level.maps.levelData.settlementData.settler;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.friendly.human.humanShop.HunterHumanMob;
import necesse.gfx.HumanLook;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;

public class HunterSettler extends Settler {
   public static ArrayList<String> spawnBiomes = new ArrayList<>(Arrays.asList("forest", "plains", "desert", "swamp", "snow"));
   public static double spawnChance = GameMath.getAverageSuccessRuns(120.0);

   public HunterSettler() {
      super("hunterhuman");
   }

   @Override
   public GameMessage getAcquireTip() {
      return new LocalMessage("settlement", "huntertip");
   }

   @Override
   public void setDefaultArmor(HumanDrawOptions drawOptions, int settlerSeed, HumanLook look, boolean customLook) {
      drawOptions.helmet(new InventoryItem("hunterhood"));
      drawOptions.chestplate(new InventoryItem("huntershirt"));
      drawOptions.boots(new InventoryItem("hunterboots"));
   }

   @Override
   public double getSpawnChance(Server server, ServerClient client, Level level) {
      return !level.isCave && spawnBiomes.contains(level.getBiome(client.playerMob.getTileX(), client.playerMob.getTileY()).getStringID()) ? spawnChance : 0.0;
   }

   @Override
   public void spawnAtClient(Server server, ServerClient client, Level level) {
      if (!level.entityManager
         .mobs
         .streamInRegionsInTileRange(client.playerMob.getX(), client.playerMob.getY(), SETTLER_SPAWN_AREA.maxSpawnDistance * 2)
         .anyMatch(m -> m.getStringID().equals(this.mobStringID))) {
         HunterHumanMob mob = (HunterHumanMob)MobRegistry.getMob(this.mobStringID, level);
         mob.setLost(true);
         Point spawnPos = this.getSpawnLocation(client, level, mob, SETTLER_SPAWN_AREA);
         if (spawnPos != null) {
            level.entityManager.addMob(mob, spawnPos.x, spawnPos.y);
         }
      }
   }
}
