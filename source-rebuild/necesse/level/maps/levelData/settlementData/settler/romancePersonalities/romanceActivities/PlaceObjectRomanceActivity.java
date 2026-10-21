package necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities;

import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.AspiringHeroRomancePersonality;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;

public class PlaceObjectRomanceActivity extends RomanceActivity {
   public String objectToPlaceStringID;

   public PlaceObjectRomanceActivity(PlayerRomanceManager manager) {
      super(manager);
   }

   public PlaceObjectRomanceActivity(PlayerRomanceManager manager, String objectToPlaceStringID) {
      this(manager);
      this.objectToPlaceStringID = objectToPlaceStringID;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addObjectStringID("objectToPlaceStringID", this.objectToPlaceStringID);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.objectToPlaceStringID = save.getObjectStringID("objectToPlaceStringID", null, true);
      if (this.objectToPlaceStringID == null) {
         throw new LoadDataException("Romance Activity: " + this.getStringID() + " objectToPlaceStringID was null on load.");
      }
   }

   @Override
   public void onObjectPlaced(ServerClient client, GameObject object, int tileX, int tileY, int rotation) {
      if (this.hasInitializedActivity && !this.activityCompleted) {
         if (object.getStringID().equals(this.objectToPlaceStringID)) {
            String romancePersonalityStringID = this.getMob().getRomancePersonality().getStringID();
            if (this.getMob().isSettlerWithinSettlement()) {
               this.activityCompleted = true;
               RomanceSettlerDialogue dialogue = this.getDialogue(client);
               dialogue.startYapping(client, dialogue.settlerChatsOnCompletingActivity, 4.0F, false, () -> {
                  if (romancePersonalityStringID.equals("aspiringhero")) {
                     ((AspiringHeroRomancePersonality)this.getMob().getRomancePersonality()).onObjectPlacedCustomEvent();
                  }
               });
            } else {
               RomanceSettlerDialogue dialogue = this.getDialogue(client);
               if (!dialogue.settlerChatsOnDoingActivityWrong.isEmpty()) {
                  dialogue.startYapping(client, dialogue.settlerChatsOnDoingActivityWrong, 4.0F, false, null);
               }
            }
         }
      }
   }

   @Override
   public RomanceSettlerDialogue getDialogue(ServerClient client) {
      return this.getMob().getRomancePersonality().placeObjectRomanceActivityDialogue(client, this.objectToPlaceStringID);
   }
}
