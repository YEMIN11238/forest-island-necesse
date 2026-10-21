package necesse.level.maps.levelData.settlementData;

import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.inventory.container.settlement.events.SettlementSettlersChangedEvent;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.HappinessObject;
import necesse.level.gameObject.furniture.SettlerBedObject;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;

public class SettlementBed {
   public final ServerSettlementData data;
   public final int tileX;
   public final int tileY;
   protected LevelSettler settler;
   public boolean isLocked;

   public SettlementBed(ServerSettlementData data, int tileX, int tileY) {
      this.data = data;
      this.tileX = tileX;
      this.tileY = tileY;
   }

   public SettlementBed(ServerSettlementData data, LoadData save, int tileXOffset, int tileYOffset) {
      this.data = data;
      this.tileX = save.getInt("tileX") + tileXOffset;
      this.tileY = save.getInt("tileY") + tileYOffset;
      this.isLocked = save.getBoolean("isLocked", this.isLocked, false);
   }

   public void addSaveData(SaveData save) {
      save.addInt("tileX", this.tileX);
      save.addInt("tileY", this.tileY);
      save.addBoolean("isLocked", this.isLocked);
   }

   public boolean isValidBed() {
      return this.data.networkData.isTileWithinBounds(this.tileX, this.tileY) && isValidBed(this.data.getLevel(), this.tileX, this.tileY);
   }

   public static boolean isValidBed(Level level, int tileX, int tileY) {
      GameObject object = level.getObject(tileX, tileY);
      return object instanceof SettlerBedObject && ((SettlerBedObject)object).isMasterBedObject(level, tileX, tileY);
   }

   public SettlementRoom getRoom() {
      return this.data.rooms.getRoom(this.tileX, this.tileY);
   }

   public LevelSettler getSettler() {
      return this.settler;
   }

   public boolean clearSettler() {
      if (this.settler != null) {
         if (this.settler.getBed() == this) {
            this.settler.assignBed(null);
         }

         this.settler = null;
         this.data.sendEvent(SettlementSettlersChangedEvent.class);
         return true;
      } else {
         return false;
      }
   }

   public SettlerThoughtsList getSettlerThoughts() {
      SettlementRoom room = this.getRoom();
      if (room != null) {
         SettlerThoughtsList thoughts = room.getBaseThoughts();
         if (this.settler != null) {
            HappinessObject happinessObject = room.getHappinessObjectForSettler(this.settler);
            if (happinessObject != null) {
               thoughts = new SettlerThoughtsList(thoughts);
               thoughts.addThought("happinessobject_" + ((GameObject)happinessObject).getStringID());
            }
         }

         return thoughts;
      } else {
         return new SettlerThoughtsList(SettlerThoughtRegistry.bedOutsideThought);
      }
   }

   public int getHappinessScore() {
      SettlementRoom room = this.getRoom();
      return room == null ? SettlerThoughtRegistry.bedOutsideThought.getStackedHappinessModifier(null, 1) : room.getHappinessScore();
   }
}
