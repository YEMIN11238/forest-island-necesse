package necesse.entity.mobs.ai.behaviourTree.trees;

import java.awt.Point;
import java.util.function.Supplier;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.EscapeAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.WandererAINode;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;

public abstract class CollisionChaserWandererAI<T extends Mob> extends SelectorAINode<T> {
   public final EscapeAINode<T> escapeAINode;
   public final CollisionChaserAI<T> collisionChaserAI;
   public final WandererAINode<T> wandererAINode;

   public CollisionChaserWandererAI(final Supplier<Boolean> shouldEscape, int searchDistance, GameDamage damage, int knockback, int wanderFrequency) {
      this.addChild(this.escapeAINode = new EscapeAINode<T>() {
         @Override
         public boolean shouldEscape(T mob, Blackboard<T> blackboard) {
            return mob.isHostile && !mob.isSummoned && mob.shouldLevelRetreat() ? true : shouldEscape != null && shouldEscape.get();
         }
      });
      this.addChild(this.collisionChaserAI = new CollisionChaserAI<T>(searchDistance, damage, knockback) {
         @Override
         public GameAreaStream<Mob> streamPossibleTargets(T mob, Point base, TargetFinderDistance<T> distance) {
            return CollisionChaserWandererAI.this.streamPossibleTargets(mob, base, distance);
         }
      });
      this.addChild(this.wandererAINode = new WandererAINode<>(wanderFrequency));
   }

   public abstract GameAreaStream<Mob> streamPossibleTargets(T var1, Point var2, TargetFinderDistance<T> var3);
}
