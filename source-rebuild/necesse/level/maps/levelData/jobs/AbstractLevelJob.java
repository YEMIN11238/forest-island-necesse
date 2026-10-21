package necesse.level.maps.levelData.jobs;

import necesse.engine.GameState;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.registries.IDData;
import necesse.engine.registries.JobTypeRegistry;
import necesse.engine.registries.LevelJobRegistry;
import necesse.engine.util.GameObjectReservable;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.WorldEntityGameClock;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.JobType;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.ZoneTester;

public abstract class AbstractLevelJob implements GameState, WorldEntityGameClock {
   public final IDData idData = new IDData();
   public final JobType jobType;
   public GameObjectReservable reservable = new GameObjectReservable();

   public final String getStringID() {
      return this.idData.getStringID();
   }

   public final int getID() {
      return this.idData.getID();
   }

   public AbstractLevelJob() {
      this.jobType = LevelJobRegistry.applyIDData((Class<? extends AbstractLevelJob>)this.getClass(), this.idData);
   }

   public abstract Level getLevel();

   public abstract int getTileX();

   public abstract int getTileY();

   public boolean isWithinRestrictZone(ZoneTester zone) {
      return zone.containsTile(this.getTileX(), this.getTileY());
   }

   @Override
   public WorldEntity getWorldEntity() {
      Level level = this.getLevel();
      return level == null ? null : level.getWorldEntity();
   }

   @Override
   public boolean isClient() {
      Level level = this.getLevel();
      return level != null && level.isClient();
   }

   @Override
   public Client getClient() {
      Level level = this.getLevel();
      return level == null ? null : level.getClient();
   }

   @Override
   public boolean isServer() {
      Level level = this.getLevel();
      return level != null && level.isServer();
   }

   @Override
   public Server getServer() {
      Level level = this.getLevel();
      return level == null ? null : level.getServer();
   }

   public boolean isValid() {
      return true;
   }

   public int getSameJobPriority() {
      return 0;
   }

   public boolean canDoWhileFollowing(Mob followingMob) {
      return false;
   }

   public String getRecreationExhaustionIdentifier() {
      return null;
   }

   public int getSameTypePriority(FoundJob<?> foundJob) {
      if (foundJob.priority != null && foundJob.priority.type.getID() == JobTypeRegistry.recreationID) {
         String identifier = this.getRecreationExhaustionIdentifier();
         if (identifier == null) {
            throw new IllegalStateException(
               "Job with stringID " + this.getStringID() + " is missing an override of getRecreationExhaustionIdentifier() that doesn't return null."
            );
         }

         float modifier = foundJob.worker.getRecreationRegenModifier(identifier);
         return (int)(100.0F * modifier);
      } else {
         return 0;
      }
   }

   public int getFirstPriority(FoundJob<?> foundJob) {
      return 0;
   }

   public boolean prioritizeForSameJobAgain() {
      return false;
   }

   public int getAfterPrioritizedPriority(FoundJob<?> foundJob) {
      return foundJob.priority != null && foundJob.priority.type.getID() == JobTypeRegistry.recreationID && foundJob.worker.wantsToDoRecreation() ? 10000 : 0;
   }

   public boolean estimateCanMoveTo(EntityJobWorker worker) {
      return worker.getMobWorker().estimateCanMoveTo(this.getTileX(), this.getTileY(), true);
   }

   public int groupDistanceToJobByTiles(EntityJobWorker worker) {
      return 0;
   }
}
