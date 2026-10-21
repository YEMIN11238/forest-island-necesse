package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.Point;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.ai.behaviourTree.event.AIWasHitEvent;
import necesse.entity.mobs.ai.behaviourTree.event.TargetTileAIEvent;
import necesse.entity.mobs.friendly.critters.CritterMob;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.levelData.settlementData.ZoneTester;

public class CritterRunAINode<T extends CritterMob> extends MoveTaskAINode<T> {
   public boolean runsFromPlayers = true;
   public int runDistance = 150;
   public long nextRunCooldown;
   public String startRunEventType = "startRun";

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
      if (mob.isRunning() && !mob.getLevel().isCave && !(mob.getLevel() instanceof IncursionLevel) && EscapeAINode.canEscape(mob, 3)) {
         mob.remove();
         return AINodeResult.SUCCESS;
      }

      if (mob.isRunning() && !blackboard.mover.isMoving()) {
         mob.setRunning(false);
      }

      for (AIWasHitEvent e : blackboard.getLastHits()) {
         Mob attackOwner = e.event.attacker != null ? e.event.attacker.getAttackOwner() : null;
         if (attackOwner != null) {
            AINodeResult result = this.startRun(mob, mob.getTileX(), mob.getTileY(), attackOwner);
            if (result != null) {
               return result;
            }
         } else {
            AINodeResult result = this.startRun(mob, mob.getTileX(), mob.getTileY(), e.event.knockbackX, e.event.knockbackY, (ZoneTester)null);
            if (result != null) {
               return result;
            }
         }
      }

      if (this.runsFromPlayers && this.nextRunCooldown < mob.getTime()) {
         CritterRunAINode.TempDistance closest = mob.getLevel()
            .entityManager
            .players
            .getInRegionByTileRange(mob.getTileX(), mob.getTileY(), this.runDistance / 32 + 2)
            .stream()
            .map(p -> new CritterRunAINode.TempDistance(p, mob))
            .min((p1, p2) -> Float.compare(p1.distance, p2.distance))
            .orElse(null);
         if (closest != null && closest.distance <= this.runDistance) {
            AINodeResult result = this.startRun(mob, closest.player.getTileX(), closest.player.getTileY(), closest.player);
            if (result != null) {
               return result;
            }
         }
      }

      return AINodeResult.FAILURE;
   }

   public AINodeResult startRun(T mob, int startTileX, int startTileY, float dx, float dy, ZoneTester zoneTester) {
      int runDistanceTile = (int)Math.ceil(this.runDistance / 32.0F);
      java.awt.geom.Point2D.Float dir = GameMath.normalize(dx, dy);
      float dirMod;
      if (Math.abs(dir.x) > Math.abs(dir.y)) {
         dirMod = 1.0F / Math.abs(dir.x);
      } else {
         dirMod = 1.0F / Math.abs(dir.y);
      }

      dir.x *= dirMod;
      dir.y *= dirMod;
      int radius = 8 + runDistanceTile;
      Point runPoint = WandererAINode.findWanderingPointAround(
         mob, startTileX + (int)(dir.x * radius), startTileY + (int)(dir.y * radius), radius, zoneTester, 20, 5
      );
      this.nextRunCooldown = mob.getTime() + 2000L;
      if (runPoint != null) {
         return this.moveToTileTask(runPoint.x, runPoint.y, null, path -> {
            mob.setRunning(path.moveIfWithin(-1, -1, null));
            if (path.result != null && path.result.target != null) {
               this.getBlackboard().submitEvent(this.startRunEventType, new TargetTileAIEvent(path.result.target.x, path.result.target.y));
            }

            return AINodeResult.FAILURE;
         });
      }

      runPoint = WandererAINode.findWanderingPointAround(mob, startTileX, startTileY, radius * 2, zoneTester, 20, 5);
      return runPoint != null ? this.moveToTileTask(runPoint.x, runPoint.y, null, path -> {
         mob.setRunning(path.moveIfWithin(-1, -1, null));
         if (path.result != null && path.result.target != null) {
            this.getBlackboard().submitEvent(this.startRunEventType, new TargetTileAIEvent(path.result.target.x, path.result.target.y));
         }

         return AINodeResult.FAILURE;
      }) : null;
   }

   public AINodeResult startRun(T mob, int startTileX, int startTileY, float dx, float dy, Mob threat) {
      int runDistanceTile = (int)Math.ceil(this.runDistance / 32.0F);
      ZoneTester zoneTester = (tileX, tileY) -> {
         double distance = GameMath.diagonalMoveDistance(tileX, tileY, threat.getTileX(), threat.getTileY());
         return distance >= runDistanceTile + 2;
      };
      return this.startRun(mob, startTileX, startTileY, dx, dy, zoneTester);
   }

   public AINodeResult startRun(T mob, int startTileX, int startTileY, Mob threat) {
      return this.startRun(mob, startTileX, startTileY, mob.x - threat.x, mob.y - threat.y, threat);
   }

   protected static class TempDistance {
      public final PlayerMob player;
      public final float distance;

      public TempDistance(PlayerMob player, Mob mob) {
         this.player = player;
         this.distance = player.getDistance(mob);
      }
   }
}
