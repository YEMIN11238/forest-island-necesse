package necesse.level.maps.levelData.jobs;

import java.awt.geom.Line2D.Float;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.save.LoadData;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.HumanMobChatterHandler;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.JobWorkerChatterHandler;
import necesse.entity.mobs.job.SingleJobSequence;
import necesse.entity.mobs.job.activeJob.ActiveJob;
import necesse.entity.mobs.job.activeJob.ActiveJobHitResult;
import necesse.entity.mobs.job.activeJob.ActiveJobResult;
import necesse.entity.mobs.job.activeJob.TileActiveJob;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.levelData.settlementData.settler.SettlerRecreationExhaustionManager;
import necesse.level.maps.liquidManager.ClosestHeightResult;

public class WaterGazingTileLevelJob extends TileLevelJob {
   public WaterGazingTileLevelJob(int tileX, int tileY) {
      super(tileX, tileY);
   }

   public WaterGazingTileLevelJob(LoadData save) {
      super(save);
   }

   @Override
   public String getRecreationExhaustionIdentifier() {
      return SettlerRecreationExhaustionManager.BEAUTY_IDENTIFIER;
   }

   @Override
   public boolean shouldSave() {
      return false;
   }

   @Override
   public boolean isValid() {
      if (!super.isValid()) {
         return false;
      }

      int height = this.getLevel().liquidManager.getHeight(this.tileX, this.tileY);
      return height == -5;
   }

   @Override
   public int groupDistanceToJobByTiles(EntityJobWorker worker) {
      return 10;
   }

   public static <T extends WaterGazingTileLevelJob> JobSequence getJobSequence(HumanMob humanMob, FoundJob<T> foundJob) {
      GameMessage activityDescription = new LocalMessage("activities", "gazing");
      return new SingleJobSequence(foundJob.job.getActiveJob(humanMob, foundJob.priority), activityDescription)
         .withPerformedLevelJob(foundJob.job)
         .markRecreationJob();
   }

   public ActiveJob getActiveJob(HumanMob humanMob, JobTypeHandler.TypePriority priority) {
      return new WaterGazingTileLevelJob.WaterGazingActiveJob(humanMob, priority, this.tileX, this.tileY);
   }

   public static JobTypeHandler.SubHandler<WaterGazingTileLevelJob> handler(EntityJobWorker worker, JobTypeHandler handler) {
      if (worker instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)worker;
         return handler.<WaterGazingTileLevelJob>setJobHandler(WaterGazingTileLevelJob.class, foundJob -> getJobSequence(humanMob, foundJob))
            .setPredicate(() -> !humanMob.isOnStrike() && (!humanMob.isSettler() || humanMob.isSettlerWithinSettlement()));
      } else {
         return null;
      }
   }

   private class WaterGazingActiveJob extends TileActiveJob {
      protected HumanMob humanMob;
      protected int maxTicksRemaining = 20 * GameRandom.globalRandom.getIntBetween(20, 30);
      protected boolean hasSetDirection = false;
      protected HumanMobChatterHandler chatterHandler;

      public WaterGazingActiveJob(HumanMob humanMob, JobTypeHandler.TypePriority priority, int tileX, int tileY) {
         super(humanMob, priority, tileX, tileY);
         this.humanMob = humanMob;
         this.chatterHandler = new HumanMobChatterHandler(humanMob, false);
      }

      @Override
      public JobMoveToTile getMoveToTile(JobMoveToTile lastTile) {
         CollisionFilter collisionFilter = this.humanMob.modifyChasingCollisionFilter(new CollisionFilter().projectileCollision(), null);
         if (lastTile != null
            && !this.getLevel().isSolidTile(lastTile.tileX, lastTile.tileY)
            && !this.getLevel()
               .collides(
                  new Float(this.tileX * 32 + 16, this.tileY * 32 + 16, lastTile.tileX * 32 + 16, lastTile.tileY * 32 + 16), 16.0F, 4.0F, collisionFilter
               )) {
            return lastTile;
         }

         ClosestHeightResult result = this.getLevel()
            .liquidManager
            .findClosestHeightTile(
               this.tileX,
               this.tileY,
               1,
               checkTile -> this.getLevel().isSolidTile(checkTile.x, checkTile.y)
                  ? false
                  : !this.getLevel()
                     .collides(
                        new Float(this.tileX * 32 + 16, this.tileY * 32 + 16, checkTile.x * 32 + 16, checkTile.y * 32 + 16), 16.0F, 4.0F, collisionFilter
                     )
            );
         return new JobMoveToTile(result.best.x, result.best.y, false);
      }

      @Override
      public void tick(boolean isCurrent, boolean isMovingTo) {
      }

      @Override
      public int getCompleteRange() {
         return 8;
      }

      @Override
      public boolean isValid(boolean isCurrent) {
         return WaterGazingTileLevelJob.this.isRemoved() ? false : WaterGazingTileLevelJob.this.isValid();
      }

      @Override
      public ActiveJobHitResult onHit(MobWasHitEvent event, boolean isMovingTo) {
         return super.onHit(event, isMovingTo);
      }

      @Override
      public ActiveJobResult perform() {
         this.maxTicksRemaining--;
         if (this.maxTicksRemaining % 20 * 5 == 0) {
            this.hasSetDirection = false;
         }

         if (!this.hasSetDirection) {
            java.awt.geom.Point2D.Float dir = GameMath.normalize(this.tileX - this.humanMob.getTileX(), this.tileY - this.humanMob.getTileY());
            this.humanMob.setFacingDir(dir.x, dir.y);
            this.hasSetDirection = true;
            this.humanMob.sendMovementPacket(false);
         }

         this.chatterHandler.serverTick();
         if (!this.chatterHandler.isInConversation()) {
            JobWorkerChatterHandler foundChatter = this.chatterHandler.lookForOtherToChatWith(4, false, true, false);
            if (foundChatter == null && this.maxTicksRemaining <= 0) {
               this.chatterHandler.invalidate();
               return ActiveJobResult.FINISHED;
            }
         }

         this.worker.regenRecreation(50.0F, 1.0F, WaterGazingTileLevelJob.this.getRecreationExhaustionIdentifier());
         return ActiveJobResult.PERFORMING;
      }

      @Override
      public void onCancelled(boolean becauseOfInvalid, boolean isCurrent, boolean isMovingTo) {
         super.onCancelled(becauseOfInvalid, isCurrent, isMovingTo);
         this.chatterHandler.invalidate();
      }

      @Override
      public JobWorkerChatterHandler getChatterHandler() {
         return this.chatterHandler;
      }
   }
}
