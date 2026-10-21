package necesse.level.maps.levelData.settlementData.settler;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;

public class QualityOfLifeDescriptionWrapped extends QualityOfLifeDescriptionAbstract {
   protected final QualityOfLifeDescription parent;
   public final int actualLevel;

   public QualityOfLifeDescriptionWrapped(QualityOfLifeDescription parent, int actualLevel) {
      super(parent.minLevel);
      this.parent = parent;
      this.actualLevel = actualLevel;
   }

   @Override
   public GameMessage getAdjective() {
      if (this.actualLevel != this.parent.minLevel) {
         GameMessageBuilder builder = new GameMessageBuilder();
         builder.append(this.parent.getAdjective());
         builder.append(" (" + this.actualLevel + ")");
         return builder;
      } else {
         return this.parent.getAdjective();
      }
   }
}
