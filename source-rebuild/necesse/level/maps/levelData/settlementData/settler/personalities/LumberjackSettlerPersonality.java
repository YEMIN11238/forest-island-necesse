package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.level.maps.levelData.jobs.AbstractLevelJob;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleScalingHappinessSettlerThought;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;

public class LumberjackSettlerPersonality extends SettlerPersonality {
   public LumberjackSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      LocalMessage completedLumberjackMessage = new LocalMessage("personalities", "lumberjacktask");
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_task", new SimpleScalingHappinessSettlerThought(completedLumberjackMessage, 5, -1, 1).setPersonalitySubmitter(this)
      );
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_none", new SimpleSettlerThought("personalities", "lumberjacknotasks", 0).setPersonalitySubmitter(this)
      );
   }

   @Override
   public void onPerformedJob(AbstractLevelJob job, JobTypeHandler.TypePriority priority) {
      super.onPerformedJob(job, priority);
      if (priority != null && priority.type.canChangePriority && priority.type.getStringID().equals("forestry")) {
         float seconds = 1800.0F;
         this.mob.getActiveThoughts().addThoughtSeconds(this.getStringID() + "_task", seconds);
      }
   }

   @Override
   public SettlerThoughtsList getThoughts(SettlementBed bed) {
      if (!this.mob.getActiveThoughts().hasThought(this.getStringID() + "_task")) {
         SettlerThoughtsList list = new SettlerThoughtsList();
         list.addThought(this.getStringID() + "_none");
         return list;
      } else {
         return null;
      }
   }
}
