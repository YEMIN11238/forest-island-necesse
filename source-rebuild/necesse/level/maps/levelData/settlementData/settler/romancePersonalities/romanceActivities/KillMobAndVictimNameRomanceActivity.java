package necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;

public class KillMobAndVictimNameRomanceActivity extends KillMobRomanceActivity {
   protected String victimDefeatedName;

   public KillMobAndVictimNameRomanceActivity(PlayerRomanceManager manager) {
      super(manager);
   }

   public KillMobAndVictimNameRomanceActivity(
      PlayerRomanceManager manager, String mobToKillStringID, GameMessage mobToKillLocationMessage, String victimDefeatedName
   ) {
      super(manager, mobToKillStringID, mobToKillLocationMessage);
      this.victimDefeatedName = victimDefeatedName;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addSafeString("victimDefeatedName", this.victimDefeatedName);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.victimDefeatedName = save.getSafeString("victimDefeatedName", null, true);
      if (this.victimDefeatedName == null) {
         throw new LoadDataException("Romance Activity: " + this.getStringID() + " victimDefeatedName was null on load.");
      }
   }

   public String getVictimDefeatedName() {
      return this.victimDefeatedName;
   }

   @Override
   public RomanceSettlerDialogue getDialogue(ServerClient client) {
      return this.getMob()
         .getRomancePersonality()
         .killMobAndVictimNameRomanceActivityDialogue(client, this.mobToKillStringID, this.mobToKillLocationMessage, this.victimDefeatedName);
   }
}
