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

public class GardenerSettlerPersonality extends SettlerPersonality {
   public GardenerSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      LocalMessage completedFarmingMessage = new LocalMessage("personalities", "gardenertask");
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_task", new SimpleScalingHappinessSettlerThought(completedFarmingMessage, 4, -1, 1).setPersonalitySubmitter(this)
      );
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_none", new SimpleSettlerThought("personalities", "gardenernotasks", 0).setPersonalitySubmitter(this)
      );
   }

   @Override
   public void onPerformedJob(AbstractLevelJob job, JobTypeHandler.TypePriority priority) {
      super.onPerformedJob(job, priority);
      if (priority != null
         && priority.type.canChangePriority
         && (priority.type.getStringID().equals("farming") || priority.type.getStringID().equals("fertilize"))) {
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
