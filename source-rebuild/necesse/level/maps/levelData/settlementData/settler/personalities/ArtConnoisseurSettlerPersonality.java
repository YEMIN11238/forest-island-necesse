package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.SettlementRoom;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleScalingHappinessSettlerThought;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;

public class ArtConnoisseurSettlerPersonality extends SettlerPersonality {
   public int uniquePaintingsCount;

   public ArtConnoisseurSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_none", new SimpleSettlerThought("personalities", "noartinroom", 0).setPersonalitySubmitter(this)
      );
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_some", new SimpleScalingHappinessSettlerThought("personalities", "someartinroom", 8, -2, 2).setPersonalitySubmitter(this)
      );
   }

   @Override
   public void onRoomStatsCalculated(SettlementBed bed, SettlementRoom settlementRoom) {
      super.onRoomStatsCalculated(bed, settlementRoom);
      if (settlementRoom == null) {
         this.uniquePaintingsCount = 0;
      } else {
         this.uniquePaintingsCount = (int)settlementRoom.streamObjectsInRoom(false, true)
            .filter(obj -> obj.roomProperties.contains("painting"))
            .mapToInt(GameObject::getID)
            .distinct()
            .count();
      }
   }

   @Override
   public SettlerThoughtsList getThoughts(SettlementBed bed) {
      SettlerThoughtsList list = new SettlerThoughtsList();
      if (this.uniquePaintingsCount == 0) {
         list.addThought(this.getStringID() + "_none");
      } else {
         list.addThought(this.getStringID() + "_some", this.uniquePaintingsCount);
      }

      return list;
   }
}
