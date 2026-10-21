package necesse.level.maps.levelData.jobs;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Predicate;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.HusbandryMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.GameLinkedListJobSequence;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.activeJob.ActiveJobResult;
import necesse.entity.mobs.job.activeJob.InteractMobActiveJob;
import necesse.entity.mobs.job.activeJob.PerformLevelJobEventActiveJob;
import necesse.entity.mobs.job.activeJob.PickupItemEntityActiveJob;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.inventory.InventoryItem;
import necesse.level.maps.levelData.settlementData.ZoneTester;
import necesse.level.maps.levelData.settlementData.zones.SettlementHusbandryZone;

public class MilkHusbandryMobLevelJob extends EntityLevelJob<HusbandryMob> {
   public SettlementHusbandryZone zone;

   public MilkHusbandryMobLevelJob(HusbandryMob target, SettlementHusbandryZone zone) {
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

   public static <T extends MilkHusbandryMobLevelJob> JobSequence getJobSequence(EntityJobWorker worker, final FoundJob<T> foundJob) {
      final InventoryItem milkingItem = new InventoryItem("bucket");
      if (foundJob.job.target.canMilk(milkingItem)) {
         GameMessage targetDescription = foundJob.job.target.getLocalization();
         GameMessage activityDescription = new LocalMessage("activities", "milking", "target", targetDescription);
         final GameLinkedListJobSequence sequence = new GameLinkedListJobSequence(activityDescription, false);
         sequence.add(
            new InteractMobActiveJob<HusbandryMob>(
               worker, foundJob.priority, foundJob.job.target, m -> foundJob.job.isValid() && m.canMilk(milkingItem), foundJob.job.reservable, milkingItem
            ) {
               public ActiveJobResult onInteracted(HusbandryMob target) {
                  ArrayList<InventoryItem> products = new ArrayList<>();
                  target.onMilk(milkingItem, products);
                  ArrayList<ItemPickupEntity> pickups = new ArrayList<>(products.size());

                  for (InventoryItem product : products) {
                     ItemPickupEntity pickupItem = product.getPickupEntity(target.getLevel(), target.x, target.y);
                     target.getLevel().entityManager.pickups.add(pickupItem);
                     pickups.add(pickupItem);
                  }

                  PickupItemEntityActiveJob.addItemPickupJobs(this.worker, foundJob.priority, pickups, sequence);
                  return ActiveJobResult.FINISHED;
               }
            }
         );
         sequence.add(new PerformLevelJobEventActiveJob(worker, foundJob));
         return sequence;
      } else {
         return null;
      }
   }

   public static JobTypeHandler.JobStreamSupplier<? extends MilkHusbandryMobLevelJob> getJobStreamer() {
      return (worker, handler) -> {
         Mob mob = worker.getMobWorker();
         ZoneTester restrictZone = worker.getJobRestrictZone();
         return mob.getLevel()
            .entityManager
            .mobs
            .streamInRegionsShape(worker.getJobSearchBounds(), 0)
            .filter(m -> !m.removed() && mob.isSamePlace(m) && m instanceof HusbandryMob)
            .filter(m -> restrictZone.containsTile(m.getTileX(), m.getTileY()))
            .map(m -> ((HusbandryMob)m).milkJob)
            .filter(Objects::nonNull)
            .filter(MilkHusbandryMobLevelJob::isValid);
      };
   }

   public static JobTypeHandler.SubHandler<MilkHusbandryMobLevelJob> handler(EntityJobWorker worker, JobTypeHandler handler) {
      if (worker instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)worker;
         return handler.<MilkHusbandryMobLevelJob>setJobHandler(MilkHusbandryMobLevelJob.class, foundJob -> getJobSequence(humanMob, foundJob))
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
