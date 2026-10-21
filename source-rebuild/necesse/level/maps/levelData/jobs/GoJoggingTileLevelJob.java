package necesse.level.maps.levelData.jobs;

import java.awt.Point;
import java.util.ArrayList;
import java.util.stream.Stream;
import necesse.engine.GameTileRange;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.SingleJobSequence;
import necesse.entity.mobs.job.activeJob.ActiveJob;
import necesse.entity.mobs.job.activeJob.ActiveJobResult;
import necesse.level.maps.levelData.settlementData.ZoneTester;
import necesse.level.maps.levelData.settlementData.settler.SettlerRecreationExhaustionManager;

public class GoJoggingTileLevelJob extends TileLevelJob {
   public static GameTileRange NEXT_SEARCH_RANGE = new GameTileRange(10, 15);

   public GoJoggingTileLevelJob(int tileX, int tileY) {
      super(tileX, tileY);
   }

   public GoJoggingTileLevelJob(LoadData save) {
      super(save);
   }

   @Override
   public String getRecreationExhaustionIdentifier() {
      return SettlerRecreationExhaustionManager.EXERCISING_IDENTIFIER;
   }

   @Override
   public boolean isValid() {
      return super.isValid() && this.getLevel().isOutside(this.tileX, this.tileY);
   }

   @Override
   public int groupDistanceToJobByTiles(EntityJobWorker worker) {
      return NEXT_SEARCH_RANGE.maxRange;
   }

   public static <T extends GoJoggingTileLevelJob> JobSequence getJobSequence(HumanMob humanMob, FoundJob<T> foundJob) {
      GameMessage activityDescription = new LocalMessage("activities", "jogging");
      return new SingleJobSequence(foundJob.job.getActiveJob(humanMob, foundJob.priority), activityDescription)
         .withPerformedLevelJob(foundJob.job)
         .markRecreationJob();
   }

   public ActiveJob getActiveJob(HumanMob humanMob, JobTypeHandler.TypePriority priority) {
      return new GoJoggingTileLevelJob.GoJoggingActiveJob(humanMob, priority, new Point(this.tileX, this.tileY));
   }

   public static Point findNextTargetTile(Mob mob, ZoneTester zoneTester) {
      Point pathOffset = mob.getPathMoveOffset();
      ArrayList<Point> tiles = new ArrayList<>(NEXT_SEARCH_RANGE.size());
      int startTileX = mob.getTileX();
      int startTileY = mob.getTileY();

      for (Point tile : NEXT_SEARCH_RANGE.getValidTiles(startTileX, startTileY)) {
         if ((zoneTester == null || zoneTester.containsTile(tile.x, tile.y))
            && (startTileX != tile.x || startTileY != tile.y)
            && !mob.getLevel().isSolidTile(tile.x, tile.y)
            && !mob.getLevel().isLiquidTile(tile.x, tile.y)
            && mob.getLevel().isOutside(tile.x, tile.y)
            && !mob.collidesWith(mob.getLevel(), tile.x * 32 + pathOffset.x, tile.y * 32 + pathOffset.y)) {
            tiles.add(tile);
         }
      }

      for (int attempts = 50; !tiles.isEmpty() && attempts > 0; attempts--) {
         int index = GameRandom.globalRandom.nextInt(tiles.size());
         Point tile = tiles.get(index);
         if (mob.estimateCanMoveTo(tile.x, tile.y, false)) {
            return tile;
         }

         tiles.remove(index);
      }

      return null;
   }

   public static JobTypeHandler.JobStreamSupplier<? extends GoJoggingTileLevelJob> getJobStreamer() {
      return (worker, handler) -> {
         Mob mob = worker.getMobWorker();
         if (worker.getLevel().isOutside(mob.getTileX(), mob.getTileY())) {
            Point nextTargetTile = findNextTargetTile(mob, worker.getJobRestrictZone());
            if (nextTargetTile != null) {
               return Stream.of(new GoJoggingTileLevelJob(nextTargetTile.x, nextTargetTile.y));
            }
         }

         return Stream.empty();
      };
   }

   public static JobTypeHandler.SubHandler<GoJoggingTileLevelJob> handler(EntityJobWorker worker, JobTypeHandler handler) {
      if (worker instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)worker;
         return handler.<GoJoggingTileLevelJob>setJobHandler(GoJoggingTileLevelJob.class, foundJob -> getJobSequence(humanMob, foundJob))
            .setPredicate(() -> !humanMob.isOnStrike() && (!humanMob.isSettler() || humanMob.isSettlerWithinSettlement()))
            .setExtraJobStreamer(false, getJobStreamer());
      } else {
         return null;
      }
   }

   private static class GoJoggingActiveJob extends ActiveJob {
      protected HumanMob humanMob;
      protected Point nextTargetTile;
      protected int maxTicksRemaining = 20 * GameRandom.globalRandom.getIntBetween(20, 30);
      public long nextApplyBuffTime;

      public GoJoggingActiveJob(HumanMob humanMob, JobTypeHandler.TypePriority priority, Point initialTargetTile) {
         super(humanMob, priority);
         this.humanMob = humanMob;
         this.nextTargetTile = initialTargetTile;
      }

      @Override
      public JobMoveToTile getMoveToTile(JobMoveToTile lastTile) {
         return this.nextTargetTile != null ? new JobMoveToTile(this.nextTargetTile.x, this.nextTargetTile.y, false) : null;
      }

      @Override
      public boolean isAt(JobMoveToTile moveToTile) {
         return this.worker.getMobWorker().getDistance(moveToTile.tileX * 32 + 16, moveToTile.tileY * 32 + 16) <= 32.0F;
      }

      @Override
      public void tick(boolean isCurrent, boolean isMovingTo) {
         if (isCurrent && this.maxTicksRemaining > 0) {
            this.maxTicksRemaining--;
         }

         if (isMovingTo) {
            this.worker.regenRecreation(50.0F, 1.0F, SettlerRecreationExhaustionManager.EXERCISING_IDENTIFIER);
            Mob mob = this.worker.getMobWorker();
            if (this.nextApplyBuffTime <= mob.getTime()) {
               mob.buffManager.addBuff(new ActiveBuff(BuffRegistry.SETTLER_JOG, mob, 2.0F, null), true);
               this.nextApplyBuffTime = mob.getTime() + 1500L;
            }
         }
      }

      @Override
      public boolean isValid(boolean isCurrent) {
         return true;
      }

      @Override
      public ActiveJobResult perform() {
         Mob mob = this.worker.getMobWorker();
         if (this.maxTicksRemaining <= 0) {
            mob.buffManager.removeBuff(BuffRegistry.SETTLER_JOG, true);
            return ActiveJobResult.FINISHED;
         } else {
            this.nextTargetTile = GoJoggingTileLevelJob.findNextTargetTile(mob, this.worker.getJobRestrictZone());
            if (this.nextTargetTile == null) {
               mob.buffManager.removeBuff(BuffRegistry.SETTLER_JOG, true);
               return ActiveJobResult.FINISHED;
            } else {
               return ActiveJobResult.MOVE_TO;
            }
         }
      }

      @Override
      public void onCancelled(boolean becauseOfInvalid, boolean isCurrent, boolean isMovingTo) {
         super.onCancelled(becauseOfInvalid, isCurrent, isMovingTo);
         this.worker.getMobWorker().buffManager.removeBuff(BuffRegistry.SETTLER_JOG, true);
      }
   }
}
