package necesse.level.maps.levelData.jobs;

import java.awt.Point;
import java.awt.geom.Line2D.Float;
import java.util.ArrayList;
import necesse.engine.GameTileRange;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.save.LoadData;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.SingleJobSequence;
import necesse.entity.mobs.job.activeJob.ActiveJob;
import necesse.entity.mobs.job.activeJob.ActiveJobResult;
import necesse.entity.mobs.job.activeJob.TileActiveJob;
import necesse.entity.objectEntity.MusicPlayerObjectEntity;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.levelData.settlementData.ZoneTester;
import necesse.level.maps.levelData.settlementData.settler.SettlerRecreationExhaustionManager;

public class ListenToMusicTileLevelJob extends TileLevelJob {
   public static GameTileRange NEXT_SEARCH_RANGE = new GameTileRange(1, 5);
   public int objectID;

   public ListenToMusicTileLevelJob(int tileX, int tileY, GameObject object) {
      super(tileX, tileY);
      this.objectID = object.getID();
   }

   public ListenToMusicTileLevelJob(LoadData save) {
      super(save);
   }

   @Override
   public String getRecreationExhaustionIdentifier() {
      return SettlerRecreationExhaustionManager.ENTERTAINMENT_IDENTIFIER;
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

      if (this.getLevel().getObjectID(this.tileX, this.tileY) != this.objectID) {
         return false;
      }

      MusicPlayerObjectEntity musicPlayer = this.getLevel().entityManager.getObjectEntity(this.tileX, this.tileY, MusicPlayerObjectEntity.class);
      return musicPlayer != null && !musicPlayer.isPaused() && musicPlayer.getMusicManager().hasAnyVinyls();
   }

   @Override
   public int groupDistanceToJobByTiles(EntityJobWorker worker) {
      return 10;
   }

   public boolean checkLOSToArt(int fromTileX, int fromTileY) {
      return !this.getLevel()
         .collides(
            new Float(this.tileX * 32 + 16, this.tileY * 32 + 16, fromTileX * 32 + 16, fromTileY * 32 + 16), 16.0F, 4.0F, new CollisionFilter().wallCollision()
         );
   }

   public static <T extends ListenToMusicTileLevelJob> JobSequence getJobSequence(HumanMob humanMob, FoundJob<T> foundJob) {
      GameMessage activityDescription = new LocalMessage("activities", "listeningtomusic");
      return new SingleJobSequence(foundJob.job.getActiveJob(humanMob, foundJob.priority), activityDescription)
         .withPerformedLevelJob(foundJob.job)
         .markRecreationJob();
   }

   public ActiveJob getActiveJob(HumanMob humanMob, JobTypeHandler.TypePriority priority) {
      return new ListenToMusicTileLevelJob.ListenToMusicActiveJob(humanMob, priority, this.tileX, this.tileY);
   }

   public static Point findNextTargetTile(Mob mob, int musicTileX, int musicTileY, ZoneTester zoneTester) {
      Point pathOffset = mob.getPathMoveOffset();
      ArrayList<Point> tiles = new ArrayList<>(NEXT_SEARCH_RANGE.size());
      int musicRoomID = mob.getLevel().getRoomID(musicTileX, musicTileY);

      for (Point tile : NEXT_SEARCH_RANGE.getValidTiles(musicTileX, musicTileY)) {
         if ((zoneTester == null || zoneTester.containsTile(tile.x, tile.y))
            && (musicTileX != tile.x || musicTileY != tile.y)
            && !mob.getLevel().isSolidTile(tile.x, tile.y)
            && !mob.getLevel().isLiquidTile(tile.x, tile.y)
            && mob.getLevel().getRoomID(tile.x, tile.y) == musicRoomID
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

   public static JobTypeHandler.SubHandler<ListenToMusicTileLevelJob> handler(EntityJobWorker worker, JobTypeHandler handler) {
      if (worker instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)worker;
         return handler.<ListenToMusicTileLevelJob>setJobHandler(ListenToMusicTileLevelJob.class, foundJob -> getJobSequence(humanMob, foundJob))
            .setPredicate(() -> !humanMob.isOnStrike() && (!humanMob.isSettler() || humanMob.isSettlerWithinSettlement()));
      } else {
         return null;
      }
   }

   private class ListenToMusicActiveJob extends TileActiveJob {
      protected HumanMob humanMob;
      protected Point nextTargetTile;
      protected int maxTicksRemaining = 20 * GameRandom.globalRandom.getIntBetween(20, 30);
      protected int moveTickCountdown = 20 * GameRandom.globalRandom.getIntBetween(2, 5);
      protected int danceSubmitCooldown = 1000;
      protected long lastDanceSubmitTime;

      public ListenToMusicActiveJob(HumanMob humanMob, JobTypeHandler.TypePriority priority, int tileX, int tileY) {
         super(humanMob, priority, tileX, tileY);
         this.humanMob = humanMob;
      }

      @Override
      public JobMoveToTile getMoveToTile(JobMoveToTile lastTile) {
         return this.nextTargetTile != null
            ? new JobMoveToTile(this.nextTargetTile.x, this.nextTargetTile.y, false)
            : new JobMoveToTile(this.tileX, this.tileY, true);
      }

      @Override
      public void tick(boolean isCurrent, boolean isMovingTo) {
         if (isCurrent && isMovingTo) {
            this.maxTicksRemaining--;
         }
      }

      @Override
      public boolean isAt(JobMoveToTile moveToTile) {
         return this.maxTicksRemaining <= 0 ? true : super.isAt(moveToTile);
      }

      @Override
      public int getCompleteRange() {
         return this.nextTargetTile != null ? 8 : super.getCompleteRange();
      }

      @Override
      public boolean isValid(boolean isCurrent) {
         return ListenToMusicTileLevelJob.this.isRemoved() ? false : ListenToMusicTileLevelJob.this.isValid();
      }

      @Override
      public ActiveJobResult perform() {
         this.maxTicksRemaining--;
         if (this.maxTicksRemaining <= 0) {
            this.humanMob.stopDanceAnimationNow();
            return ActiveJobResult.FINISHED;
         }

         this.moveTickCountdown--;
         if (this.moveTickCountdown <= 0) {
            this.nextTargetTile = ListenToMusicTileLevelJob.findNextTargetTile(this.humanMob, this.tileX, this.tileY, this.worker.getJobRestrictZone());
            this.moveTickCountdown = 20 * GameRandom.globalRandom.getIntBetween(2, 5);
            return ActiveJobResult.MOVE_TO;
         }

         long timeSinceLastDanceSubmit = this.getLevel().getTime() - this.lastDanceSubmitTime;
         if (timeSinceLastDanceSubmit >= this.danceSubmitCooldown) {
            this.humanMob.showDanceAnimation(this.danceSubmitCooldown + 500);
            this.lastDanceSubmitTime = this.getLevel().getTime();
         }

         this.worker.regenRecreation(50.0F, 1.0F, ListenToMusicTileLevelJob.this.getRecreationExhaustionIdentifier());
         return ActiveJobResult.PERFORMING;
      }
   }
}
