package necesse.entity.mobs.ai.behaviourTree.trees;

import java.awt.Point;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.CritterRunAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.WanderHomeAtConditionAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.WandererAINode;
import necesse.entity.mobs.friendly.critters.CritterMob;

public class HomesickCritterAI<T extends CritterMob> extends SelectorAINode<T> {
   public final WandererAINode<T> wanderer;
   public Point homeTile;
   public float distanceBeforeHomesick = 96.0F;

   public HomesickCritterAI(Mob mob, AINode<T> runner, WandererAINode<T> wanderer) {
      if (runner != null) {
         this.addChild(runner);
      }

      this.homeTile = new Point(mob.getTileX(), mob.getTileY());
      this.addChild(
         new WanderHomeAtConditionAINode<T>() {
            public boolean shouldGoHome(T mob) {
               return HomesickCritterAI.this.distanceBeforeHomesick
                  < mob.getDistance(HomesickCritterAI.this.homeTile.x * 32.0F + 16.0F, HomesickCritterAI.this.homeTile.y * 32.0F + 16.0F);
            }

            public Point getHomeTile(T mob) {
               return HomesickCritterAI.this.homeTile;
            }

            public boolean isHomeRoom(T mob) {
               return true;
            }

            public boolean isHomeHouse(T mob) {
               return true;
            }
         }
      );
      this.addChild(this.wanderer = wanderer);
   }

   public HomesickCritterAI(Mob mob) {
      this(mob, new CritterRunAINode<>(), new WandererAINode<>(5000));
   }
}
