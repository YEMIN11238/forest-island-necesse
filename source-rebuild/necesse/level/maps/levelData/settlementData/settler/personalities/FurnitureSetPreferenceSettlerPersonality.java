package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.GameColor;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.SettlementRoom;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleScalingHappinessSettlerThought;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;
import necesse.level.maps.presets.set.FurnitureSet;

public class FurnitureSetPreferenceSettlerPersonality extends SettlerPersonality {
   public FurnitureSet furnitureSet;
   public int furnitureSetObjectsCount = 0;

   public FurnitureSetPreferenceSettlerPersonality(HumanMob mob, FurnitureSet furnitureSet) {
      super(mob);
      this.furnitureSet = furnitureSet;
   }

   @Override
   public GameMessage getName() {
      return new LocalMessage("personalities", "furnitureenthusiast", "type", this.furnitureSet.typeMessage);
   }

   @Override
   public GameMessage getDescription() {
      return new LocalMessage("personalities", "furnitureenthusiast_desc")
         .addReplacement("name", GameColor.ITEM_QUEST.getColorCode() + this.mob.getSettlerName() + GameColor.NO_COLOR.getColorCode())
         .addReplacement(
            "type",
            new GameMessageBuilder()
               .append(GameColor.ITEM_QUEST.getColorCode())
               .append(this.furnitureSet.typeMessage)
               .append(GameColor.NO_COLOR.getColorCode())
         );
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      LocalMessage noFurnitureMessage = new LocalMessage("personalities", "nopreferedfurniture", "type", this.furnitureSet.typeMessage);
      SettlerThoughtRegistry.registerSettlerThought(this.getStringID() + "_none", new SimpleSettlerThought(noFurnitureMessage, 0).setPersonalitySubmitter(this));
      LocalMessage someFurnitureMessage = new LocalMessage("personalities", "somepreferedfurniture", "type", this.furnitureSet.typeMessage);
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_some", new SimpleScalingHappinessSettlerThought(someFurnitureMessage, 10, -2, 0).setPersonalitySubmitter(this)
      );
   }

   @Override
   public void onRoomStatsCalculated(SettlementBed bed, SettlementRoom settlementRoom) {
      super.onRoomStatsCalculated(bed, settlementRoom);
      this.furnitureSetObjectsCount = 0;
      if (settlementRoom != null && this.furnitureSet.objectArrays != null) {
         this.furnitureSetObjectsCount = (int)settlementRoom.streamObjectsInRoom(false, true).filter(o -> this.checkIfObjectIDMatchesSet(o.getID())).count();
      }
   }

   public boolean checkIfObjectIDMatchesSet(int objectID) {
      for (int[] setObjectIDsArray : this.furnitureSet.objectArrays) {
         for (int setObjectID : setObjectIDsArray) {
            if (setObjectID == objectID) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public SettlerThoughtsList getThoughts(SettlementBed bed) {
      SettlerThoughtsList list = new SettlerThoughtsList();
      if (this.furnitureSetObjectsCount == 0) {
         list.addThought(this.getStringID() + "_none");
      } else {
         list.addThought(this.getStringID() + "_some", Math.min(this.furnitureSetObjectsCount, 5));
      }

      return list;
   }
}
