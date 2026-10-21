package necesse.entity.mobs.ai.behaviourTree.util;

import java.awt.Point;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PathDoorOption;
import necesse.entity.mobs.ai.behaviourTree.AINode;

public class SummonTargetValidity<T extends Mob> extends TargetValidity<T> {
   public String baseKey = "mobBase";

   @Override
   public boolean isValidTarget(AINode<T> node, T mob, Mob target, boolean isNewTarget) {
      if (!super.isValidTarget(node, mob, target, isNewTarget)) {
         return false;
      }

      Point base = node.getBlackboard().getObjectNotNull(Point.class, this.baseKey, new Point(mob.getX(), mob.getY()));
      if (target.getLevel().getLightLevel(target).getLevel() <= 0.0F && !target.isBoss() && target.getDistance(base.x, base.y) >= 192.0F) {
         return false;
      }

      if (!target.isHostile && target != node.getBlackboard().getObject(Mob.class, "focusTarget")) {
         return false;
      }

      PathDoorOption doorOption = mob.getLevel().regionManager.BASIC_DOOR_OPTIONS;
      return doorOption.canMoveToTile(GameMath.getTileCoordinate(base.x), GameMath.getTileCoordinate(base.y), target.getTileX(), target.getTileY(), false);
   }
}
