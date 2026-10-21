package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;

public class PacifistSettlerPersonality extends SettlerPersonality {
   public static int MILLISECONDS_THOUGHT_DURATION = 7200000;
   public long lastCombatTime = 0L;

   public PacifistSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_happy", new SimpleSettlerThought("personalities", "pacifisthappy", 30).setPersonalitySubmitter(this)
      );
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_unhappy", new SimpleSettlerThought("personalities", "pacifistunhappy", -30).setPersonalitySubmitter(this)
      );
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addLong("lastCombatTime", this.lastCombatTime);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.lastCombatTime = save.getLong("lastCombatTime", this.lastCombatTime, false);
   }

   @Override
   public SettlerThoughtsList getThoughts(SettlementBed bed) {
      SettlerThoughtsList list = new SettlerThoughtsList();
      if (this.lastCombatTime + MILLISECONDS_THOUGHT_DURATION > this.mob.getTime() && this.lastCombatTime != 0L) {
         list.addThought(this.getStringID() + "_unhappy");
      } else {
         list.addThought(this.getStringID() + "_happy");
      }

      return list;
   }

   @Override
   public void onMobWasHit(MobWasHitEvent event) {
      super.onMobWasHit(event);
      this.lastCombatTime = this.mob.getTime();
   }

   @Override
   public void onMobHasAttacked(MobWasHitEvent event) {
      super.onMobHasAttacked(event);
      this.lastCombatTime = this.mob.getTime();
   }
}
