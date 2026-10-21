package necesse.level.maps.levelData.jobs;

import java.awt.Point;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Predicate;
import java.util.stream.Stream;
import necesse.engine.expeditions.SettlerExpedition;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.ItemRegistry;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.LinkedListJobSequence;
import necesse.entity.mobs.job.WorkInventory;
import necesse.entity.mobs.job.activeJob.PerformLevelJobEventActiveJob;
import necesse.entity.mobs.job.activeJob.PickupSettlementStorageActiveJob;
import necesse.entity.mobs.job.activeJob.StartExpeditionActiveJob;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementMissionBoardMission;
import necesse.level.maps.levelData.settlementData.SettlementStoragePickupSlot;
import necesse.level.maps.levelData.settlementData.ZoneTester;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageItemIDIndex;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageRecords;

public class StartExpeditionLevelJob extends EntityLevelJob<Mob> {
   public JobTypeHandler.SubHandler<?> handler;
   public Predicate<SettlerExpedition> canDoExpedition;
   public long shopSeed;

   public StartExpeditionLevelJob(Mob mob, JobTypeHandler.SubHandler<?> handler, Predicate<SettlerExpedition> canDoExpedition, long shopSeed) {
      super(mob);
      this.handler = handler;
      this.canDoExpedition = canDoExpedition;
      this.shopSeed = shopSeed;
   }

   @Override
   public boolean isWithinRestrictZone(ZoneTester zone) {
      return true;
   }

   @Override
   public boolean isValid() {
      return true;
   }

   @Override
   public int getAfterPrioritizedPriority(FoundJob<?> foundJob) {
      return 1000;
   }

   public static <T extends StartExpeditionLevelJob> JobSequence getJobSequence(EntityJobWorker worker, FoundJob<T> foundJob) {
      Mob mob = worker.getMobWorker();
      Level level = mob.getLevel();
      if (!(mob instanceof SettlerMob)) {
         return null;
      }

      SettlerMob settlerMob = (SettlerMob)mob;
      if (!settlerMob.isSettlerWithinSettlement()) {
         return null;
      }

      ServerSettlementData settlement = settlerMob.getSettlerSettlementServerData();
      if (settlement == null) {
         return null;
      }

      Point missionBoardTile = settlement.getMissionBoardTile();
      if (missionBoardTile == null) {
         return null;
      }

      Iterator var7 = settlement.missionBoardManager.getMissions().iterator();

      SettlementMissionBoardMission mission;
      LinkedListJobSequence sequence;
      int currentCost;
      label84:
      while (true) {
         if (!var7.hasNext()) {
            return null;
         }

         mission = (SettlementMissionBoardMission)var7.next();
         if (foundJob.job.canDoExpedition.test(mission.expedition)
            && (mission.allSettlersAssigned || mission.assignedSettlers.contains(mob.getUniqueID()))
            && mission.condition.isConditionMet(worker, settlement)) {
            GameMessage typedName = mission.expedition.getFullDisplayName();
            GameMessage activityDescription = new LocalMessage("activities", "goingmission", "mission", typedName);
            sequence = new LinkedListJobSequence(activityDescription, false);
            currentCost = mission.expedition.getCurrentCost(settlement, foundJob.job.shopSeed);
            if (currentCost == 0) {
               break;
            }

            int foundCost = 0;
            int coinID = ItemRegistry.getItemID("coin");
            WorkInventory workInventory = worker.getWorkInventory();

            for (InventoryItem item : workInventory.items()) {
               if (item.item.getID() == coinID) {
                  foundCost += item.getAmount();
                  if (foundCost >= currentCost) {
                     break;
                  }
               }
            }

            if (foundCost >= currentCost) {
               break;
            }

            SettlementStorageRecords storageRecords = PickupSettlementStorageActiveJob.getStorageRecords(worker);
            if (storageRecords != null) {
               int missingCost = currentCost - foundCost;
               LinkedList<SettlementStoragePickupSlot> foundItems = storageRecords.getIndex(SettlementStorageItemIDIndex.class)
                  .findPickupSlots(coinID, worker, null, missingCost, missingCost);
               if (foundItems != null) {
                  Iterator var19 = foundItems.iterator();

                  while (true) {
                     if (!var19.hasNext()) {
                        break label84;
                     }

                     SettlementStoragePickupSlot foundItem = (SettlementStoragePickupSlot)var19.next();
                     sequence.add(foundItem.toPickupJob(worker, foundJob.priority));
                  }
               }
            }
         }
      }

      sequence.add(new StartExpeditionActiveJob(worker, foundJob.priority, mission, currentCost));
      sequence.add(new PerformLevelJobEventActiveJob(worker, foundJob));
      worker.setPrioritizeNextJob(HasStorageLevelJob.class, false);
      return sequence;
   }

   public static JobTypeHandler.JobStreamSupplier<? extends StartExpeditionLevelJob> getJobStreamer(HumanMob humanMob) {
      return (worker, handler) -> {
         Mob mobWorker = worker.getMobWorker();
         return Stream.of(new StartExpeditionLevelJob(mobWorker, handler, humanMob::canDoExpedition, humanMob.getShopSeed()));
      };
   }

   public static JobTypeHandler.SubHandler<StartExpeditionLevelJob> handler(EntityJobWorker worker, JobTypeHandler handler) {
      if (worker instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)worker;
         return handler.<StartExpeditionLevelJob>setJobHandler(StartExpeditionLevelJob.class, foundJob -> getJobSequence(humanMob, foundJob))
            .setPredicate(
               () -> !humanMob.wantsToDoRecreation()
                  && !humanMob.isOnStrike()
                  && !humanMob.hasCompletedMission()
                  && humanMob.isSettlerWithinSettlement()
                  && humanMob.nextMissionWorldTime <= humanMob.getWorldTime()
                  && humanMob.hungerLevel > 0.1F
                  && !humanMob.isInventoryFull(true)
            )
            .setExtraJobStreamer(false, getJobStreamer(humanMob));
      } else {
         return null;
      }
   }
}
