package necesse.level.maps.levelData.settlementData.settler.personalities;

import java.awt.Point;
import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.SettlementRoom;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;

public class BeachLoverSettlerPersonality extends SettlerPersonality {
   public boolean satisfied;

   public BeachLoverSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_yes", new SimpleSettlerThought("personalities", "roomisnearwater", 30).setPersonalitySubmitter(this)
      );
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_no", new SimpleSettlerThought("personalities", "roomnotnearwater", 0).setPersonalitySubmitter(this)
      );
   }

   @Override
   public void onRoomStatsCalculated(SettlementBed bed, SettlementRoom settlementRoom) {
      super.onRoomStatsCalculated(bed, settlementRoom);
      this.satisfied = false;
      if (settlementRoom != null) {
         for (Point tile : settlementRoom.getRoomTiles()) {
            int height = settlementRoom.getLevel().liquidManager.getHeight(tile.x, tile.y);
            if (height < Math.min(10, 10)) {
               this.satisfied = true;
               break;
            }
         }
      }
   }

   @Override
   public SettlerThoughtsList getThoughts(SettlementBed bed) {
      SettlerThoughtsList list = new SettlerThoughtsList();
      if (this.satisfied) {
         list.addThought(this.getStringID() + "_yes");
      } else {
         list.addThought(this.getStringID() + "_no");
      }

      return list;
   }
}
