package necesse.level.maps.levelData.settlementData.settler;

import necesse.engine.localization.message.GameMessage;

public class QualityOfLifeDescription extends QualityOfLifeDescriptionAbstract {
   protected final GameMessage adjective;

   public QualityOfLifeDescription(GameMessage adjective, int minLevel) {
      super(minLevel);
      this.adjective = adjective;
   }

   @Override
   public GameMessage getAdjective() {
      return this.adjective;
   }
}
