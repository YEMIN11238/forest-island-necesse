package necesse.level.maps.levelData.settlementData.settler;

import java.awt.Point;
import java.util.function.Supplier;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.humanShop.MageHumanMob;
import necesse.gfx.HumanLook;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.dungeon.DungeonArenaLevel;
import necesse.level.maps.biomes.dungeon.DungeonLevel;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class MageSettler extends Settler {
   public static double spawnChance = GameMath.getAverageSuccessRuns(120.0);

   public MageSettler() {
      super("magehuman");
   }

   @Override
   public void loadTextures() {
      this.texture = GameTexture.fromFile("mobs/icons/magehuman");
   }

   @Override
   public GameMessage getAcquireTip() {
      return new LocalMessage("settlement", "magetip");
   }

   @Override
   public void setDefaultArmor(HumanDrawOptions drawOptions, int settlerSeed, HumanLook look, boolean customLook) {
      GameRandom random = new GameRandom(settlerSeed).nextSeeded(945);
      if (random.getChance(0.5F)) {
         drawOptions.helmet(new InventoryItem("crimsonhat"));
         drawOptions.chestplate(new InventoryItem("crimsonrobe"));
         drawOptions.boots(new InventoryItem("crimsonshoes"));
      } else {
         drawOptions.helmet(new InventoryItem("magehat"));
         drawOptions.chestplate(new InventoryItem("magerobe"));
         drawOptions.boots(new InventoryItem("mageshoes"));
      }
   }

   @Override
   public double getSpawnChance(Server server, ServerClient client, Level level) {
      return level instanceof DungeonLevel && !(level instanceof DungeonArenaLevel) ? spawnChance : 0.0;
   }

   @Override
   public void spawnAtClient(Server server, ServerClient client, Level level) {
      if (!level.entityManager.mobs.stream().anyMatch(m -> m.getStringID().equals(this.mobStringID))) {
         MageHumanMob mob = (MageHumanMob)MobRegistry.getMob(this.mobStringID, level);
         mob.setTrapped();
         mob.canDespawn = true;
         Point spawnPos = this.getSpawnLocation(client, level, mob, SETTLER_SPAWN_AREA);
         if (spawnPos != null) {
            level.entityManager.addMob(mob, spawnPos.x, spawnPos.y);
         }
      }
   }

   @Override
   public void addNewRecruitSettler(ServerSettlementData data, boolean isRandomEvent, TicketSystemList<Supplier<HumanMob>> ticketSystem) {
      if ((isRandomEvent || !this.doesSettlementHaveThisSettler(data)) && data.storyObjectives.hasCompletedStoryObjectiveOrHigher("defeatvoidwizard")) {
         ticketSystem.addObject(100, this.getNewRecruitMob(data));
      }
   }
}
