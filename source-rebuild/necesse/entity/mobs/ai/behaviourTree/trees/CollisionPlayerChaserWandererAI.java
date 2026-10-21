package necesse.entity.mobs.ai.behaviourTree.trees;

import java.util.function.Supplier;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.CollisionChaserAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.EscapeAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.WandererAINode;

public class CollisionPlayerChaserWandererAI<T extends Mob> extends SelectorAINode<T> {
   public final EscapeAINode<T> escapeAINode;
   public final CollisionPlayerChaserAI<T> collisionPlayerChaserAI;
   public final WandererAINode<T> wandererAINode;

   public CollisionPlayerChaserWandererAI(final Supplier<Boolean> shouldEscape, int searchDistance, GameDamage damage, int knockback, int wanderFrequency) {
      this.addChild(this.escapeAINode = new EscapeAINode<T>() {
         @Override
         public boolean shouldEscape(T mob, Blackboard<T> blackboard) {
            return mob.isHostile && !mob.isSummoned && mob.shouldLevelRetreat() ? true : shouldEscape != null && shouldEscape.get();
         }
      });
      this.addChild(this.collisionPlayerChaserAI = new CollisionPlayerChaserAI<T>(searchDistance, damage, knockback) {
         @Override
         public boolean attackTarget(T mob, Mob target) {
            return CollisionPlayerChaserWandererAI.this.attackTarget(mob, target);
         }
      });
      this.addChild(this.wandererAINode = new WandererAINode<>(wanderFrequency));
   }

   public boolean attackTarget(T mob, Mob target) {
      return CollisionChaserAINode.simpleAttack(mob, target, this.collisionPlayerChaserAI.damage, this.collisionPlayerChaserAI.knockback);
   }
}
