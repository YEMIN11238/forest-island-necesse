package necesse.level.gameLogicGate.entities;

import necesse.level.gameLogicGate.GameLogicGate;
import necesse.level.maps.TilePosition;

public class XOrLogicGateEntity extends SimpleLogicGateEntity {
   public XOrLogicGateEntity(GameLogicGate logicGate, TilePosition pos) {
      super(logicGate, pos);
   }

   @Override
   public boolean condition() {
      int activated = 0;

      for (int i = 0; i < 4; i++) {
         if (this.wireInputs[i] && this.isWireActive(i)) {
            activated++;
         }
      }

      return activated == 1;
   }
}
