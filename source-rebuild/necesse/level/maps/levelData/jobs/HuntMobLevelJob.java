package necesse.level.maps.levelData.jobs;

import java.util.Objects;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.critters.CritterMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.GameLinkedListJobSequence;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.activeJob.HuntMobActiveJob;
import necesse.entity.mobs.job.activeJob.PerformLevelJobEventActiveJob;
import necesse.level.maps.levelData.settlementData.ZoneTester;

public class HuntMobLevelJob extends EntityLevelJob<Mob> {
   public HuntMobLevelJob(Mob target) {
      super(target);
   }

   public static <T extends HuntMobLevelJob> JobSequence getJobSequence(EntityJobWorker worker, FoundJob<T> foundJob) {
      GameMessage targetDescription = foundJob.job.target.getLocalization();
      GameMessage activityDescription = new LocalMessage("activities", "hunting", "target", targetDescription);
      GameLinkedListJobSequence sequence = new GameLinkedListJobSequence(activityDescription, false);
      sequence.add(
         foundJob.job
            .getActiveJob(worker, foundJob.priority, sequence, "woodsword", new GameDamage(DamageTypeRegistry.MELEE, 20.0F), 64, 500, 500)
            .addRangedAttack("woodbow", new GameDamage(DamageTypeRegistry.RANGED, 20.0F), 256, 500, 1000, "stonearrow", 100)
      );
      sequence.add(new PerformLevelJobEventActiveJob(worker, foundJob));
      return sequence;
   }

   public static JobTypeHandler.JobStreamSupplier<? extends HuntMobLevelJob> getJobStreamer() {
      return (worker, handler) -> {
         Mob mob = worker.getMobWorker();
         ZoneTester restrictZone = worker.getJobRestrictZone();
         return mob.getLevel()
            .entityManager
            .mobs
            .streamInRegionsShape(worker.getJobSearchBounds(), 0)
            .filter(m -> !m.removed() && mob.isSamePlace(m) && m.isCritter)
            .filter(m -> restrictZone.containsTile(m.getTileX(), m.getTileY()))
            .map(m -> ((CritterMob)m).huntJob)
            .filter(Objects::nonNull)
            .filter(EntityLevelJob::isValid);
      };
   }

   public HuntMobActiveJob getActiveJob(
      EntityJobWorker worker,
      JobTypeHandler.TypePriority priority,
      GameLinkedListJobSequence sequence,
      String attackItemStringID,
      GameDamage damage,
      int attackRange,
      int attackAnimTime,
      int attackCooldown
   ) {
      return new HuntMobActiveJob(worker, priority, this, sequence, attackItemStringID, damage, attackRange, attackAnimTime, attackCooldown);
   }

   public static JobTypeHandler.SubHandler<HuntMobLevelJob> handler(EntityJobWorker worker, JobTypeHandler handler) {
      if (worker instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)worker;
         return handler.<HuntMobLevelJob>setJobHandler(HuntMobLevelJob.class, foundJob -> getJobSequence(humanMob, foundJob))
            .setPredicate(
               () -> !humanMob.isOnStrike()
                  && !humanMob.hasCompletedMission()
                  && (!humanMob.isSettler() || humanMob.isSettlerWithinSettlement())
                  && !humanMob.isInventoryFull(true)
            )
            .setExtraJobStreamer(false, getJobStreamer());
      } else {
         return null;
      }
   }
}
