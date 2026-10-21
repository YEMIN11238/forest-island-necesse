package necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;

public class FindAndPlaceObjectRomanceActivity extends PlaceObjectRomanceActivity {
   GameMessage whereToFindObjectLocation;

   public FindAndPlaceObjectRomanceActivity(PlayerRomanceManager manager) {
      super(manager);
   }

   public FindAndPlaceObjectRomanceActivity(PlayerRomanceManager manager, String objectToPlaceStringID, GameMessage whereToFindObjectLocation) {
      super(manager, objectToPlaceStringID);
      this.whereToFindObjectLocation = whereToFindObjectLocation;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addSaveData(this.whereToFindObjectLocation.getSaveData("whereToFindObjectLocation"));
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.whereToFindObjectLocation = GameMessage.loadSave(save, "whereToFindObjectLocation", true);
      if (this.whereToFindObjectLocation == null) {
         throw new LoadDataException("Romance Activity: " + this.getStringID() + " whereToFindObjectLocation was null on load.");
      }
   }

   @Override
   public RomanceSettlerDialogue getDialogue(ServerClient client) {
      return this.getMob()
         .getRomancePersonality()
         .findAndPlaceObjectRomanceActivityDialogue(client, this.objectToPlaceStringID, this.whereToFindObjectLocation);
   }
}
