package necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.MobWasKilledEvent;
import necesse.gfx.forms.components.FormExpressionWheel;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;

public class KillMobRomanceActivity extends RomanceActivity {
   public String mobToKillStringID;
   public GameMessage mobToKillLocationMessage;

   public KillMobRomanceActivity(PlayerRomanceManager manager) {
      super(manager);
   }

   public KillMobRomanceActivity(PlayerRomanceManager manager, String mobToKillStringID, GameMessage mobToKillLocationMessage) {
      this(manager);
      this.mobToKillStringID = mobToKillStringID;
      this.mobToKillLocationMessage = mobToKillLocationMessage;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addMobStringID("mobToKillStringID", this.mobToKillStringID);
      save.addSaveData(this.mobToKillLocationMessage.getSaveData("mobToKillLocationMessage"));
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.mobToKillStringID = save.getMobStringID("mobToKillStringID", null, true);
      if (this.mobToKillStringID == null) {
         throw new LoadDataException("Romance Activity: " + this.getStringID() + " mobToKillStringID was null on load.");
      }

      this.mobToKillLocationMessage = GameMessage.loadSave(save, "mobToKillLocationMessage", true);
      if (this.mobToKillLocationMessage == null) {
         throw new LoadDataException("Romance Activity: " + this.getStringID() + " mobToKillLocationMessage was null on load.");
      }
   }

   @Override
   public void onMobKilled(ServerClient client, MobWasKilledEvent event) {
      if (event.target.getStringID().equals(this.mobToKillStringID) && !this.activityCompleted && this.hasInitializedActivity) {
         this.activityCompleted = true;
         this.getMob().startExpressionAbility.runAndSend(FormExpressionWheel.Expression.ANGRY, 6000);
         RomanceSettlerDialogue dialogue = this.getDialogue(client);
         dialogue.startYapping(client, dialogue.settlerChatsOnCompletingActivity, 4.0F, false, null);
      }
   }

   @Override
   public RomanceSettlerDialogue getDialogue(ServerClient client) {
      return this.getMob().getRomancePersonality().killMobRomanceActivityDialogue(client, this.mobToKillStringID, this.mobToKillLocationMessage);
   }
}
