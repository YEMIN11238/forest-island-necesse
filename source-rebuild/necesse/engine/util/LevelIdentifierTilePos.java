package necesse.engine.util;

import java.awt.Point;
import java.util.Objects;

public class LevelIdentifierTilePos {
   public final LevelIdentifier identifier;
   public final Point tile;

   public LevelIdentifierTilePos(LevelIdentifier identifier, Point tile) {
      Objects.requireNonNull(identifier);
      this.identifier = identifier;
      this.tile = tile;
   }
}
