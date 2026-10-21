package necesse.entity.mobs.ai.behaviourTree.trees;

import java.util.function.Supplier;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.EscapeAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.WandererAINode;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.inventory.InventoryItem;

public class ItemAttackerPlayerChaserWandererAI<T extends ItemAttackerMob> extends SelectorAINode<T> {
   public final EscapeAINode<T> escapeAINode;
   public final ItemAttackerPlayerChaserAI<T> itemAttackerPlayerChaserAI;
   public final WandererAINode<T> wandererAINode;

   public ItemAttackerPlayerChaserWandererAI(final Supplier<Boolean> shouldEscape, int searchDistance, InventoryItem defaultAttackItem, int wanderFrequency) {
      this.addChild(this.escapeAINode = new EscapeAINode<T>() {
         public boolean shouldEscape(T mob, Blackboard<T> blackboard) {
            return mob.isHostile && !mob.isSummoned && mob.shouldLevelRetreat() ? true : shouldEscape != null && shouldEscape.get();
         }
      });
      this.addChild(this.itemAttackerPlayerChaserAI = new ItemAttackerPlayerChaserAI<>(searchDistance, defaultAttackItem));
      this.addChild(this.wandererAINode = new WandererAINode<>(wanderFrequency));
   }
}
