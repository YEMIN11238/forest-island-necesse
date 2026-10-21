package necesse.entity.mobs.ai.behaviourTree.trees;

import java.awt.Point;
import java.util.function.Supplier;
import necesse.engine.registries.ProjectileRegistry;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.ChaserAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.EscapeAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.TargetFinderAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.WandererAINode;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.projectile.Projectile;

public abstract class PlayerChaserWandererAI<T extends Mob> extends SelectorAINode<T> {
   public final EscapeAINode<T> escapeAINode;
   public final PlayerChaserAI<T> playerChaserAI;
   public final WandererAINode<T> wandererAINode;

   public PlayerChaserWandererAI(
      final Supplier<Boolean> shouldEscape, int searchDistance, int shootDistance, int wanderFrequency, boolean smartPositioning, boolean changePositionOnHit
   ) {
      this.addChild(this.escapeAINode = new EscapeAINode<T>() {
         @Override
         public boolean shouldEscape(T mob, Blackboard<T> blackboard) {
            return mob.isHostile && !mob.isSummoned && mob.shouldLevelRetreat() ? true : shouldEscape != null && shouldEscape.get();
         }
      });
      this.addChild(this.playerChaserAI = new PlayerChaserAI<T>(searchDistance, shootDistance, smartPositioning, changePositionOnHit) {
         @Override
         public boolean canHitTarget(T mob, float fromX, float fromY, Mob target) {
            return PlayerChaserWandererAI.this.canHitTarget(mob, fromX, fromY, target);
         }

         @Override
         public boolean attackTarget(T mob, Mob target) {
            return PlayerChaserWandererAI.this.attackTarget(mob, target);
         }

         @Override
         public GameAreaStream<Mob> streamPossibleTargets(T mob, Point base, TargetFinderDistance<T> distance) {
            return PlayerChaserWandererAI.this.streamPossibleTargets(mob, base, distance);
         }
      });
      if (wanderFrequency >= 0) {
         this.addChild(this.wandererAINode = new WandererAINode<>(wanderFrequency));
      } else {
         this.wandererAINode = null;
      }
   }

   public boolean canHitTarget(T mob, float fromX, float fromY, Mob target) {
      return ChaserAINode.hasLineOfSightToTarget(mob, fromX, fromY, target);
   }

   public abstract boolean attackTarget(T var1, Mob var2);

   public boolean shootSimpleProjectile(T mob, Mob target, String projectileID, GameDamage damage, int speed, int distance) {
      return this.shootSimpleProjectile(mob, target, projectileID, damage, speed, distance, 10);
   }

   public boolean shootSimpleProjectile(T mob, Mob target, String projectileID, GameDamage damage, int speed, int distance, int moveDist) {
      return this.shootAndGetSimpleProjectile(mob, target, projectileID, damage, speed, distance, moveDist) != null;
   }

   public Projectile shootAndGetSimpleProjectile(T mob, Mob target, String projectileID, GameDamage damage, int speed, int distance, int moveDist) {
      if (mob.canAttack()) {
         mob.attack(target.getX(), target.getY(), false);
         Projectile projectile = ProjectileRegistry.getProjectile(projectileID, mob.getLevel(), mob.x, mob.y, target.x, target.y, speed, distance, damage, mob);
         projectile.moveDist(moveDist);
         mob.getLevel().entityManager.projectiles.add(projectile);
         return projectile;
      } else {
         return null;
      }
   }

   public GameAreaStream<Mob> streamPossibleTargets(T mob, Point base, TargetFinderDistance<T> distance) {
      return TargetFinderAINode.streamPlayersAndHumans(mob, base, distance);
   }
}
