package necesse.entity.levelEvent.mobAbilityLevelEvent;

import java.awt.Shape;
import java.awt.geom.Point2D.Float;
import necesse.engine.util.GameMath;
import necesse.engine.util.LineHitbox;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;

public class BrutesBattleaxeDashLevelEvent extends MobDashLevelEvent {
   public BrutesBattleaxeDashLevelEvent() {
   }

   public BrutesBattleaxeDashLevelEvent(Mob owner, int seed, float dirX, float dirY, float distance, int animTime, GameDamage damage, float resilienceGain) {
      super(owner, seed, dirX, dirY, distance, animTime, damage, resilienceGain);
   }

   @Override
   public Shape getHitBox() {
      int ownerDir = this.owner.getDir();
      Float dir;
      if (ownerDir == 3) {
         dir = GameMath.getPerpendicularDir(-this.dirX, -this.dirY);
      } else {
         dir = GameMath.getPerpendicularDir(this.dirX, this.dirY);
      }

      float width = 40.0F;
      float frontOffset = 0.0F;
      float range = 80.0F;
      float rangeOffset = -65 + (ownerDir != 0 && ownerDir != 2 ? 0 : 5);
      return new LineHitbox(
         this.owner.x + dir.x * rangeOffset + this.dirX * frontOffset, this.owner.y + dir.y * rangeOffset + this.dirY * frontOffset, dir.x, dir.y, range, width
      );
   }
}
