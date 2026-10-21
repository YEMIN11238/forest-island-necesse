package necesse.level.gameLogicGate.entities;

import necesse.level.gameLogicGate.GameLogicGate;
import necesse.level.maps.TilePosition;

public class NOrLogicGateEntity extends OrLogicGateEntity {
   public NOrLogicGateEntity(GameLogicGate logicGate, TilePosition pos) {
      super(logicGate, pos);
   }

   @Override
   public boolean condition() {
      return !super.condition();
   }
}
