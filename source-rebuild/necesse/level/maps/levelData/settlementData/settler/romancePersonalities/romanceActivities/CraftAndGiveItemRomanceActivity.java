package necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities;

import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;

public class CraftAndGiveItemRomanceActivity extends GiveItemRomanceActivity {
   public String whereToCraftWorkStationStringID;

   public CraftAndGiveItemRomanceActivity(PlayerRomanceManager manager) {
      super(manager);
   }

   public CraftAndGiveItemRomanceActivity(PlayerRomanceManager manager, String itemStringID, int amount, String whereToCraftWorkStationStringID) {
      super(manager, itemStringID, amount);
      this.whereToCraftWorkStationStringID = whereToCraftWorkStationStringID;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addSafeString("craftItemLocationStringID", this.whereToCraftWorkStationStringID);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.whereToCraftWorkStationStringID = save.getSafeString("craftItemLocationStringID", null, true);
      if (this.whereToCraftWorkStationStringID == null) {
         throw new LoadDataException("Romance Activity: " + this.getStringID() + " whereToCraftWorkStationStringID was null on load.");
      }
   }

   @Override
   public RomanceSettlerDialogue getDialogue(ServerClient client) {
      return this.getMob()
         .getRomancePersonality()
         .craftAndGiveItemRomanceActivityDialogue(client, this.itemStringID, this.amount, this.whereToCraftWorkStationStringID);
   }
}
