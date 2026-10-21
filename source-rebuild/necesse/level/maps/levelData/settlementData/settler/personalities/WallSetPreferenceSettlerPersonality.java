package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.GameColor;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.SettlementRoom;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;
import necesse.level.maps.presets.set.WallSet;

public class WallSetPreferenceSettlerPersonality extends SettlerPersonality {
   public final WallSet wallSet;
   public boolean hasSetWall;
   public boolean hasNonSetWall;

   public WallSetPreferenceSettlerPersonality(HumanMob mob, WallSet wallSet) {
      super(mob);
      this.wallSet = wallSet;
   }

   @Override
   public GameMessage getName() {
      return new LocalMessage("personalities", "wallarchitect", "type", new LocalMessage("itemcategory", this.wallSet.prefixStringID));
   }

   @Override
   public GameMessage getDescription() {
      return new LocalMessage("personalities", "wallarchitect_desc")
         .addReplacement("name", GameColor.ITEM_QUEST.getColorCode() + this.mob.getSettlerName() + GameColor.NO_COLOR.getColorCode())
         .addReplacement(
            "type",
            new GameMessageBuilder()
               .append(GameColor.ITEM_QUEST.getColorCode())
               .append(new LocalMessage("itemcategory", this.wallSet.prefixStringID))
               .append(GameColor.NO_COLOR.getColorCode())
         );
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      LocalMessage noWallMessage = new LocalMessage("personalities", "nopreferedwalls", "type", new LocalMessage("itemcategory", this.wallSet.prefixStringID));
      SettlerThoughtRegistry.registerSettlerThought(this.getStringID() + "_none", new SimpleSettlerThought(noWallMessage, 0).setPersonalitySubmitter(this));
      LocalMessage someWallMessage = new LocalMessage(
         "personalities", "somepreferedwalls", "type", new LocalMessage("itemcategory", this.wallSet.prefixStringID)
      );
      SettlerThoughtRegistry.registerSettlerThought(this.getStringID() + "_some", new SimpleSettlerThought(someWallMessage, 15).setPersonalitySubmitter(this));
      LocalMessage fullWallMessage = new LocalMessage(
         "personalities", "fullpreferedwalls", "type", new LocalMessage("itemcategory", this.wallSet.prefixStringID)
      );
      SettlerThoughtRegistry.registerSettlerThought(this.getStringID() + "_full", new SimpleSettlerThought(fullWallMessage, 30).setPersonalitySubmitter(this));
   }

   @Override
   public void onRoomStatsCalculated(SettlementBed bed, SettlementRoom settlementRoom) {
      super.onRoomStatsCalculated(bed, settlementRoom);
      this.hasSetWall = false;
      this.hasNonSetWall = false;
      if (settlementRoom != null && this.wallSet.objectArrays != null) {
         for (GameObject wallObject : settlementRoom.getWallsInRoom()) {
            if (this.checkIfObjectIDMatchesSet(wallObject.getID())) {
               this.hasSetWall = true;
               if (this.hasNonSetWall) {
                  break;
               }
            } else {
               this.hasNonSetWall = true;
               if (this.hasSetWall) {
                  break;
               }
            }
         }
      }
   }

   public boolean checkIfObjectIDMatchesSet(int objectID) {
      for (int[] setObjectIDsArray : this.wallSet.objectArrays) {
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
      if (!this.hasSetWall) {
         list.addThought(this.getStringID() + "_none");
      } else if (this.hasNonSetWall) {
         list.addThought(this.getStringID() + "_some");
      } else {
         list.addThought(this.getStringID() + "_full");
      }

      return list;
   }
}
