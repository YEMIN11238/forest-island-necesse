package necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities;

import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;

public class GiveItemRomanceActivity extends RomanceActivity {
   public String itemStringID;
   public int amount;

   public GiveItemRomanceActivity(PlayerRomanceManager manager) {
      super(manager);
   }

   public GiveItemRomanceActivity(PlayerRomanceManager manager, String itemStringID, int amount) {
      this(manager);
      this.itemStringID = itemStringID;
      this.amount = amount;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addItemStringID("itemStringID", this.itemStringID);
      save.addInt("amount", this.amount);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.itemStringID = save.getItemStringID("itemStringID", null, false);
      if (this.itemStringID == null) {
         throw new LoadDataException("Romance Activity: " + this.getStringID() + " itemStringID was null on load.");
      }

      this.amount = save.getInt("amount", 1, false);
   }

   @Override
   public RomanceSettlerDialogue getDialogue(ServerClient client) {
      return this.getMob().getRomancePersonality().giveItemRomanceActivityDialogue(client, this.itemStringID, this.amount);
   }
}
