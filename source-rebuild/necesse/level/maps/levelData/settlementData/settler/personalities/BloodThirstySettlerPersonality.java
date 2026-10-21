package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.TrainingDummyMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleScalingHappinessSettlerThought;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;

public class BloodThirstySettlerPersonality extends SettlerPersonality {
   public static int THOUGHT_GIVEN_COOLDOWN_SECONDS = 30;
   public static int THOUGHT_DURATION_SECONDS = 1800;
   public long lastThoughtGivenTime;

   public BloodThirstySettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      LocalMessage dealtDamageMessage = new LocalMessage("personalities", "bloodthirstysatisfied");
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_yes", new SimpleScalingHappinessSettlerThought(dealtDamageMessage, 5, -1, 1).setPersonalitySubmitter(this)
      );
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_no", new SimpleSettlerThought("personalities", "bloodthirstyneeded", 0).setPersonalitySubmitter(this)
      );
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addLong("lastThoughtGivenTime", this.lastThoughtGivenTime);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.lastThoughtGivenTime = save.getLong("lastThoughtGivenTime", 0L, false);
   }

   @Override
   public void onMobHasAttacked(MobWasHitEvent event) {
      super.onMobHasAttacked(event);
      if (!(event.target instanceof TrainingDummyMob)) {
         long timeSinceLastDamageDealt = this.mob.getTime() - this.lastThoughtGivenTime;
         if (timeSinceLastDamageDealt >= THOUGHT_GIVEN_COOLDOWN_SECONDS * 1000L) {
            this.mob.getActiveThoughts().addThoughtSeconds(this.getStringID() + "_yes", THOUGHT_DURATION_SECONDS);
            this.lastThoughtGivenTime = this.mob.getTime();
         }
      }
   }

   @Override
   public SettlerThoughtsList getThoughts(SettlementBed bed) {
      if (!this.mob.getActiveThoughts().hasThought(this.getStringID() + "_yes")) {
         SettlerThoughtsList list = new SettlerThoughtsList();
         list.addThought(this.getStringID() + "_no");
         return list;
      } else {
         return null;
      }
   }
}
