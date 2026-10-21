package necesse.level.maps.levelData.jobs;

import java.awt.Point;
import java.awt.geom.Line2D.Float;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketFishingStatus;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.util.GameRandom;
import necesse.entity.Entity;
import necesse.entity.levelEvent.fishingEvent.FishingEvent;
import necesse.entity.mobs.FishingMob;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.SingleJobSequence;
import necesse.entity.mobs.job.activeJob.ActiveJob;
import necesse.entity.mobs.job.activeJob.ActiveJobHitResult;
import necesse.entity.mobs.job.activeJob.ActiveJobResult;
import necesse.entity.mobs.job.activeJob.TileActiveJob;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.baitItem.BaitItem;
import necesse.inventory.item.placeableItem.fishingRodItem.FishingRodItem;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.LevelTile;
import necesse.level.maps.liquidManager.ClosestHeightResult;

public class FishingTileLevelJob extends TileLevelJob {
   public FishingTileLevelJob(int tileX, int tileY) {
      super(tileX, tileY);
   }

   public FishingTileLevelJob(LoadData save) {
      super(save);
   }

   public LevelTile getTile() {
      return this.getLevel().getLevelTile(this.tileX, this.tileY);
   }

   public int getLiquidHeight() {
      return this.getLevel().liquidManager.getHeight(this.tileX, this.tileY);
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
      return -5 < height && height < -1;
   }

   @Override
   public int getSameJobPriority() {
      return -this.getLiquidHeight();
   }

   @Override
   public int groupDistanceToJobByTiles(EntityJobWorker worker) {
      return 8;
   }

   protected static Point findClosestHeightTileSimple(Entity entity, int tileX, int tileY, int desiredHeight) {
      int currentHeight = entity.getLevel().liquidManager.getHeight(tileX, tileY);
      Point currentTile = new Point(tileX, tileY);
      if (currentHeight == desiredHeight) {
         return currentTile;
      }

      boolean goDown = desiredHeight < currentHeight;
      int mobTileX = entity.getTileX();
      int mobTileY = entity.getTileY();
      int[][] dirs = new int[][]{{-1, 0, 1}, {0, 1, -1}, {1, 0, -1}};

      while (true) {
         int[] checkX = dirs[Integer.compare(mobTileX - currentTile.x, 0) + 1];
         int[] checkY = dirs[Integer.compare(mobTileY - currentTile.y, 0) + 1];
         Point nextTile = null;

         for (int x : checkX) {
            int nextX = currentTile.x + x;
            if (entity.getLevel().isTileXWithinBounds(nextX)) {
               for (int y : checkY) {
                  if (x != 0 || y != 0) {
                     int nextY = currentTile.y + y;
                     if (entity.getLevel().isTileYWithinBounds(nextY) && !entity.getLevel().isSolidTile(nextX, nextY)) {
                        int height = entity.getLevel().liquidManager.getHeight(nextX, nextY);
                        if (height == desiredHeight) {
                           return new Point(nextX, nextY);
                        }

                        if (goDown && height < currentHeight) {
                           currentHeight = height;
                           nextTile = new Point(nextX, nextY);
                        } else if (height > currentHeight) {
                           currentHeight = height;
                           nextTile = new Point(nextX, nextY);
                        }
                     }
                  }
               }
            }
         }

         if (nextTile == null) {
            return null;
         }

         currentTile = nextTile;
      }
   }

   public static <T extends FishingTileLevelJob> JobSequence getJobSequence(EntityJobWorker worker, FishingMob fishingMob, FoundJob<T> foundJob) {
      GameMessage activityDescription = new LocalMessage("activities", "fishing");
      return new SingleJobSequence(foundJob.job.getActiveJob(worker, foundJob.priority, fishingMob), activityDescription).withPerformedLevelJob(foundJob.job);
   }

   public ActiveJob getActiveJob(EntityJobWorker worker, JobTypeHandler.TypePriority priority, FishingMob fishingMob) {
      return new FishingTileLevelJob.FishingActiveJob(worker, priority, this.tileX, this.tileY, fishingMob);
   }

