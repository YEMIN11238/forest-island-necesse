package necesse.level.maps.levelData.jobs;

import java.util.Objects;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.HusbandryMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.GameLinkedListJobSequence;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.activeJob.HuntMobActiveJob;
import necesse.entity.mobs.job.activeJob.PerformLevelJobEventActiveJob;
import necesse.level.maps.levelData.settlementData.ZoneTester;
import necesse.level.maps.levelData.settlementData.zones.SettlementHusbandryZone;

public class SlaughterHusbandryMobLevelJob extends EntityLevelJob<Mob> {
   public SettlementHusbandryZone zone;

   public SlaughterHusbandryMobLevelJob(Mob target, SettlementHusbandryZone zone) {
      super(target);
      this.zone = zone;
   }

   @Override
   public boolean isValid() {
      if (!super.isValid()) {
         return false;
      } else if (this.zone == null) {
         return false;
      } else {
         return this.zone.isRemoved() ? false : this.zone.containsTile(this.target.getTileX(), this.target.getTileY());
      }
   }

   public static <T extends SlaughterHusbandryMobLevelJob> JobSequence getJobSequence(EntityJobWorker worker, FoundJob<T> foundJob) {
      GameMessage targetDescription = foundJob.job.target.getLocalization();
      GameMessage activityDescription = new LocalMessage("activities", "slaughtering", "target", targetDescription);
      GameLinkedListJobSequence sequence = new GameLinkedListJobSequence(activityDescription, false);
      sequence.add(foundJob.job.getActiveJob(worker, foundJob.priority, sequence, "woodsword", new GameDamage(DamageTypeRegistry.MELEE, 20.0F), 50, 500, 500));
      sequence.add(new PerformLevelJobEventActiveJob(worker, foundJob));
      return sequence;
   }

   public static JobTypeHandler.JobStreamSupplier<? extends SlaughterHusbandryMobLevelJob> getJobStreamer() {
      return (worker, handler) -> {
         Mob mob = worker.getMobWorker();
         ZoneTester restrictZone = worker.getJobRestrictZone();
         return mob.getLevel()
            .entityManager
            .mobs
            .streamInRegionsShape(worker.getJobSearchBounds(), 0)
            .filter(m -> !m.removed() && mob.isSamePlace(m) && m instanceof HusbandryMob)
            .filter(m -> restrictZone.containsTile(m.getTileX(), m.getTileY()))
            .map(m -> ((HusbandryMob)m).slaughterJob)
            .filter(Objects::nonNull)
            .filter(SlaughterHusbandryMobLevelJob::isValid);
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

   public static JobTypeHandler.SubHandler<SlaughterHusbandryMobLevelJob> handler(EntityJobWorker worker, JobTypeHandler handler) {
      if (worker instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)worker;
         return handler.<SlaughterHusbandryMobLevelJob>setJobHandler(SlaughterHusbandryMobLevelJob.class, foundJob -> getJobSequence(humanMob, foundJob))
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
