package necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities;

import java.util.Objects;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketMobChat;
import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.Mob;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.components.FormExpressionWheel;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;

public class CaptureAnimalRomanceActivity extends RomanceActivity {
   public String mobToCaptureID;
   public GameMessage mobToCaptureLocationMessage;
   public int mobCapturedUniqueID;

   public CaptureAnimalRomanceActivity(PlayerRomanceManager manager) {
      super(manager);
   }

   public CaptureAnimalRomanceActivity(PlayerRomanceManager manager, String mobToCaptureStringID, GameMessage mobToCaptureLocationMessage) {
      this(manager);
      this.mobToCaptureID = mobToCaptureStringID;
      this.mobToCaptureLocationMessage = mobToCaptureLocationMessage;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addMobStringID("mobToCaptureID", this.mobToCaptureID);
      save.addSaveData(this.mobToCaptureLocationMessage.getSaveData("mobToCaptureLocationMessage"));
      save.addInt("mobCapturedUniqueID", this.mobCapturedUniqueID);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.mobToCaptureID = save.getMobStringID("mobToCaptureID", null, true);
      if (this.mobToCaptureID == null) {
         throw new LoadDataException("Romance Activity: " + this.getStringID() + " mobToCaptureID was null on load.");
      }

      this.mobToCaptureLocationMessage = GameMessage.loadSave(save, "mobToCaptureLocationMessage", true);
      if (this.mobToCaptureLocationMessage == null) {
         throw new LoadDataException("Romance Activity: " + this.getStringID() + " mobToCaptureLocationMessage was null on load.");
      }

      this.mobCapturedUniqueID = save.getInt("mobCapturedUniqueID", -1);
   }

   @Override
   public void onAnimalCaptured(ServerClient client, Mob animalCaptured) {
      if (this.hasInitializedActivity && !this.activityCompleted) {
         this.mobCapturedUniqueID = animalCaptured.getUniqueID();
         String romancePersonalityStringID = this.getMob().getRomancePersonality().getStringID();
         String playerName = client.getName();
         if (animalCaptured.getStringID().equals(this.mobToCaptureID) && !this.activityCompleted) {
            this.getMob().startExpressionAbility.runAndSend(FormExpressionWheel.Expression.SURPRISED, 6000);
            RomanceSettlerDialogue dialogue = this.getDialogue(client);
            dialogue.startYapping(client, dialogue.settlerChatsAtCustomMoment, 4.0F, false, null);
         } else if (!animalCaptured.getStringID().equals(this.mobToCaptureID) && !this.activityCompleted) {
            GameMessage message = new LocalMessage(
               "romance",
               romancePersonalityStringID + "_capturedwronganimalchat",
               "mob",
               TypeParsers.getMobAndNameParseString(this.mobToCaptureID, "**"),
               "name",
               playerName,
               "location",
               new GameMessageBuilder().append("**").append(this.mobToCaptureLocationMessage).append("**")
            );
            this.getMob().getServer().network.sendToClientsWithEntity(new PacketMobChat(this.getMob().getUniqueID(), message), this.getMob());
         }
      }
   }

   @Override
   public void onAnimalReleased(ServerClient client, Mob animalReleased) {
      if (this.hasInitializedActivity && !this.activityCompleted) {
         if (animalReleased.getUniqueID() == this.mobCapturedUniqueID
            && Objects.equals(animalReleased.getStringID(), this.mobToCaptureID)
            && !this.activityCompleted) {
            String romancePersonalityStringID = this.getMob().getRomancePersonality().getStringID();
            if (this.getMob().isSettlerWithinSettlement()) {
               this.activityCompleted = true;
               this.getMob().startExpressionAbility.runAndSend(FormExpressionWheel.Expression.HAPPY, 6000);
               RomanceSettlerDialogue dialogue = this.getDialogue(client);
               dialogue.startYapping(client, dialogue.settlerChatsOnCompletingActivity, 4.0F, false, null);
            } else {
               GameMessage message = new LocalMessage(
                  "romance",
                  romancePersonalityStringID + "_releasedanimalelsewherechat",
                  "mob",
                  animalReleased.getLocalization(),
                  "mobid",
                  animalReleased.getStringID(),
                  "name",
                  client.getName()
               );
               this.getMob().getServer().network.sendToClientsWithEntity(new PacketMobChat(this.getMob().getUniqueID(), message), this.getMob());
            }
         }
      }
   }

   @Override
   public RomanceSettlerDialogue getDialogue(ServerClient client) {
      return this.getMob().getRomancePersonality().captureAnimalRomanceActivityDialogue(client, this.mobToCaptureID, this.mobToCaptureLocationMessage);
   }
}
