package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.geom.Point2D.Float;
import java.util.Iterator;
import java.util.stream.Collectors;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.event.AIWasHitEvent;
import necesse.entity.mobs.ai.behaviourTree.trees.BirdCritterAI;
import necesse.entity.mobs.friendly.critters.BirdMob;
import necesse.entity.mobs.mobMovement.MobMovementConstant;

public class BirdCritterRunAINode<T extends BirdMob> extends AINode<T> {
   public int flyAwayFromPlayers;
   protected Float runDir;
   public int flyAwayIn;

   public BirdCritterRunAINode(int flyAwayFromPlayers) {
      this.flyAwayFromPlayers = flyAwayFromPlayers;
      this.flyAwayIn = GameRandom.globalRandom.getIntBetween(40, 2400);
   }

   public BirdCritterRunAINode() {
      this(150);
   }

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      Iterator closest = blackboard.getLastHits().iterator();
      if (closest.hasNext()) {
         AIWasHitEvent e = (AIWasHitEvent)closest.next();
         Mob attackOwner = e.event.attacker != null ? e.event.attacker.getAttackOwner() : null;
         float deltaX;
         float deltaY;
         if (attackOwner != null) {
            deltaX = mob.getX() - attackOwner.getX();
            deltaY = mob.getY() - attackOwner.getY();
         } else {
            deltaX = (float)GameRandom.globalRandom.nextGaussian();
            deltaY = (float)GameRandom.globalRandom.nextGaussian();
         }

         this.flyAway(deltaX, deltaY);
      }

      if (mob.isRunning()) {
         if (blackboard.mover.hasMovingNode()) {
            blackboard.mover.stopMoving(mob);
         }

         if (this.runDir != null) {
            mob.setMovement(new MobMovementConstant(this.runDir.x, this.runDir.y));
         }

         if (EscapeAINode.canEscape(mob, 3)) {
            mob.remove();
         }
      } else {
         this.flyAwayIn--;
         if (this.flyAwayIn <= 0) {
            ServerClient randomClient = GameRandom.globalRandom.getOneOf(GameUtils.streamServerClients(mob.getLevel()).collect(Collectors.toList()));
            float deltaX;
            float deltaY;
            if (randomClient != null) {
               int posXOffset = GameRandom.globalRandom.getIntOffset(randomClient.playerMob.getX(), 200);
               int posYOffset = GameRandom.globalRandom.getIntOffset(randomClient.playerMob.getY(), 200);
               deltaX = posXOffset - mob.x;
               deltaY = posYOffset - mob.y;
            } else {
               deltaX = (float)GameRandom.globalRandom.nextGaussian();
               deltaY = (float)GameRandom.globalRandom.nextGaussian();
            }

            this.flyAway(deltaX, deltaY);
         } else if (this.flyAwayFromPlayers > 0) {
            CritterRunAINode.TempDistance closestx = mob.getLevel()
               .entityManager
               .players
               .getInRegionByTileRange(mob.getTileX(), mob.getTileY(), this.flyAwayFromPlayers / 32 + 2)
               .stream()
               .map(p -> new CritterRunAINode.TempDistance(p, mob))
               .min((p1, p2) -> java.lang.Float.compare(p1.distance, p2.distance))
               .orElse(null);
            if (closestx != null && closestx.distance <= this.flyAwayFromPlayers) {
               float deltaX = mob.x - closestx.player.x;
               float deltaY = mob.y - closestx.player.y;
               this.flyAway(deltaX, deltaY);
            }
         }
      }

      return AINodeResult.SUCCESS;
   }

   public <T extends BirdMob> void flyAway(float deltaX, float deltaY) {
      this.runDir = GameMath.normalize(deltaX, deltaY);
      if (Math.abs(this.runDir.y) > 0.5F) {
         this.runDir = GameMath.normalize(this.runDir.x == 0.0F ? 1.0F : Math.signum(this.runDir.x), Math.signum(this.runDir.y) * 0.5F);
      }

      this.mob().setRunning(true);
      this.mob()
         .getLevel()
         .entityManager
         .mobs
         .streamInRegionsInTileRange(this.mob().getX(), this.mob().getY(), 5)
         .filter(m -> m instanceof BirdMob && m.ai.tree instanceof BirdCritterAI)
         .filter(m -> m.getDistance(this.mob()) < 100.0F)
         .forEach(m -> {
            BirdCritterRunAINode<?> runNode = ((BirdCritterAI)m.ai.tree).runNode;
            if (runNode.runDir == null) {
               runNode.flyAway(deltaX, deltaY);
            }
         });
   }
}
