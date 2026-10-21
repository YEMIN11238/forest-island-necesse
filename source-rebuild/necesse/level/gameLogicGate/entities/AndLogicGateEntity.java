package necesse.level.gameLogicGate.entities;

import necesse.level.gameLogicGate.GameLogicGate;
import necesse.level.maps.TilePosition;

public class AndLogicGateEntity extends SimpleLogicGateEntity {
   public AndLogicGateEntity(GameLogicGate logicGate, TilePosition pos) {
      super(logicGate, pos);
   }

   @Override
   public boolean condition() {
      for (int i = 0; i < 4; i++) {
         if (this.wireInputs[i] && !this.isWireActive(i)) {
            return false;
         }
      }

      return true;
   }
}
