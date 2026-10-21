package necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.Mob;
import necesse.gfx.forms.components.FormExpressionWheel;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;

public class CaptureCritterWithNetRomanceActivity extends RomanceActivity {
   public String mobToCaptureStringID;
   public GameMessage mobToCaptureLocationMessage;

   public CaptureCritterWithNetRomanceActivity(PlayerRomanceManager manager) {
      super(manager);
   }

   public CaptureCritterWithNetRomanceActivity(PlayerRomanceManager manager, String mobToCaptureStringID, GameMessage mobToCaptureLocationMessage) {
      this(manager);
      this.mobToCaptureStringID = mobToCaptureStringID;
      this.mobToCaptureLocationMessage = mobToCaptureLocationMessage;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addMobStringID("mobToCaptureStringID", this.mobToCaptureStringID);
      save.addSaveData(this.mobToCaptureLocationMessage.getSaveData("mobToCaptureLocationMessage"));
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.mobToCaptureStringID = save.getMobStringID("mobToCaptureStringID", null, true);
      if (this.mobToCaptureStringID == null) {
         throw new LoadDataException("Romance Activity: " + this.getStringID() + " mobToCaptureStringID was null on load.");
      }

      this.mobToCaptureLocationMessage = GameMessage.loadSave(save, "mobToCaptureLocationMessage", true);
      if (this.mobToCaptureLocationMessage == null) {
         throw new LoadDataException("Romance Activity: " + this.getStringID() + " mobToCaptureLocationMessage was null on load.");
      }
   }

   @Override
   public void onCritterNetted(ServerClient client, Mob critterNetted) {
      if (!this.activityCompleted && this.hasInitializedActivity) {
         if (critterNetted.getStringID().equals(this.mobToCaptureStringID)) {
            this.correctCritterCaptured(client);
         } else {
            this.wrongCritterCaught(client);
         }
      }
   }

   public void correctCritterCaptured(ServerClient client) {
      this.activityCompleted = true;
      this.getMob().startExpressionAbility.runAndSend(FormExpressionWheel.Expression.BLUSH, 6000);
      RomanceSettlerDialogue dialogue = this.getDialogue(client);
      dialogue.startYapping(client, dialogue.settlerChatsOnCompletingActivity, 4.0F, false, null);
   }

   public void wrongCritterCaught(ServerClient client) {
      RomanceSettlerDialogue dialogue = this.getDialogue(client);
      dialogue.startYapping(client, dialogue.settlerChatsAtCustomMoment, 4.0F, false, null);
   }

   @Override
   public RomanceSettlerDialogue getDialogue(ServerClient client) {
      return this.getMob()
         .getRomancePersonality()
         .captureCritterWithNetRomanceActivityDialogue(client, this.mobToCaptureStringID, this.mobToCaptureLocationMessage);
   }
}
