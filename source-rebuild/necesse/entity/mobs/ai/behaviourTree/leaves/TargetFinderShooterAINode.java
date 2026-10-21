package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.geom.Line2D.Float;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.level.maps.CollisionFilter;

public abstract class TargetFinderShooterAINode<T extends Mob> extends AINode<T> {
   public int shootDistance;
   public String focusTargetKey;

   public TargetFinderShooterAINode(int shootDistance, String focusTargetKey) {
      this.shootDistance = shootDistance;
      this.focusTargetKey = focusTargetKey;
   }

   public TargetFinderShooterAINode(int shootDistance) {
      this(shootDistance, "focusTarget");
   }

   @Override
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   @Override
   public void init(T mob, Blackboard<T> blackboard) {
   }

   @Override
   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      if (this.canAttack(mob)) {
         Mob focusTarget = blackboard.getObject(Mob.class, this.focusTargetKey);
         Mob target;
         if (focusTarget != null && this.canShootTarget(mob, focusTarget)) {
            target = focusTarget;
         } else {
            target = this.streamTargets(mob, this.shootDistance)
               .filter(m -> this.canShootTarget(mob, m))
               .min(Comparator.comparingDouble(mob::getDistance))
               .orElse(null);
         }

         if (target != null) {
            this.shootTarget(mob, target);
            return AINodeResult.SUCCESS;
         }
      }

      return AINodeResult.FAILURE;
   }

   public boolean canAttack(T mob) {
      return mob.canAttack();
   }

   public abstract Stream<Mob> streamTargets(T var1, int var2);

   public boolean canShootTarget(T mob, Mob target) {
      if (mob.getDistance(target) > this.shootDistance) {
         return false;
      }

      CollisionFilter collisionFilter = mob.modifyChasingCollisionFilter(new CollisionFilter().projectileCollision(), target);
      return !mob.getLevel().collides(new Float(mob.x, mob.y, target.x, target.y), collisionFilter);
   }

   public abstract void shootTarget(T var1, Mob var2);

   public static <T extends Mob> Stream<Mob> streamPlayersAndHumans(T mob, int distance) {
      return mob.getLevel().entityManager.streamAreaMobsAndPlayers(mob.x, mob.y, distance).filter(m -> {
         if (m != null && m != mob && !m.removed() && m.isVisible()) {
            int team = m.getTeam();
            if (team == -100) {
               return true;
            } else {
               return m.isHuman && team != -1 ? true : m.isPlayer;
            }
         } else {
            return false;
         }
      }).collect(Collectors.toList()).stream();
   }
}
