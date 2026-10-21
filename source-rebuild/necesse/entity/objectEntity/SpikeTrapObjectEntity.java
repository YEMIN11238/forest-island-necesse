package necesse.entity.objectEntity;

import necesse.entity.levelEvent.SpikeTrapEvent;
import necesse.level.maps.Level;

public class SpikeTrapObjectEntity extends TrapObjectEntity {
   public SpikeTrapObjectEntity(Level level, int x, int y, int animationDuration) {
      super(level, x, y, animationDuration);
      this.shouldSave = false;
   }

   @Override
   public void triggerTrap(int wireID, int dir) {
      if (!this.isClient() && !this.onCooldown()) {
         if (!this.otherWireActive(wireID)) {
            SpikeTrapEvent e = new SpikeTrapEvent(this.tileX, this.tileY);
            this.getLevel().entityManager.events.add(e);
            this.sendClientTriggerPacket();
            this.startCooldown();
         }
      }
   }

   @Override
   public void onClientTrigger() {
      super.onClientTrigger();
      this.startCooldown();
   }
}
