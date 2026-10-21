package necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities;

import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.gfx.forms.components.FormExpressionWheel;
import necesse.inventory.recipe.Recipe;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;

public class CraftAndPlaceObjectRomanceActivity extends PlaceObjectRomanceActivity {
   public String workStationItemStringID;

   public CraftAndPlaceObjectRomanceActivity(PlayerRomanceManager manager) {
      super(manager);
   }

   public CraftAndPlaceObjectRomanceActivity(PlayerRomanceManager manager, String objectToPlaceStringID, String workStationItemStringID) {
      super(manager, objectToPlaceStringID);
      this.workStationItemStringID = workStationItemStringID;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addItemStringID("workStationItemStringID", this.workStationItemStringID);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.workStationItemStringID = save.getItemStringID("workStationItemStringID", null, true);
      if (this.workStationItemStringID == null) {
         throw new LoadDataException("Romance Activity: " + this.getStringID() + " workStationItemStringID was null on load.");
      }
   }

   @Override
   public void onCraftedRecipe(ServerClient client, Recipe recipe, int amount) {
      if (this.hasInitializedActivity && !this.activityCompleted) {
         if (recipe.resultStringID.equals(this.objectToPlaceStringID)) {
            this.getMob().startExpressionAbility.runAndSend(FormExpressionWheel.Expression.HAPPY, 4000);
            RomanceSettlerDialogue dialogue = this.getDialogue(client);
            dialogue.startYapping(client, dialogue.settlerChatsAtCustomMoment, 4.0F, false, null);
         }
      }
   }

   @Override
   public RomanceSettlerDialogue getDialogue(ServerClient client) {
      return this.getMob().getRomancePersonality().craftAndPlaceObjectRomanceActivityDialogue(client, this.objectToPlaceStringID, this.workStationItemStringID);
   }
}
