package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;

public class ExpeditionerSettlerPersonality extends SettlerPersonality {
   public static int MILLISECONDS_THOUGHT_DURATION = 7200000;
   public long lastExpeditionTime = 0L;

   public ExpeditionerSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_happy", new SimpleSettlerThought("personalities", "expeditioner_happy", 30).setPersonalitySubmitter(this)
      );
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_unhappy", new SimpleSettlerThought("personalities", "expeditioner_unhappy", 0).setPersonalitySubmitter(this)
      );
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addLong("lastExpeditionTime", this.lastExpeditionTime);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.lastExpeditionTime = save.getLong("lastExpeditionTime", this.lastExpeditionTime, false);
   }

   @Override
   public SettlerThoughtsList getThoughts(SettlementBed bed) {
      SettlerThoughtsList list = new SettlerThoughtsList();
      if (this.lastExpeditionTime + MILLISECONDS_THOUGHT_DURATION > this.mob.getTime() && this.lastExpeditionTime != 0L) {
         list.addThought(this.getStringID() + "_happy");
      } else {
         list.addThought(this.getStringID() + "_unhappy");
      }

      return list;
   }

   @Override
   public void onExpeditionStarted() {
      super.onExpeditionStarted();
      this.lastExpeditionTime = this.mob.getTime();
   }
}
