package necesse.entity.mobs.job;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.function.Consumer;
import java.util.stream.Stream;
import necesse.engine.GameTileRange;
import necesse.engine.registries.LevelJobRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.inventory.item.Item;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.jobs.AbstractLevelJob;
import necesse.level.maps.levelData.settlementData.NetworkSettlementData;
import necesse.level.maps.levelData.settlementData.ZoneTester;
import necesse.level.maps.levelData.settlementData.notifications.SettlementNotificationSeverity;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;

public interface EntityJobWorker {
   GameTileRange defaultJobRange = new GameTileRange(50);

   default Mob getMobWorker() {
      return (Mob)this;
   }

   default Level getLevel() {
      return this.getMobWorker().getLevel();
   }

   default Point getJobSearchTile() {
      Mob mob = this.getMobWorker();
      return new Point(mob.getTileX(), mob.getTileY());
   }

   default Rectangle getJobSearchBounds() {
      return defaultJobRange.getRangeBounds(this.getJobSearchTile());
   }

   default Stream<? extends AbstractLevelJob> streamValidJobsWithinRange(Rectangle searchBounds) {
      if (searchBounds == null) {
         searchBounds = this.getJobSearchBounds();
      }

      return this.getLevel().jobsLayer.streamValidJobsInRegionsShape(searchBounds, 0);
   }

   Stream<FoundJobGenerator<?>> streamExtraJobs();

   default JobSequence findJob(boolean ignoreRecreationJobs, Consumer<JobFinder> finderMod) {
      JobTypeHandler handler = this.getJobTypeHandler();
      long currentTime = this.getMobWorker().getTime();
      if (handler.isOnGlobalCooldown(currentTime)) {
         return null;
      }

      JobFinder jobFinder = new JobFinder(this);
      if (finderMod != null) {
         finderMod.accept(jobFinder);
      }

      FoundJob<?> first = jobFinder.findJob(ignoreRecreationJobs);
      if (handler.resetPrioritizeNextJobIfFound) {
         handler.prioritizeNextJobID = -1;
      }

      if (first != null) {
         first.startCooldown(currentTime);
         if (first.job.prioritizeForSameJobAgain()) {
            handler.lastPerformedJobID = first.job.getID();
         } else {
            handler.lastPerformedJobID = -1;
         }

         return first.getSequence();
      } else {
         handler.lastPerformedJobID = -1;
         handler.prioritizeNextJobID = -1;
         return null;
      }
   }

   default boolean allowsFindingJobType(JobType jobType) {
      return true;
   }

   default void onHitCausedFailed(boolean clearedSequence) {
      int cooldown = GameRandom.globalRandom.getIntBetween(5000, 10000);
      if (!clearedSequence) {
         cooldown /= 2;
      }

      this.getJobTypeHandler().startGlobalCooldown(this.getMobWorker().getTime(), cooldown);
   }

   default void onTargetFoundCausedFailed(Mob target) {
      int cooldown = GameRandom.globalRandom.getIntBetween(5000, 10000);
      this.getJobTypeHandler().startGlobalCooldown(this.getMobWorker().getTime(), cooldown);
   }

   default void onFollowOutOfRangeCausedFailed(Mob followingMob) {
      int cooldown = GameRandom.globalRandom.getIntBetween(5000, 10000);
      this.getJobTypeHandler().startGlobalCooldown(this.getMobWorker().getTime(), cooldown);
   }

   boolean estimateCanMoveTo(int var1, int var2, boolean var3);

   default ZoneTester getJobRestrictZone() {
      Point searchTile = this.getJobSearchTile();
      return (tileX, tileY) -> defaultJobRange.isWithinRange(searchTile, tileX, tileY);
   }

   JobTypeHandler getJobTypeHandler();

   Mob getJobFollowingMob();

   WorkInventory getWorkInventory();

   default void submitFullInventoryNotification() {
      if (this instanceof SettlerMob) {
         SettlerMob settlerMob = (SettlerMob)this;
         SettlementNotificationSeverity severity = SettlementNotificationSeverity.WARNING;
         NetworkSettlementData settlement = settlerMob.getSettlerSettlementNetworkData();
         if (settlement != null) {
            settlement.notifications.submitNotification("fullinventory", (SettlerMob)this, severity);
         }
      }
   }

   default void removeFullInventoryNotification() {
      if (this instanceof SettlerMob) {
         SettlerMob settlerMob = (SettlerMob)this;
         NetworkSettlementData settlement = settlerMob.getSettlerSettlementNetworkData();
         if (settlement != null) {
            settlement.notifications.removeNotification("fullinventory", (SettlerMob)this);
         }
      }
   }

   default boolean isFullInventoryNotificationStillValid() {
      return this.getWorkInventory().isFull();
   }

   void showPickupAnimation(int var1, int var2, Item var3, int var4, boolean var5);

   void showPlaceAnimation(int var1, int var2, Item var3, int var4, boolean var5);

   void showWorkAnimation(int var1, int var2, Item var3, int var4, boolean var5);

   void showAttackAnimation(int var1, int var2, Item var3, int var4, boolean var5);

   float getWorkSpeedModifier();

   default float getWorkAnimSpeedMultiplier() {
      return 1.0F / this.getWorkSpeedModifier();
   }

   void showHoldAnimation(Item var1, int var2);

   void clearHoldAnimation();

   boolean hasActiveJob();

   boolean isInWorkAnimation();

   boolean isJobCancelled();

   void resetJobCancelled();

   void onPerformedJob(AbstractLevelJob var1, JobTypeHandler.TypePriority var2);

   void useRecreation(float var1, float var2);

   void regenRecreation(float var1, float var2, String var3);

   float getRecreationRegenModifier(String var1);

   boolean wantsToDoRecreation();

   boolean isOnStrike();

   boolean attemptStartStrike(boolean var1);

   default void setPrioritizeNextJob(int jobID, boolean resetPrioritizeNextJobIfFound) {
      JobTypeHandler handler = this.getJobTypeHandler();
      handler.prioritizeNextJobID = jobID;
      handler.resetPrioritizeNextJobIfFound = resetPrioritizeNextJobIfFound;
   }

   default void setPrioritizeNextJob(Class<? extends AbstractLevelJob> job, boolean resetPrioritizeNextJobIfFound) {
      this.setPrioritizeNextJob(LevelJobRegistry.getJobID(job), resetPrioritizeNextJobIfFound);
   }
}
