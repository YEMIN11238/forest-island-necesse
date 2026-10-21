package necesse.entity.objectEntity;

import java.awt.Point;
import necesse.entity.mobs.GameDamage;
import necesse.entity.projectile.TrapArrowProjectile;
import necesse.level.maps.Level;

public class ArrowTrapObjectEntity extends TrapObjectEntity {
   public static GameDamage damage = new GameDamage(40.0F, 100.0F, 0.0F, 2.0F, 1.0F);

   public ArrowTrapObjectEntity(Level level, int x, int y) {
      super(level, x, y, 1000L);
      this.shouldSave = false;
   }

   @Override
   public void triggerTrap(int wireID, int dir) {
      if (!this.isClient() && !this.onCooldown()) {
         if (!this.otherWireActive(wireID)) {
            Point position = this.getPos(this.tileX, this.tileY, dir);
            Point targetDir = this.getDir(dir);
            int xPos = position.x * 32;
            if (targetDir.x == 0) {
               xPos += 16;
            } else if (targetDir.x == -1) {
               xPos += 30;
            } else if (targetDir.x == 1) {
               xPos += 2;
            }

            int yPos = position.y * 32;
            if (targetDir.y == 0) {
               yPos += 16;
            } else if (targetDir.y == -1) {
               yPos += 30;
            } else if (targetDir.y == 1) {
               yPos += 2;
            }

            this.getLevel().entityManager.projectiles.add(new TrapArrowProjectile(xPos, yPos, xPos + targetDir.x, yPos + targetDir.y, damage, null));
            this.startCooldown();
         }
      }
   }

   public Point getDir(int dir) {
      if (dir == 0) {
         return new Point(0, -1);
      } else if (dir == 1) {
         return new Point(1, 0);
      } else if (dir == 2) {
         return new Point(0, 1);
      } else {
         return dir == 3 ? new Point(-1, 0) : new Point();
      }
   }
}
