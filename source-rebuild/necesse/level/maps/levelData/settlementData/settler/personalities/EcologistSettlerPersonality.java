package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.SettlementRoom;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleScalingHappinessSettlerThought;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;

public class EcologistSettlerPersonality extends SettlerPersonality {
   public boolean hasRoom;
   public boolean hasFloorTiles;
   public int plantCount;

   public EcologistSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_solidfloor", new SimpleSettlerThought("personalities", "ecologistsolidfloor", 0).setPersonalitySubmitter(this)
      );
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_noplants", new SimpleSettlerThought("personalities", "ecologistnoplant", 0).setPersonalitySubmitter(this)
      );
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_organicfloor", new SimpleSettlerThought("personalities", "ecologistorganicfloor", 20).setPersonalitySubmitter(this)
      );
      LocalMessage somePlantsMessage = new LocalMessage("personalities", "ecologistsomeplants");
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_some", new SimpleScalingHappinessSettlerThought(somePlantsMessage, 4, -1, 1).setPersonalitySubmitter(this)
      );
   }

   @Override
   public void onRoomStatsCalculated(SettlementBed bed, SettlementRoom settlementRoom) {
      super.onRoomStatsCalculated(bed, settlementRoom);
      this.hasRoom = settlementRoom != null;
      this.hasFloorTiles = false;
      this.plantCount = 0;
      if (settlementRoom != null) {
         this.plantCount = settlementRoom.getRoomProperty("plant");
         this.hasFloorTiles = settlementRoom.streamTilesInRoom().anyMatch(tile -> !tile.isOrganic);
      }
   }

   @Override
   public SettlerThoughtsList getThoughts(SettlementBed bed) {
      SettlerThoughtsList list = new SettlerThoughtsList();
      if (this.hasRoom) {
         if (this.hasFloorTiles) {
            list.addThought(this.getStringID() + "_solidfloor");
         } else {
            list.addThought(this.getStringID() + "_organicfloor");
         }

         if (this.plantCount == 0) {
            list.addThought(this.getStringID() + "_noplants");
         } else {
            list.addThought(this.getStringID() + "_some", this.plantCount);
         }
      }

      return list;
   }

   @Override
   public boolean preventsMissingFloorThought() {
      return true;
   }
}
