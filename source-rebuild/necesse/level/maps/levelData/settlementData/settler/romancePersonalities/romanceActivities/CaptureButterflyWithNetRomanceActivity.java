package necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.critters.flyingbugs.ButterflyMob;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;

public class CaptureButterflyWithNetRomanceActivity extends CaptureCritterWithNetRomanceActivity {
   public ButterflyMob.SmallButterflyType butterflyType;

   public CaptureButterflyWithNetRomanceActivity(PlayerRomanceManager manager) {
      super(manager);
   }

   public CaptureButterflyWithNetRomanceActivity(
      PlayerRomanceManager manager, String mobToCaptureStringID, GameMessage mobToCaptureLocationMessage, ButterflyMob.SmallButterflyType butterflyType
   ) {
      super(manager, mobToCaptureStringID, mobToCaptureLocationMessage);
      this.butterflyType = butterflyType;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addEnum("butterflyType", this.butterflyType);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.butterflyType = save.getEnum(ButterflyMob.SmallButterflyType.class, "butterflyType", null, true);
      if (this.butterflyType == null) {
         throw new LoadDataException("Romance Activity: " + this.getStringID() + " butterflyType was null on load.");
      }
   }

   @Override
   public void onCritterNetted(ServerClient client, Mob critterNetted) {
      if (!this.activityCompleted && this.hasInitializedActivity) {
         if (critterNetted.getStringID().equals(this.mobToCaptureStringID) && ((ButterflyMob)critterNetted).butterflyType == this.butterflyType) {
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
         .captureButterflyWithNetRomanceActivityDialogue(client, this.mobToCaptureStringID, this.mobToCaptureLocationMessage, this.butterflyType);
   }
}
