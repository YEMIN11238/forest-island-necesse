package necesse.entity.mobs.ai.behaviourTree.trees;

import java.awt.Point;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.composites.SequenceAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.ChaserAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.CooldownAttackTargetAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.TargetFinderAINode;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.hostile.pirates.PirateMob;

public class PirateChaserAI<T extends PirateMob> extends SequenceAINode<T> {
   public final CooldownAttackTargetAINode<T> shootAtTargetNode;
   public final TargetFinderAINode<T> targetFinderNode;
   public final ChaserAINode<T> chaserNode;

   public PirateChaserAI(int shootDistance, int shootCooldown, int meleeDistance, int searchDistance) {
      if (shootDistance > 0) {
         this.addChild(
            this.shootAtTargetNode = new CooldownAttackTargetAINode<T>(CooldownAttackTargetAINode.CooldownTimer.TICK, shootCooldown, shootDistance) {
               public boolean attackTarget(T mob, Mob target) {
                  if (mob.canAttack()) {
                     mob.startShootingAbility.runAndSend(target);
                     return true;
                  } else {
                     return false;
                  }
               }
            }
         );
      } else {
         this.shootAtTargetNode = null;
      }

      TargetFinderDistance<T> targetFinder = new TargetFinderDistance<>(searchDistance);
      targetFinder.targetLostAddedDistance = searchDistance * 2;
      this.addChild(this.targetFinderNode = new TargetFinderAINode<T>(targetFinder) {
         public GameAreaStream<? extends Mob> streamPossibleTargets(T mob, Point base, TargetFinderDistance<T> distance) {
            return TargetFinderAINode.streamPlayersAndHumans(mob, base, distance);
         }
      });
      this.addChild(this.chaserNode = new ChaserAINode<T>(meleeDistance, false, true) {
         public boolean attackTarget(T mob, Mob target) {
            if (mob.canAttack()) {
               mob.attack(target.getX(), target.getY(), false);
               target.isServerHit(new GameDamage(mob.meleeDamage), target.x - mob.x, target.y - mob.y, 100.0F, mob);
               return true;
            } else {
               return false;
            }
         }
      });
   }
}
