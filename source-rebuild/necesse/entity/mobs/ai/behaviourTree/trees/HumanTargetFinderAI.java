package necesse.entity.mobs.ai.behaviourTree.trees;

import java.awt.Point;
import java.util.function.BiPredicate;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.util.ComputedValue;
import necesse.engine.util.GameMath;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.engine.util.pathfinding.PathResult;
import necesse.engine.util.pathfinding.Pathfinding;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.HumanAngerTargetAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.HumanCommandAttackTargetterAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.TargetFinderAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.WandererAINode;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.ai.behaviourTree.util.TargetValidity;
import necesse.entity.mobs.ai.path.TilePathfinding;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.NetworkSettlementData;
import necesse.level.maps.levelData.settlementData.ZoneTester;
import necesse.level.maps.regionSystem.SubRegion;

public class HumanTargetFinderAI<T extends HumanMob> extends SelectorAINode<T> {
   public final HumanAngerTargetAINode<T> humanAngerTargetAINode;
   public boolean attackHostiles;
   public boolean ignoreHiding;

   public HumanTargetFinderAI(int searchDistance, boolean attackHostiles, boolean ignoreHiding) {
      this.attackHostiles = attackHostiles;
      this.ignoreHiding = ignoreHiding;
      TargetFinderDistance<T> distance = new TargetFinderDistance<T>(searchDistance) {
         protected int getSearchDistanceFlat(T mob, Mob target) {
            int addedDistance = 0;
            boolean homeOutside = mob.hasCommandOrders() || mob.home == null || mob.getLevel().isOutside(mob.home.x, mob.home.y);
            if (!homeOutside) {
               addedDistance = 160;
            }

            NetworkSettlementData settlement = mob.getSettlerSettlementNetworkData();
            if (settlement != null && settlement.isRaidActive()) {
               addedDistance = 320;
            }

            return super.getSearchDistanceFlat(mob, target) + addedDistance;
         }
      };
      distance.targetLostAddedDistance = 160;
      TargetValidity<T> validity = new TargetValidity<T>() {
         public boolean isValidTarget(AINode<T> node, T mob, Mob target, boolean isNewTarget) {
            if (!super.isValidTarget(node, mob, target, isNewTarget)) {
               return false;
            }

            boolean homeOutside = mob.isSettler() && !mob.isSettlerOnCurrentLevel()
               || mob.hasCommandOrders() && !mob.isHiding
               || mob.home == null
               || mob.getLevel().isOutside(mob.home.x, mob.home.y);
            boolean isHidingInside = HumanTargetFinderAI.this.getBlackboard().getObject(Boolean.class, "isHidingInside", false);
            ComputedValue<ZoneTester> zoneTester = new ComputedValue<>(
               () -> mob.levelSettler != null && !mob.hasCommandOrders() ? mob.levelSettler.isTileInSettlementBoundsAndRestrictZoneTester() : (x, y) -> true
            );
            return HumanTargetFinderAI.this.isValidTarget(mob, target, homeOutside, isHidingInside, zoneTester, isNewTarget);
         }
      };
      this.addChild(this.humanAngerTargetAINode = new HumanAngerTargetAINode<>(distance, validity));
      this.addChild(new HumanCommandAttackTargetterAINode<>());
      TargetFinderAINode<T> targetFinder = new TargetFinderAINode<T>(distance, validity) {
         public GameAreaStream<? extends Mob> streamPossibleTargets(T mob, Point base, TargetFinderDistance<T> distancex) {
            return HumanTargetFinderAI.this.streamHumanAITargets(mob, distancex);
         }
      };
      targetFinder.moveToAttacker = false;
      targetFinder.runOnGlobalTick = true;
      targetFinder.loseTargetMinCooldown = 1000;
      targetFinder.loseTargetMaxCooldown = 3000;
      targetFinder.noTargetFoundMinCooldown = 750;
      targetFinder.noTargetFoundMaxCooldown = 1250;
      this.addChild(targetFinder);
   }

   protected boolean isTargetSameRoom(Mob target, int roomID) {
      int tileX = target.getTileX();
      int tileY = target.getTileY();
      SubRegion subRegion = target.getLevel().regionManager.getSubRegionByTile(tileX, tileY);
      if (subRegion == null) {
         return roomID == 0;
      } else {
         return subRegion.getRoomID() == roomID ? true : subRegion.streamAdjacentRegions().anyMatch(r -> r.getRoomID() == roomID);
      }
   }

