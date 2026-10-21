package necesse.entity.mobs.misc;

import necesse.engine.util.GameMath;
import necesse.engine.util.GameObjectReservable;
import necesse.engine.util.MovedRectangle;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.trees.HusbandryImpregnateWandererAI;
import necesse.entity.mobs.mobMovement.MobMovementRelative;

public abstract class ProcessMobHandler<C extends Mob, T extends Mob> {
   public final C mob;
   public T target;
   public GameObjectReservable reservable;

   public ProcessMobHandler(C mob, T target, GameObjectReservable reservable) {
      this.mob = mob;
      this.target = target;
      this.reservable = reservable;
   }

   public void tickReserve() {
      if (this.reservable != null) {
         this.reservable.reserve(this.mob);
      }
   }

   public HusbandryImpregnateWandererAI.MoveToTarget getMoveToTile() {
      double distance = GameMath.diagonalMoveDistance(this.mob.getX(), this.mob.getY(), this.target.getX(), this.target.getY());
      return distance <= 160.0
            && !this.mob
               .getLevel()
               .collides(
                  new MovedRectangle(this.mob, this.target.getX(), this.target.getY()),
                  this.mob.modifyChasingCollisionFilter(this.mob.getLevelCollisionFilter(), this.target)
               )
         ? new HusbandryImpregnateWandererAI.MoveToTarget(new MobMovementRelative(this.target, 0.0F, 0.0F))
         : new HusbandryImpregnateWandererAI.MoveToTarget(this.target.getTileX(), this.target.getTileY());
   }

   public abstract boolean isValid();

   public abstract void completeProgress();

   public abstract int getTimeItTakesInMilliseconds();
}
