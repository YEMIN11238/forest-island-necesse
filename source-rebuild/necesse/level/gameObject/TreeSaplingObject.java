package necesse.level.gameObject;

import java.awt.Color;

public class TreeSaplingObject extends SaplingObject implements ForestrySaplingObject {
   public TreeSaplingObject(
      String textureName,
      Color mapColor,
      String resultObjectStringID,
      int minGrowTimeInSeconds,
      int maxGrowTimeInSeconds,
      boolean addAnySaplingIngredient,
      String... additionalValidTiles
   ) {
      super(textureName, mapColor, resultObjectStringID, minGrowTimeInSeconds, maxGrowTimeInSeconds, addAnySaplingIngredient, additionalValidTiles);
   }

   @Override
   public String getForestryResultObjectStringID() {
      return this.resultObjectStringID;
   }
}
