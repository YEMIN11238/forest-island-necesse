package necesse.entity.mobs.ai.behaviourTree.trees;

import java.awt.geom.Point2D.Float;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.composites.SequenceAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.CooldownAttackTargetAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.FollowerBaseSetterAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.FollowerFocusTargetSetterAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.PlayerFlyingFollowerAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.SummonTargetFinderAINode;
import necesse.entity.mobs.mobMovement.MobMovementRelative;

public class PlayerFlyingFollowerChargeChaserAI<T extends Mob> extends SelectorAINode<T> {
   protected int lastTarget;

   public PlayerFlyingFollowerChargeChaserAI(
      int searchDistance,
      CooldownAttackTargetAINode.CooldownTimer cooldownTimer,
      final int chargeCooldown,
      final int targetStoppingDistance,
      int teleportDistance,
      int stoppingDistance
   ) {
      SequenceAINode<T> chaserSequence = new SequenceAINode<>();
      chaserSequence.addChild(new FollowerBaseSetterAINode<>());
      chaserSequence.addChild(new FollowerFocusTargetSetterAINode<>());
      SummonTargetFinderAINode<T> targetFinder = new SummonTargetFinderAINode<>(searchDistance);
      chaserSequence.addChild(targetFinder);
      final CooldownAttackTargetAINode<T> attackNode = new CooldownAttackTargetAINode<T>(cooldownTimer, chargeCooldown, -1) {
         @Override
         public boolean attackTarget(T mob, Mob target) {
            Float currentDir;
            if (PlayerFlyingFollowerChargeChaserAI.this.lastTarget != target.getUniqueID()) {
               currentDir = GameMath.getAngleDir(GameRandom.globalRandom.nextInt(360));
               PlayerFlyingFollowerChargeChaserAI.this.lastTarget = target.getUniqueID();
            } else {
               currentDir = GameMath.normalize(mob.x - target.x, mob.y - target.y);
            }

            this.getBlackboard()
               .mover
               .setCustomMovement(this, new MobMovementRelative(target, -currentDir.x * targetStoppingDistance, -currentDir.y * targetStoppingDistance));
            return true;
         }

         @Override
         public boolean canAttackTarget(T mob, Mob target) {
            return true;
         }
      };
      chaserSequence.addChild(attackNode);
      this.addChild(chaserSequence);
      this.addChild(new AINode<T>() {
         @Override
         protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         }

         @Override
         public void init(T mob, Blackboard<T> blackboard) {
         }

         @Override
         public AINodeResult tick(T mob, Blackboard<T> blackboard) {
            if (PlayerFlyingFollowerChargeChaserAI.this.lastTarget != -1) {
               PlayerFlyingFollowerChargeChaserAI.this.lastTarget = -1;
               attackNode.attackTimer = GameRandom.globalRandom.nextInt(chargeCooldown);
            }

            return AINodeResult.FAILURE;
         }
      });
      this.addChild(new PlayerFlyingFollowerAINode<>(teleportDistance, stoppingDistance));
   }
}