   public static JobTypeHandler.SubHandler<FishingTileLevelJob> handler(EntityJobWorker worker, JobTypeHandler handler) {
      if (worker instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)worker;
         return handler.<FishingTileLevelJob>setJobHandler(FishingTileLevelJob.class, foundJob -> getJobSequence(humanMob, humanMob, foundJob))
            .setPredicate(
               () -> !humanMob.isOnStrike()
                  && !humanMob.hasCompletedMission()
                  && (!humanMob.isSettler() || humanMob.isSettlerWithinSettlement())
                  && !humanMob.isInventoryFull(true)
            );
      } else {
         return null;
      }
   }

   private class FishingActiveJob extends TileActiveJob {
      protected FishingMob fishingMob;
      protected FishingEvent fishingEvent;
      protected int maxWaitTicker;

      public FishingActiveJob(EntityJobWorker worker, JobTypeHandler.TypePriority priority, int tileX, int tileY, FishingMob fishingMob) {
         super(worker, priority, tileX, tileY);
         this.fishingMob = fishingMob;
      }

      @Override
      public JobMoveToTile getMoveToTile(JobMoveToTile lastTile) {
         CollisionFilter collisionFilter = ((Mob)this.fishingMob).modifyChasingCollisionFilter(new CollisionFilter().projectileCollision(), null);
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
      public boolean isAt(JobMoveToTile moveToTile) {
         Mob mob = this.worker.getMobWorker();
         double dist = new Point(mob.getX(), mob.getY()).distance(moveToTile.tileX * 32 + 16, moveToTile.tileY * 32 + 16);
         return dist < 32.0;
      }

      @Override
      public boolean isValid(boolean isCurrent) {
         return FishingTileLevelJob.this.isRemoved() ? false : FishingTileLevelJob.this.isValid();
      }

      @Override
      public ActiveJobHitResult onHit(MobWasHitEvent event, boolean isMovingTo) {
         if (this.fishingEvent != null && !this.fishingEvent.isReeled) {
            this.fishingEvent.reel();
            if (FishingTileLevelJob.this.isServer()) {
               this.getLevel().getServer().network.sendToClientsWithEntity(PacketFishingStatus.getReelPacket(this.fishingEvent), this.fishingEvent);
            }
         }

         return ActiveJobHitResult.CLEAR_SEQUENCE;
      }

      @Override
      public void onCancelled(boolean becauseOfInvalid, boolean isCurrent, boolean isMovingTo) {
         if (this.fishingEvent != null && !this.fishingEvent.isReeled) {
            this.fishingEvent.reel();
            if (FishingTileLevelJob.this.isServer()) {
               this.getLevel().getServer().network.sendToClientsWithEntity(PacketFishingStatus.getReelPacket(this.fishingEvent), this.fishingEvent);
            }
         }
      }

      @Override
      public ActiveJobResult perform() {
         if (this.fishingEvent == null) {
            FishingRodItem fishingRod = (FishingRodItem)ItemRegistry.getItem("ironfishingrod");
            BaitItem baitItem = (BaitItem)ItemRegistry.getItem("wormbait");
            this.fishingEvent = new FishingEvent(
               this.fishingMob, this.tileX * 32 + 16, this.tileY * 32 + 16, fishingRod, baitItem, this.worker.getWorkAnimSpeedMultiplier()
            );
            this.getLevel().entityManager.events.add(this.fishingEvent);
            this.worker
               .showAttackAnimation(
                  this.tileX * 32 + 16, this.tileY * 32 + 16, fishingRod, fishingRod.getAttackAnimTime(new InventoryItem(fishingRod), null), true
               );
            this.maxWaitTicker = 600;
         } else if (!this.fishingEvent.isReeled) {
            this.maxWaitTicker--;
            if (this.maxWaitTicker <= 0 || this.fishingEvent.getTicksToNextCatch() == -10 && GameRandom.globalRandom.getChance(0.7F)) {
               this.fishingEvent.reel();
               if (FishingTileLevelJob.this.isServer()) {
                  this.getLevel().getServer().network.sendToClientsWithEntity(PacketFishingStatus.getReelPacket(this.fishingEvent), this.fishingEvent);
               }
            }
         }

         if (this.fishingEvent.isOver()) {
            this.fishingEvent = null;
            return ActiveJobResult.FINISHED;
         } else {
            return ActiveJobResult.PERFORMING;
         }
      }
   }
}
