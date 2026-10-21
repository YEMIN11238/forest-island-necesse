package necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.critters.flyingbugs.FireflyMob;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;

public class CaptureFireflyWithNetRomanceActivity extends CaptureCritterWithNetRomanceActivity {
   public FireflyMob.FireflyType fireflyType;

   public CaptureFireflyWithNetRomanceActivity(PlayerRomanceManager manager) {
      super(manager);
   }

   public CaptureFireflyWithNetRomanceActivity(
      PlayerRomanceManager manager, String mobToCaptureStringID, GameMessage mobToCaptureLocationMessage, FireflyMob.FireflyType fireflyType
   ) {
      super(manager, mobToCaptureStringID, mobToCaptureLocationMessage);
      this.fireflyType = fireflyType;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addEnum("fireflyType", this.fireflyType);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.fireflyType = save.getEnum(FireflyMob.FireflyType.class, "fireflyType", null, true);
      if (this.fireflyType == null) {
         throw new LoadDataException("Romance Activity: " + this.getStringID() + " fireflyType was null on load.");
      }
   }

   @Override
   public void onCritterNetted(ServerClient client, Mob critterNetted) {
      if (!this.activityCompleted && this.hasInitializedActivity) {
         if (critterNetted.getStringID().equals(this.mobToCaptureStringID) && ((FireflyMob)critterNetted).fireflyType == this.fireflyType) {
            this.correctCritterCaptured(client);
         } else {
            this.wrongCritterCaught(client);
         }
      }
   }

   @Override
   public RomanceSettlerDialogue getDialogue(ServerClient client) {
      return this.getMob()
         .getRomancePersonality()
         .captureFireflyWithNetRomanceActivityDialogue(client, this.mobToCaptureStringID, this.mobToCaptureLocationMessage, this.fireflyType);
   }
}
