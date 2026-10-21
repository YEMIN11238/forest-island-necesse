package necesse.entity.mobs.ai.behaviourTree.trees;

import java.util.function.Supplier;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.CooldownAttackTargetAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.EscapeAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.WandererAINode;

public abstract class CollisionShooterPlayerChaserWandererAI<T extends Mob> extends SelectorAINode<T> {
   public CollisionShooterPlayerChaserWandererAI(
      final Supplier<Boolean> shouldEscape,
      int searchDistance,
      GameDamage damage,
      int knockback,
      CooldownAttackTargetAINode.CooldownTimer cooldownTimer,
      int shootCooldown,
      int shootDistance,
      int wanderFrequency
   ) {
      this.addChild(new EscapeAINode<T>() {
         @Override
         public boolean shouldEscape(T mob, Blackboard<T> blackboard) {
            return mob.isHostile && !mob.isSummoned && mob.shouldLevelRetreat() ? true : shouldEscape != null && shouldEscape.get();
         }
      });
      CollisionPlayerChaserAI<T> chaser = new CollisionPlayerChaserAI<>(searchDistance, damage, knockback);
      chaser.addChild(new CooldownAttackTargetAINode<T>(cooldownTimer, shootCooldown, shootDistance) {
         @Override
         public boolean attackTarget(T mob, Mob target) {
            return CollisionShooterPlayerChaserWandererAI.this.shootAtTarget(mob, target);
         }
      });
      this.addChild(chaser);
      this.addChild(new WandererAINode<>(wanderFrequency));
   }

   public abstract boolean shootAtTarget(T var1, Mob var2);
}
