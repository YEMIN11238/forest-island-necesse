package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;

public class OrderlySettlerPersonality extends SettlerPersonality {
   public boolean satisfied = false;

   public OrderlySettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_yes", new SimpleSettlerThought("personalities", "orderlyhappy", 30).setPersonalitySubmitter(this)
      );
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_no", new SimpleSettlerThought("personalities", "orderlyunhappy", 0).setPersonalitySubmitter(this)
      );
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addBoolean("satisfied", this.satisfied);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.satisfied = save.getBoolean("satisfied", this.satisfied, false);
   }

   @Override
   public void gameTick() {
      super.gameTick();
   }

   @Override
   public void settlementTick(ServerSettlementData data) {
      super.settlementTick(data);
      if (data != null && data.hasTickedFirstJobs) {
         this.satisfied = !data.hasInvalidPlacedItemsInStorage;
      }
   }

   @Override
   public SettlerThoughtsList getThoughts(SettlementBed bed) {
      SettlerThoughtsList list = new SettlerThoughtsList();
      if (this.satisfied) {
         list.addThought(this.getStringID() + "_yes");
      } else {
         list.addThought(this.getStringID() + "_no");
      }

      return list;
   }
}
