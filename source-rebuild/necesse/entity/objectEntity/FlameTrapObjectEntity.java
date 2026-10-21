package necesse.entity.objectEntity;

import java.awt.Point;
import necesse.engine.registries.ObjectRegistry;
import necesse.entity.levelEvent.FlameTrapEvent;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.WallFlameTrapObject;
import necesse.level.maps.Level;

public class FlameTrapObjectEntity extends TrapObjectEntity {
   public FlameTrapObjectEntity(Level level, int x, int y) {
      super(level, x, y, 1000L);
      this.shouldSave = false;
   }

   @Override
   public void triggerTrap(int wireID, int dir) {
      if (!this.isClient() && !this.onCooldown()) {
         if (!this.otherWireActive(wireID)) {
            Point position = this.getPos(this.tileX, this.tileY, dir);
            GameObject object = this.getObject();
            if (!(object instanceof WallFlameTrapObject)) {
               object = ObjectRegistry.getObject("stoneflametrap");
            }

            WallFlameTrapObject flameTrapObject = (WallFlameTrapObject)object;
            FlameTrapEvent e = new FlameTrapEvent(
               position.x, position.y, dir, flameTrapObject.range, flameTrapObject.perpendicularWidth, flameTrapObject.damage
            );
            this.getLevel().entityManager.events.add(e);
            this.startCooldown();
         }
      }
   }
}
