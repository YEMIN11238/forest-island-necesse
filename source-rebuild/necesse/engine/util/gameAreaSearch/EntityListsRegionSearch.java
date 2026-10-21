package necesse.engine.util.gameAreaSearch;

import java.util.stream.Stream;
import necesse.engine.util.GameMath;
import necesse.entity.Entity;
import necesse.entity.manager.EntityRegionList;
import necesse.level.maps.Level;

public class EntityListsRegionSearch<T extends Entity> extends GameRegionSearch<Stream<? extends T>> {
   private final EntityRegionList<? extends T>[] entityRegionLists;

   public EntityListsRegionSearch(Level level, float startX, float startY, int maxTileDistance, EntityRegionList<? extends T>... entityRegionLists) {
      super(
         level,
         level.regionManager.getRegionCoordByTile(GameMath.getTileCoordinate(startX)),
         level.regionManager.getRegionCoordByTile(GameMath.getTileCoordinate(startY)),
         Integer.MAX_VALUE
      );
      this.entityRegionLists = entityRegionLists;
      int startTileX = GameMath.getTileCoordinate(startX);
      int startTileY = GameMath.getTileCoordinate(startY);
      int regionStartX = level.regionManager.getRegionCoordByTile(startTileX - maxTileDistance - 1);
      int regionStartY = level.regionManager.getRegionCoordByTile(startTileY - maxTileDistance - 1);
      int regionEndX = level.regionManager.getRegionCoordByTile(startTileX + maxTileDistance + 1);
      int regionEndY = level.regionManager.getRegionCoordByTile(startTileY + maxTileDistance + 1);
      this.shrinkLimit(regionStartX, regionStartY, regionEndX, regionEndY);
      this.setMaxDistance(Math.max(regionEndX - regionStartX, regionEndY - regionStartY) + 1);
   }

   protected Stream<? extends T> get(int regionX, int regionY) {
      Stream<? extends T> out = null;

      for (EntityRegionList<? extends T> entityList : this.entityRegionLists) {
         if (out == null) {
            out = entityList.getInRegion(regionX, regionY).stream();
         } else {
            out = Stream.concat(out, entityList.getInRegion(regionX, regionY).stream());
         }
      }

      return out;
   }

   public GameAreaStream<T> streamEach() {
      return this.stream().flatStream(v -> (Stream<? extends T>)v);
   }
}