   public boolean isValidTarget(T mob, Mob target, boolean homeOutside, boolean isHidingInside, ComputedValue<ZoneTester> zoneTester, boolean isNewTarget) {
      if (this.humanAngerTargetAINode.enemies.contains(target)) {
         return true;
      }

      if (!homeOutside) {
         int roomID = mob.home == null ? 0 : mob.getLevel().getRoomID(mob.home.x, mob.home.y);
         if (this.isTargetSameRoom(target, roomID)) {
            return true;
         }

         if (mob.isHiding) {
            return false;
         }

         if (!this.ignoreHiding && isHidingInside) {
            if (WandererAINode.isTargetValidHidingInside(this.getBlackboard(), mob.getLevel(), target.getTileX(), target.getTileY())) {
               return true;
            }

            NetworkSettlementData settlement = mob.getSettlerSettlementNetworkData();
            if (settlement == null || !settlement.isRaidActive()) {
               return false;
            }
         }

         if (mob.home != null && !(GameMath.squareDistance(mob.home.x, mob.home.y, target.getTileX(), target.getTileY()) <= 20.0F)) {
            return false;
         }

         ZoneTester zone = zoneTester.get();
         return zone == null || zone.containsTile(target.getTileX(), target.getTileY());
      } else if (mob.hasCommandOrders()) {
         Point baseTile;
         if (mob.commandFollowMob != null && !mob.commandFollowMob.removed() && mob.isSamePlace(mob.commandFollowMob)) {
            baseTile = new Point(mob.commandFollowMob.getTileX(), mob.commandFollowMob.getTileY());
         } else if (mob.commandGuardPoint != null) {
            baseTile = new Point(GameMath.getTileCoordinate(mob.commandGuardPoint.x), GameMath.getTileCoordinate(mob.commandGuardPoint.y));
         } else {
            baseTile = new Point(mob.getTileX(), mob.getTileY());
         }

         int maxDistance = mob.isHiding ? 10 : 20;
         if (!isNewTarget) {
            maxDistance += 10;
         }

         if (GameMath.squareDistance(baseTile.x, baseTile.y, target.getTileX(), target.getTileY()) <= maxDistance) {
            if (!isNewTarget) {
               return true;
            }

            int finalMaxDistance = maxDistance;
            return Performance.record(
               mob.getLevel().tickManager(),
               "settlerGuardTarget",
               () -> {
                  BiPredicate<Point, Point> isAtTarget = mob.canBeTargetedFromAdjacentTiles()
                     ? TilePathfinding.isAtOrAdjacentObject(target.getLevel(), target.getTileX(), target.getTileY())
                     : null;
                  TilePathfinding tilePathFinding = new TilePathfinding(
                     mob.getLevel().tickManager(), mob.getLevel(), mob, isAtTarget, this.getBlackboard().mover.getPathOptions(this)
                  );
                  PathResult<Point, Pathfinding<Point>> result = tilePathFinding.findPath(
                     baseTile, new Point(target.getTileX(), target.getTileY()), finalMaxDistance + 5
                  );
                  return result.foundTarget;
               }
            );
         } else {
            return false;
         }
      } else {
         return mob.home == null || GameMath.squareDistance(mob.home.x, mob.home.y, target.getTileX(), target.getTileY()) <= 20.0F;
      }
   }

   public GameAreaStream<Mob> streamHumanAITargets(T mob, TargetFinderDistance<T> distance) {
      if (mob.commandFollowMob != null && mob.commandMoveToFollowPoint) {
         return GameAreaStream.empty();
      }

      if (mob.isVisitor()) {
         return GameAreaStream.empty();
      }

      boolean homeOutside = mob.hasCommandOrders() || mob.home == null || mob.getLevel().isOutside(mob.home.x, mob.home.y);
      Point base = new Point(mob.getX(), mob.getY());
      if (!homeOutside) {
         base = new Point(mob.home.x * 32 + 16, mob.home.y * 32 + 16);
      }

      Point finalBase = base;
      return distance.streamMobsAndPlayersInRange(base, mob)
         .filter(m -> m.canTakeDamage() && m.canBeHit(mob))
         .filter(mob.filterHumanTargets())
         .filter(target -> target.getDistance(finalBase.x, finalBase.y) < distance.getSearchDistance(mob, target));
   }
}
