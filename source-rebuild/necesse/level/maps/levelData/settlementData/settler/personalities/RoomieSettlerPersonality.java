package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.SettlementRoom;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;

public class RoomieSettlerPersonality extends SettlerPersonality {
   public int roomies;

   public RoomieSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_no", new SimpleSettlerThought("personalities", "roomnoroomies", 0).setPersonalitySubmitter(this)
      );
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_yes", new SimpleSettlerThought("personalities", "roomhasroomie", 30).setPersonalitySubmitter(this)
      );
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_toomany", new SimpleSettlerThought("personalities", "roomcrowdedroomie", 0).setPersonalitySubmitter(this)
      );
   }

   @Override
   public void onRoomStatsCalculated(SettlementBed bed, SettlementRoom settlementRoom) {
      super.onRoomStatsCalculated(bed, settlementRoom);
      if (settlementRoom == null) {
         this.roomies = 0;
      } else {
         this.roomies = settlementRoom.getOccupiedBeds();
      }
   }

   @Override
   public SettlerThoughtsList getThoughts(SettlementBed bed) {
      SettlerThoughtsList list = new SettlerThoughtsList();
      if (this.roomies <= 1) {
         list.addThought(this.getStringID() + "_no");
      } else if (this.roomies == 2) {
         list.addThought(this.getStringID() + "_yes");
      } else {
         list.addThought(this.getStringID() + "_toomany", this.roomies);
      }

      return list;
   }

   @Override
   public boolean preventsSharingRoomThought() {
      return true;
   }
}
