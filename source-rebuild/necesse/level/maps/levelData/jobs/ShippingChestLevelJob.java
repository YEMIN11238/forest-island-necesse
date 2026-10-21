package necesse.level.maps.levelData.jobs;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.save.LoadData;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.humanShop.explorerMission.TradingMission;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.GameLinkedListJobSequence;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.activeJob.PerformLevelJobEventActiveJob;
import necesse.entity.mobs.job.activeJob.StartTradingMissionActiveJob;
import necesse.entity.objectEntity.ShippingChestObjectEntity;
import necesse.inventory.InventoryItem;
import necesse.level.gameObject.container.ShippingChestObject;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import necesse.level.maps.levelData.settlementData.LevelStorage;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementStoragePickupFuture;
import necesse.level.maps.levelData.settlementData.SettlementStoragePickupSlot;

public class ShippingChestLevelJob extends MineObjectLevelJob {
   public ShippingChestLevelJob(int tileX, int tileY) {
      super(tileX, tileY);
   }

   public ShippingChestLevelJob(LoadData save) {
      super(save);
   }

   @Override
   public boolean isValidObject(LevelObject object) {
      return object.object instanceof ShippingChestObject;
   }

   @Override
   public boolean shouldSave() {
      return false;
   }

   @Override
   public int getAfterPrioritizedPriority(FoundJob<?> foundJob) {
      return 1000;
   }

   public static <T extends ShippingChestLevelJob> JobSequence getJobSequence(EntityJobWorker worker, FoundJob<T> foundJob) {
      Level level = worker.getLevel();
      ServerSettlementData settlement = SettlementsWorldData.getSettlementsData(level)
         .getServerDataAtTile(level.getIdentifier(), foundJob.job.tileX, foundJob.job.tileY);
      if (settlement != null) {
         ShippingChestObjectEntity objectEntity = foundJob.job.getObject().getCurrentObjectEntity(ShippingChestObjectEntity.class);
         if (objectEntity != null) {
            LevelStorage storage = settlement.storageManager.getStorage(foundJob.job.tileX, foundJob.job.tileY);
            if (storage == null) {
               storage = objectEntity.nonSettlementStorage;
            }

            ArrayList<SettlementStoragePickupSlot> pickups = new ArrayList<>();
            int addedStacks = 0;

            for (SettlementStoragePickupFuture slot : storage.findFutureUnreservedSlots().collect(Collectors.toList())) {
               pickups.add(slot.accept(slot.item.getAmount()));
               addedStacks++;
            }

            if (addedStacks < objectEntity.startMissionWhenCarryingAtLeastStacks) {
               return null;
            }

            GameMessage activityDescription = new LocalMessage("activities", "goingmission", "mission", TradingMission.getMissionName());
            GameLinkedListJobSequence sequence = new GameLinkedListJobSequence(activityDescription, false);
            ArrayList<AtomicReference<InventoryItem>> pickedUpItems = new ArrayList<>();

            for (SettlementStoragePickupSlot pickup : pickups) {
               AtomicReference<InventoryItem> itemRef = new AtomicReference<>();
               sequence.add(pickup.toPickupJob(worker, foundJob.priority, itemRef));
               pickedUpItems.add(itemRef);
            }

            sequence.add(new StartTradingMissionActiveJob(worker, foundJob.priority, pickedUpItems));
            sequence.add(new PerformLevelJobEventActiveJob(worker, foundJob));
            worker.setPrioritizeNextJob(HasStorageLevelJob.class, false);
            return sequence;
         }
      }

      return null;
   }

   public static JobTypeHandler.SubHandler<ShippingChestLevelJob> handler(EntityJobWorker worker, JobTypeHandler handler) {
      if (worker instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)worker;
         return handler.<ShippingChestLevelJob>setJobHandler(ShippingChestLevelJob.class, foundJob -> getJobSequence(humanMob, foundJob))
            .setPredicate(
               () -> !humanMob.wantsToDoRecreation()
                  && !humanMob.isOnStrike()
                  && !humanMob.hasCompletedMission()
                  && humanMob.isSettlerWithinSettlement()
                  && humanMob.nextMissionWorldTime <= humanMob.getWorldTime()
                  && humanMob.hungerLevel > 0.1F
                  && !humanMob.isInventoryFull(true)
            );
      } else {
         return null;
      }
   }
}
