package necesse.level.maps.levelData.jobs;

import java.awt.Point;
import java.awt.geom.Line2D.Float;
import necesse.engine.AreaFinder;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.save.LoadData;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.HumanMobChatterHandler;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.JobWorkerChatterHandler;
import necesse.entity.mobs.job.SingleJobSequence;
import necesse.entity.mobs.job.activeJob.ActiveJob;
import necesse.entity.mobs.job.activeJob.ActiveJobResult;
import necesse.entity.mobs.job.activeJob.TileActiveJob;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.levelData.settlementData.settler.SettlerRecreationExhaustionManager;

public class EnjoyArtTileLevelJob extends TileLevelJob {
   public int objectID;
   public int objectRotation;
   public int startSearchTileDistance;
   public boolean hasCalculatedFoundMoveToTile;
   public Point foundMoveToTile;

   public EnjoyArtTileLevelJob(int tileX, int tileY, GameObject object, int objectRotation, int startSearchTileDistance) {
      super(tileX, tileY);
      this.objectID = object.getID();
      this.objectRotation = objectRotation;
      this.startSearchTileDistance = startSearchTileDistance;
   }

   public EnjoyArtTileLevelJob(LoadData save) {
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
      } else if (this.getLevel().getObjectID(this.tileX, this.tileY) != this.objectID) {
         return false;
      } else {
         return !this.hasCalculatedFoundMoveToTile
            ? true
            : this.foundMoveToTile != null && !this.getLevel().isSolidTile(this.foundMoveToTile.x, this.foundMoveToTile.y);
      }
   }

   @Override
   public boolean estimateCanMoveTo(EntityJobWorker worker) {
      Point moveToTile = this.getOrCalculateFoundMoveToTile();
      return moveToTile == null ? false : worker.getMobWorker().estimateCanMoveTo(moveToTile.x, moveToTile.y, false);
   }

   @Override
   public int groupDistanceToJobByTiles(EntityJobWorker worker) {
      return 10;
   }

   public Point getOrCalculateFoundMoveToTile() {
      if (!this.hasCalculatedFoundMoveToTile) {
         this.hasCalculatedFoundMoveToTile = true;
         int startTileX;
         int startTileY;
         if (this.objectRotation == 0) {
            startTileX = this.tileX;
            startTileY = this.tileY - this.startSearchTileDistance;
         } else if (this.objectRotation == 1) {
            startTileX = this.tileX + this.startSearchTileDistance;
            startTileY = this.tileY;
         } else if (this.objectRotation == 2) {
            startTileX = this.tileX;
            startTileY = this.tileY + this.startSearchTileDistance;
         } else {
            startTileX = this.tileX - this.startSearchTileDistance;
            startTileY = this.tileY;
         }

         AreaFinder finder = new AreaFinder(startTileX, startTileY, this.startSearchTileDistance / 2, true) {
            @Override
            public boolean checkPoint(int x, int y) {
               return EnjoyArtTileLevelJob.this.getLevel().isSolidTile(x, y) ? false : EnjoyArtTileLevelJob.this.checkLOSToArt(x, y);
            }
         };
         finder.runFinder();
         this.foundMoveToTile = finder.getFirstFind();
      }

      return this.foundMoveToTile;
   }

   public boolean checkLOSToArt(int fromTileX, int fromTileY) {
      return !this.getLevel()
         .collides(
            new Float(this.tileX * 32 + 16, this.tileY * 32 + 16, fromTileX * 32 + 16, fromTileY * 32 + 16), 16.0F, 4.0F, new CollisionFilter().wallCollision()
         );
   }

   public static <T extends EnjoyArtTileLevelJob> JobSequence getJobSequence(HumanMob humanMob, FoundJob<T> foundJob) {
      Point moveToTile = foundJob.job.getOrCalculateFoundMoveToTile();
      if (moveToTile == null) {
         return null;
      } else if (!foundJob.job.checkLOSToArt(moveToTile.x, moveToTile.y)) {
         foundJob.job.foundMoveToTile = null;
         return null;
      } else {
         GameMessage activityDescription = new LocalMessage("activities", "enjoyingart");
         return new SingleJobSequence(foundJob.job.getActiveJob(humanMob, foundJob.priority, moveToTile), activityDescription)
            .withPerformedLevelJob(foundJob.job)
            .markRecreationJob();
      }
   }

   public ActiveJob getActiveJob(HumanMob humanMob, JobTypeHandler.TypePriority priority, Point moveToTile) {
      return new EnjoyArtTileLevelJob.EnjoyArtActiveJob(humanMob, priority, this.tileX, this.tileY, moveToTile);
   }

   public static JobTypeHandler.SubHandler<EnjoyArtTileLevelJob> handler(EntityJobWorker worker, JobTypeHandler handler) {
      if (worker instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)worker;
         return handler.<EnjoyArtTileLevelJob>setJobHandler(EnjoyArtTileLevelJob.class, foundJob -> getJobSequence(humanMob, foundJob))
            .setPredicate(() -> !humanMob.isOnStrike() && (!humanMob.isSettler() || humanMob.isSettlerWithinSettlement()));
      } else {
         return null;
      }
   }

   private class EnjoyArtActiveJob extends TileActiveJob {
      protected HumanMob humanMob;
      protected int maxTicksRemaining = 20 * GameRandom.globalRandom.getIntBetween(20, 30);
      protected boolean hasSetDirection = false;
      protected Point moveToTile;
      protected HumanMobChatterHandler chatterHandler;

      public EnjoyArtActiveJob(HumanMob humanMob, JobTypeHandler.TypePriority priority, int tileX, int tileY, Point moveToTile) {
         super(humanMob, priority, tileX, tileY);
         this.humanMob = humanMob;
         this.moveToTile = moveToTile;
         this.chatterHandler = new HumanMobChatterHandler(humanMob, false);
      }

      @Override
      public JobMoveToTile getMoveToTile(JobMoveToTile lastTile) {
         return this.moveToTile == null ? null : new JobMoveToTile(this.moveToTile.x, this.moveToTile.y, false);
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
         if (EnjoyArtTileLevelJob.this.isRemoved()) {
            return false;
         } else {
            return this.moveToTile == null ? false : EnjoyArtTileLevelJob.this.isValid();
         }
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

         this.worker.regenRecreation(50.0F, 1.0F, EnjoyArtTileLevelJob.this.getRecreationExhaustionIdentifier());
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
