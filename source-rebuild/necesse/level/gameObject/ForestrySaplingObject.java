package necesse.level.gameObject;

public interface ForestrySaplingObject {
   String getForestryResultObjectStringID();

   default boolean shouldForestryPlantAtTile(int tileX, int tileY) {
      return tileX % 2 == 0 && tileY % 2 == 0;
   }
}
