package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.OneWorldNPCVillageData;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;

public class TouristSettlerPersonality extends SettlerPersonality {
   public static int HAPPY_THOUGHT_DURATION_SECONDS = 3600;

   public TouristSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_happy", new SimpleSettlerThought("personalities", "touristhappy", 50, 1).setPersonalitySubmitter(this)
      );
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_idle", new SimpleSettlerThought("personalities", "touristidle", 0).setPersonalitySubmitter(this)
      );
   }

   @Override
   public void settlementTick(ServerSettlementData data) {
      super.settlementTick(data);
      if (this.isInAnotherSettlement(data)) {
         this.mob.getActiveThoughts().addThoughtSeconds(this.getStringID() + "_happy", HAPPY_THOUGHT_DURATION_SECONDS);
      }
   }

   public boolean isInAnotherSettlement(ServerSettlementData data) {
      if (this.mob.adventureParty.isInAdventureParty()) {
         if (data.getLevel().isSamePlace(this.mob.getLevel()) && data.networkData.isTileWithinBounds(this.mob.getTileX(), this.mob.getTileY())) {
            return false;
         }

         OneWorldNPCVillageData villageData = OneWorldNPCVillageData.getVillageData(data.getLevel(), false);
         if (villageData != null && villageData.isVillageTile(this.mob.getTileX(), this.mob.getTileY())) {
            return true;
         }

         SettlementsWorldData settlementsData = SettlementsWorldData.getSettlementsData(data.getServer());
         return settlementsData.hasSettlementAtTile(this.mob.getLevel(), this.mob.getTileX(), this.mob.getTileY());
      } else {
         return false;
      }
   }

   @Override
   public SettlerThoughtsList getThoughts(SettlementBed bed) {
      if (!this.mob.getActiveThoughts().hasThought(this.getStringID() + "_happy")) {
         SettlerThoughtsList list = new SettlerThoughtsList();
         list.addThought(this.getStringID() + "_idle");
         return list;
      } else {
         return null;
      }
   }
}
