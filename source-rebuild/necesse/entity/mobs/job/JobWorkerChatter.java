package necesse.entity.mobs.job;

import necesse.engine.localization.message.GameMessage;
import necesse.entity.mobs.Mob;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.level.maps.Level;

public interface JobWorkerChatter {
   default Mob getChatterMob() {
      return (Mob)this;
   }

   default Level getLevel() {
      return this.getChatterMob().getLevel();
   }

   HumanDrawOptions getChatterFaceDrawOptions();

   JobWorkerChatterHandler getCurrentChatterHandler();

   GameMessage getChatterSimpleName();
}
