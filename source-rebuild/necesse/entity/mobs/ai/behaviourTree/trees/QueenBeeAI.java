package necesse.entity.mobs.ai.behaviourTree.trees;

import java.awt.Point;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.MigrateToApiaryAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.WandererAINode;
import necesse.entity.mobs.ai.behaviourTree.util.WandererBaseOptions;
import necesse.entity.mobs.friendly.QueenBeeMob;

public class QueenBeeAI<T extends QueenBeeMob> extends SelectorAINode<T> {
   public final MigrateToApiaryAINode<T> migrateToApiaryAINode;
   public final WandererAINode<T> wandererAINode;

   public QueenBeeAI(int wanderFrequency) {
      this.addChild(this.migrateToApiaryAINode = new MigrateToApiaryAINode<>());
      this.addChild(this.wandererAINode = new WandererAINode<T>(wanderFrequency) {
         @Override
         public WandererBaseOptions<T> getBaseOptions() {
            return new WandererBaseOptions<T>() {
               public Point getBaseTile(T mob) {
                  return mob.migrationApiary;
               }

               public int getBaseRadius(T mob, WandererAINode<T> node) {
                  return 5;
               }

               public boolean forceFindAroundBase(T mob) {
                  return mob.migrationApiary != null;
               }
            };
         }
      });
   }
}
