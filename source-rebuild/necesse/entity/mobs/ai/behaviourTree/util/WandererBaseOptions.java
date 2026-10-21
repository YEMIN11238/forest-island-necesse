package necesse.entity.mobs.ai.behaviourTree.util;

import java.awt.Point;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.leaves.WandererAINode;

public interface WandererBaseOptions<T extends Mob> {
   Point getBaseTile(T var1);

   default int getBaseRadius(T mob, WandererAINode<T> node) {
      return node.searchRadius;
   }

   default boolean forceFindAroundBase(T mob) {
      return false;
   }

   default boolean isBaseHouse(T mob, Point base) {
      return false;
   }

   default boolean isBaseRoom(T mob, Point base) {
      return false;
   }
}
